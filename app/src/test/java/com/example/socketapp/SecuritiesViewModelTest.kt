package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.socketapp.data.MockSecuritiesRepository
import java.math.BigDecimal
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SecuritiesViewModelTest {

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
    fun `initial load without cache fetches securities`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", isFavorite = true),
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()

            assertEquals(true, vm.uiState.items.single { item -> item.codeValue == "AAPL" }.isFavorite)
            assertEquals(false to null, vm.uiState.errorState)
            assertEquals(false, vm.uiState.isLoading)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `initial load fetches sector options`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", isFavorite = true),
        ).apply {
            sectors = listOf("CER", "Dolar Linked")
        }
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()

            assertEquals(listOf("CER", "Dolar Linked"), vm.uiState.sectorOptions)
        } finally {
            store.clear()
        }
    }


    @Test
    fun `polling refresh keeps pending unfavourite update`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", isFavorite = true),
            seedCache = true,
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()

            assertEquals(true, vm.uiState.items.single { item -> item.codeValue == "AAPL" }.isFavorite)

            vm.onFavouriteClick("AAPL")
            runCurrent()
            assertEquals(false, vm.uiState.items.single { item -> item.codeValue == "AAPL" }.isFavorite)

            repository.securities = listOf(security(id = "AAPL", isFavorite = true))
            vm.startPollingIfNeeded()
            advanceTimeBy(60_000)
            runCurrent()

            assertEquals(false, vm.uiState.items.single { item -> item.codeValue == "AAPL" }.isFavorite)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `favorite change is synced immediately as single ticker`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", isFavorite = true),
            security(id = "PAMP", codeValue = "PAMP-0", isFavorite = false),
            seedCache = true,
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()

            vm.onFavouriteClick("PAMP")
            runCurrent()

            assertEquals(listOf("PAMP"), repository.toggledFavorites)
            assertEquals(1, repository.toggleFavoritesCallCount)
            assertEquals(true, repository.securities.single { item -> item.codeValue == "PAMP-0" }.isFavorite)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `double favorite click sends two single ticker toggles`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", isFavorite = false),
            seedCache = true,
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()

            vm.onFavouriteClick("AAPL")
            vm.onFavouriteClick("AAPL")
            runCurrent()

            assertEquals(listOf(listOf("AAPL"), listOf("AAPL")), repository.toggledFavoritesHistory)
            assertEquals(2, repository.toggleFavoritesCallCount)
            assertEquals(false, vm.uiState.items.single { item -> item.codeValue == "AAPL" }.isFavorite)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `favorite click while sync is in flight sends another single ticker toggle`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", isFavorite = false),
            seedCache = true,
        ).apply {
            toggleDelayMs = 1_000
        }
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()

            vm.onFavouriteClick("AAPL")
            vm.onFavouriteClick("AAPL")
            advanceTimeBy(1_000)
            runCurrent()

            assertEquals(listOf(listOf("AAPL"), listOf("AAPL")), repository.toggledFavoritesHistory)
            assertEquals(false, vm.uiState.items.single { item -> item.codeValue == "AAPL" }.isFavorite)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `polling refresh updates ui after delay`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", price = "100.00", percentageChange = "1.00"),
            seedCache = true,
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()
            assertEquals(BigDecimal("100.00"), vm.uiState.items.single { item -> item.codeValue == "AAPL" }.price)

            repository.securities = listOf(
                security(id = "AAPL", price = "125.00", percentageChange = "25.00"),
            )
            vm.startPollingIfNeeded()
            runCurrent()
            assertEquals(BigDecimal("100.00"), vm.uiState.items.single { item -> item.codeValue == "AAPL" }.price)

            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(BigDecimal("125.00"), vm.uiState.items.single { item -> item.codeValue == "AAPL" }.price)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `polling refresh exposes error and keeps previous items`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", price = "100.00"),
            seedCache = true,
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()
            repository.refreshError = IllegalStateException("Network unavailable")
            vm.startPollingIfNeeded()
            advanceTimeBy(60_000)
            runCurrent()

            assertEquals(true to "Network unavailable", vm.uiState.errorState)
            assertEquals(BigDecimal("100.00"), vm.uiState.items.single { item -> item.codeValue == "AAPL" }.price)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `successful refresh clears previous error`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", price = "100.00"),
            seedCache = true,
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()
            repository.refreshError = IllegalStateException("Network unavailable")
            vm.startPollingIfNeeded()
            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(true to "Network unavailable", vm.uiState.errorState)

            repository.refreshError = null
            repository.securities = listOf(security(id = "AAPL", price = "125.00"))
            advanceTimeBy(60_000)
            runCurrent()

            assertEquals(false to null, vm.uiState.errorState)
            assertEquals(BigDecimal("125.00"), vm.uiState.items.single { item -> item.codeValue == "AAPL" }.price)
        } finally {
            store.clear()
        }
    }

    private fun securitiesViewModel(
        store: ViewModelStore,
        repository: MockSecuritiesRepository,
    ): SecuritiesViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SecuritiesViewModel(
                    repository,
                    testDispatcher,
                    testDispatcher,
                ) as T
        }

        return ViewModelProvider(store, factory)[SecuritiesViewModel::class.java]
    }
}

