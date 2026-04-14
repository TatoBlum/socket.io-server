package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Tests de caja negra del [MainViewModel]: usan solo la API pública
 * (`subscribeToSocketEvents`, `stopSocket`, `tickers`, `connectionState`).
 *
 * Estrategia:
 * - El datasource se reemplaza por [FakeCryptoTickerDataSource] para controlar
 *   emisiones y errores desde el test.
 * - Se usa `UnconfinedTestDispatcher` tanto para `Dispatchers.Main` (vía
 *   `setMain`) como para el `ioDispatcher` inyectado al VM.
 * - El VM tiene una coroutine de publish con `while(isActive) { delay(250) }`.
 *   IMPORTANTE: cada test que llame `subscribeToSocketEvents()` debe terminar
 *   con `vm.stopSocket()` para cancelar ese loop antes de que `runTest`
 *   intente `advanceUntilIdle()` (de lo contrario corre infinitamente).
 * - Después de `send()` se llama `advanceTimeBy(300)` para que dispare el
 *   publisher y se publique el snapshot en `_tickers`.
 */
@ExperimentalCoroutinesApi
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `subscribe — los tickers del datasource aparecen en tickers map`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "42000.00"))
        advanceTimeBy(300)

        assertEquals("42000.00", vm.tickers.value["BTCUSDT"]?.price)
        vm.stopSocket()
    }

    @Test
    fun `stop — cancela la suscripcion y limpia el mapa`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "1000.00"))
        advanceTimeBy(300)
        assertEquals("1000.00", vm.tickers.value["BTCUSDT"]?.price)

        vm.stopSocket()

        assertEquals(emptyMap<String, CryptoTicker>(), vm.tickers.value)
        assertEquals(0, fake.activeCollectors.value)
    }

    @Test
    fun `subscribe es idempotente — dos llamadas seguidas no crean dos collectors`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        vm.subscribeToSocketEvents()

        assertEquals(1, fake.activeCollectors.value)
        vm.stopSocket()
    }

    @Test
    fun `subscribe stop subscribe — reinicia limpio`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "100.00"))
        advanceTimeBy(300)
        assertEquals("100.00", vm.tickers.value["BTCUSDT"]?.price)
        vm.stopSocket()
        assertEquals(0, fake.activeCollectors.value)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "200.00"))
        advanceTimeBy(300)

        assertEquals("200.00", vm.tickers.value["BTCUSDT"]?.price)
        vm.stopSocket()
    }

    @Test
    fun `error en el flow — no crashea al VM y limpia el mapa`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "50.00"))
        advanceTimeBy(300)
        assertEquals("50.00", vm.tickers.value["BTCUSDT"]?.price)

        fake.fail(IOException("boom"))

        assertEquals(emptyMap<String, CryptoTicker>(), vm.tickers.value)
        // No stopSocket needed: error cancels the collector coroutine, but
        // the publisher loop is still running. Stop it explicitly.
        vm.stopSocket()
    }

    @Test
    fun `connectionState se expone directo del datasource`() {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        assertSame(fake.connectionState, vm.connectionState)
    }

    @Test
    fun `onCleared — destruir el VM libera el collector`() = runTest {
        val fake = FakeCryptoTickerDataSource()
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
        // store.clear() calls onCleared() which cancels socketJob, so the
        // while(isActive) publisher loop stops. No extra stopSocket needed.
    }

    @Test
    fun `priceDirection UP — precio sube de 100 a 200`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "100.00"))
        advanceTimeBy(300)
        fake.send(ticker("BTCUSDT", "200.00"))
        advanceTimeBy(300)

        assertEquals(PriceDirection.UP, vm.tickers.value["BTCUSDT"]?.priceDirection)
        vm.stopSocket()
    }

    @Test
    fun `priceDirection DOWN — precio baja de 200 a 100`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "200.00"))
        advanceTimeBy(300)
        fake.send(ticker("BTCUSDT", "100.00"))
        advanceTimeBy(300)

        assertEquals(PriceDirection.DOWN, vm.tickers.value["BTCUSDT"]?.priceDirection)
        vm.stopSocket()
    }

    @Test
    fun `priceDirection NEUTRAL — precio no cambia`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "150.00"))
        advanceTimeBy(300)
        fake.send(ticker("BTCUSDT", "150.00"))
        advanceTimeBy(300)

        assertEquals(PriceDirection.NEUTRAL, vm.tickers.value["BTCUSDT"]?.priceDirection)
        vm.stopSocket()
    }

    @Test
    fun `multiples simbolos — todos aparecen en el mapa`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "42000.00"))
        fake.send(ticker("ETHUSDT", "3000.00"))
        fake.send(ticker("BNBUSDT", "500.00"))
        advanceTimeBy(300)

        assertEquals("42000.00", vm.tickers.value["BTCUSDT"]?.price)
        assertEquals("3000.00", vm.tickers.value["ETHUSDT"]?.price)
        assertEquals("500.00", vm.tickers.value["BNBUSDT"]?.price)
        vm.stopSocket()
    }

    @Test
    fun `update del mismo simbolo — reemplaza el valor previo`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "40000.00"))
        advanceTimeBy(300)
        fake.send(ticker("BTCUSDT", "41000.00"))
        advanceTimeBy(300)

        assertEquals("41000.00", vm.tickers.value["BTCUSDT"]?.price)
        assertEquals(1, vm.tickers.value.size)
        vm.stopSocket()
    }

    @Test
    fun `previousPrice — se registra el precio anterior tras actualizacion`() = runTest {
        val fake = FakeCryptoTickerDataSource()
        val vm = MainViewModel(fake, testDispatcher)

        vm.subscribeToSocketEvents()
        fake.send(ticker("BTCUSDT", "100.00"))
        advanceTimeBy(300)
        fake.send(ticker("BTCUSDT", "200.00"))
        advanceTimeBy(300)

        assertEquals("100.00", vm.tickers.value["BTCUSDT"]?.previousPrice)
        vm.stopSocket()
    }

    // ---------- helpers ----------

    private fun ticker(symbol: String, price: String) = CryptoTicker(
        symbol = symbol,
        displayName = symbol,
        price = price,
    )
}

/**
 * Fake del datasource bajo control total del test. Expone:
 * - [send]: inyectar un ticker en el flow.
 * - [fail]: hacer que el flow lance una excepcion.
 * - [activeCollectors]: cuantos consumidores estan actualmente suscritos
 *   al `start()` (util para tests de idempotencia/cancelacion).
 */
@ExperimentalCoroutinesApi
private class FakeCryptoTickerDataSource :
    CryptoTickerDataSource(WebSocketClient(), url = "ws://fake") {

    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    private val _activeCollectors = MutableStateFlow(0)
    val activeCollectors = _activeCollectors.asStateFlow()

    override fun start(): Flow<CryptoTicker> = events
        .onStart { _activeCollectors.update { it + 1 } }
        .onCompletion { _activeCollectors.update { it - 1 } }
        .transform { event ->
            when (event) {
                is Event.Ticker -> emit(event.ticker)
                is Event.Fail -> throw event.cause
            }
        }

    suspend fun send(ticker: CryptoTicker) {
        events.emit(Event.Ticker(ticker))
    }

    suspend fun fail(cause: Throwable) {
        events.emit(Event.Fail(cause))
    }

    private sealed class Event {
        data class Ticker(val ticker: CryptoTicker) : Event()
        data class Fail(val cause: Throwable) : Event()
    }
}
