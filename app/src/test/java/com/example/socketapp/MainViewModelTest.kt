package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Tests de caja negra del [MainViewModel]: sólo usan la API pública
 * (`subscribeToSocketEvents`, `stopSocket`, `bitcoin`, `connectionState`).
 *
 * Estrategia:
 * - El datasource se reemplaza por [FakeBitcoinTickerDataSource] para controlar
 *   emisiones y errores desde el test.
 * - Se usa `UnconfinedTestDispatcher` tanto para `Dispatchers.Main` (vía
 *   `setMain`) como para el `ioDispatcher` inyectado al VM. Con eso toda la
 *   pipeline corre inline en el mismo hilo del test → asserts deterministas,
 *   sin `withTimeout` ni esperas.
 */
@ExperimentalCoroutinesApi
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `subscribe — los tickers del datasource aparecen en bitcoin`() = runTest {
        val fake = FakeBitcoinTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("42000.00"))

        assertEquals("42000.00", vm.bitcoin.value?.price)
    }

    @Test
    fun `stop — cancela la suscripción y limpia el ticker`() = runTest {
        val fake = FakeBitcoinTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("1000.00"))
        assertEquals("1000.00", vm.bitcoin.value?.price)

        vm.stopSocket()

        assertNull(vm.bitcoin.value)
        assertEquals(0, fake.activeCollectors.value)
    }

    @Test
    fun `subscribe es idempotente — dos llamadas seguidas no crean dos collectors`() = runTest {
        val fake = FakeBitcoinTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        vm.subscribeToSocketEvents()

        assertEquals(1, fake.activeCollectors.value)
    }

    @Test
    fun `subscribe → stop → subscribe — reinicia limpio`() = runTest {
        val fake = FakeBitcoinTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("100.00"))
        assertEquals("100.00", vm.bitcoin.value?.price)
        vm.stopSocket()
        assertEquals(0, fake.activeCollectors.value)

        vm.subscribeToSocketEvents()
        fake.send(ticker("200.00"))

        assertEquals("200.00", vm.bitcoin.value?.price)
    }

    @Test
    fun `error en el flow — no crashea al VM y limpia el ticker`() = runTest {
        val fake = FakeBitcoinTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("50.00"))
        assertEquals("50.00", vm.bitcoin.value?.price)

        fake.fail(IOException("boom"))

        assertNull(vm.bitcoin.value)
    }

    @Test
    fun `connectionState se expone directo del datasource`() {
        val fake = FakeBitcoinTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        assertSame(fake.connectionState, vm.connectionState)
    }

    @Test
    fun `onCleared — destruir el VM libera el collector`() = runTest {
        val fake = FakeBitcoinTickerDataSource()
        // Simulamos el ciclo de vida de Android: un ViewModelStore que, al
        // llamar a clear(), invoca onCleared() en cada VM que contiene.
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(fake, testDispatcher) as T
        }
        val vm = ViewModelProvider(store, factory)[MainViewModel::class.java]

        vm.subscribeToSocketEvents()
        assertEquals(1, fake.activeCollectors.value)

        store.clear()

        assertEquals(0, fake.activeCollectors.value)
    }

    // ---------- helpers ----------

    private fun ticker(price: String) = BitcoinTicker(price)
}

/**
 * Fake del datasource bajo control total del test. Expone:
 * - [send]: inyectar un ticker en el flow.
 * - [fail]: hacer que el flow lance una excepción.
 * - [activeCollectors]: cuántos consumidores están actualmente suscritos
 *   al `start()` (útil para tests de idempotencia/cancelación).
 */
@ExperimentalCoroutinesApi
private class FakeBitcoinTickerDataSource :
    BitcoinTickerDataSource(WebSocketClient(), url = "ws://fake") {

    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    private val _activeCollectors = MutableStateFlow(0)
    val activeCollectors = _activeCollectors.asStateFlow()

    override fun start(): Flow<BitcoinTicker> = events
        .onStart { _activeCollectors.update { it + 1 } }
        .onCompletion { _activeCollectors.update { it - 1 } }
        .transform { event ->
            when (event) {
                is Event.Ticker -> emit(event.ticker)
                is Event.Fail -> throw event.cause
            }
        }

    suspend fun send(ticker: BitcoinTicker) {
        events.emit(Event.Ticker(ticker))
    }

    suspend fun fail(cause: Throwable) {
        events.emit(Event.Fail(cause))
    }

    private sealed class Event {
        data class Ticker(val ticker: BitcoinTicker) : Event()
        data class Fail(val cause: Throwable) : Event()
    }
}