private class FakeSecuritiesRepository(
    vararg initialSecurities: Security,
    seedCache: Boolean = false,
) : MockSecuritiesRepository() {

    var securities = initialSecurities.toList()
    var sectors = listOf("Tecnologia")
    var toggledFavorites: List<String>? = null
    var toggledFavoritesHistory = mutableListOf<List<String>>()
    var toggleFavoritesCallCount = 0
    var toggleDelayMs = 0L
    var refreshError: Throwable? = null
    private var cache: List<Security>? = if (seedCache) initialSecurities.toList() else null

    override fun getCachedSecurities(): List<Security>? = cache

    override suspend fun refreshSecurities(): List<Security> {
        refreshError?.let { error -> throw error }
        cache = securities
        return securities
    }

    override suspend fun getBuyableInstruments(): List<Security> =
        securities

    override suspend fun refreshSecuritySectors(): List<String> =
        sectors

    override suspend fun toggleFavorites(tickers: List<String>) {
        delay(toggleDelayMs)
        toggleFavoritesCallCount += 1
        toggledFavorites = tickers
        toggledFavoritesHistory.add(tickers)
        val toggleSet = tickers.toSet()
        securities = securities.map { security ->
            if (security.ticker in toggleSet) {
                security.copy(isFavorite = !security.isFavorite)
            } else {
                security
            }
        }
        cache = cache?.map { security ->
            if (security.ticker in toggleSet) {
                security.copy(isFavorite = !security.isFavorite)
            } else {
                security
            }
        }
    }

    override suspend fun getBuyableInstrument(
        codeType: String,
        codeValue: String,
    ): Security? =
        securities.firstOrNull { security ->
            security.codeType == codeType && security.codeValue == codeValue
        }
}

private fun security(
    id: String,
    codeValue: String = id,
    isFavorite: Boolean = false,
    price: String = "100.00",
    percentageChange: String = "1.00",
): Security = Security(
    id = id.hashCode() and Int.MAX_VALUE,
    ticker = id,
    description = id,
    type = "Acciones",
    currency = "ARS",
    codeType = "MOCK_SECURITY_ID",
    codeValue = codeValue,
    industry = "Tecnologia",
    panel = "S&P Merval",
    liderMerval = true,
    isFavorite = isFavorite,
    minInstrumentNominals = 1,
    maxInstrumentNominals = 999999999,
    lotInstrumentSize = 1,
    price = BigDecimal(price),
    priceChange = BigDecimal("1.00"),
    dailyVariationPercent = BigDecimal(percentageChange),
    askPrice00 = BigDecimal(price),
    askPrice24 = BigDecimal(price),
    bidPrice00 = BigDecimal(price),
    bidPrice24 = BigDecimal(price),
    percentageMovement = BigDecimal("0.015"),
    minTradeAmount = BigDecimal("100.00"),
)
