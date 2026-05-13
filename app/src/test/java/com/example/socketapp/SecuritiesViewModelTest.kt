package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.socketapp.data.SecuritiesRepository
import com.example.socketapp.Security as BuyableSecurity
import com.example.socketapp.model.Security as MarketSecurity
import java.math.BigDecimal
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
            security(id = "AAPL", isFavourite = true),
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()

            assertEquals(true, vm.uiState.items.single { item -> item.id == "AAPL" }.isFavourite)
            assertEquals(false to null, vm.uiState.errorState)
            assertEquals(false, vm.uiState.isLoading)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `polling refresh keeps pending unfavourite update`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", isFavourite = true),
            seedCache = true,
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)
        try {
            runCurrent()

            assertEquals(true, vm.uiState.items.single { item -> item.id == "AAPL" }.isFavourite)

            vm.onFavouriteClick("AAPL")
            runCurrent()
            assertEquals(false, vm.uiState.items.single { item -> item.id == "AAPL" }.isFavourite)

            repository.securities = listOf(security(id = "AAPL", isFavourite = true))
            vm.startPollingIfNeeded()
            advanceTimeBy(60_000)
            runCurrent()

            assertEquals(false, vm.uiState.items.single { item -> item.id == "AAPL" }.isFavourite)
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
            assertEquals(BigDecimal("100.00"), vm.uiState.items.single { item -> item.id == "AAPL" }.price)

            repository.securities = listOf(
                security(id = "AAPL", price = "125.00", percentageChange = "25.00"),
            )
            vm.startPollingIfNeeded()
            runCurrent()
            assertEquals(BigDecimal("100.00"), vm.uiState.items.single { item -> item.id == "AAPL" }.price)

            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(BigDecimal("125.00"), vm.uiState.items.single { item -> item.id == "AAPL" }.price)
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
            assertEquals(BigDecimal("100.00"), vm.uiState.items.single { item -> item.id == "AAPL" }.price)
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
            assertEquals(BigDecimal("125.00"), vm.uiState.items.single { item -> item.id == "AAPL" }.price)
        } finally {
            store.clear()
        }
    }

    private fun securitiesViewModel(
        store: ViewModelStore,
        repository: SecuritiesRepository,
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
    security: MarketSecurity,
    seedCache: Boolean = false,
) : SecuritiesRepository {

    var securities = listOf(security)
    var refreshError: Throwable? = null
    private var cache: List<MarketSecurity>? = if (seedCache) listOf(security) else null

    override fun getCachedSecurities(): List<MarketSecurity>? = cache

    override suspend fun refreshSecurities(): List<MarketSecurity> {
        refreshError?.let { error -> throw error }
        cache = securities
        return securities
    }

    override suspend fun getBuyableInstruments(): List<BuyableSecurity> =
        securities.map { security -> security.toBuyableInstrument() }

    override suspend fun getBuyableInstrument(id: String): BuyableSecurity? =
        securities.firstOrNull { security -> security.id == id }?.toBuyableInstrument()
}

private fun MarketSecurity.toBuyableInstrument(): BuyableSecurity =
    BuyableSecurity(
        id = id.hashCode() and Int.MAX_VALUE,
        ticker = symbol,
        description = name,
        type = "Acciones",
        currency = "ARS",
        codeType = "MOCK_SECURITY_ID",
        codeValue = id,
        industry = sector,
        liderMerval = panel == "S&P Merval",
        indexationType = null,
        isFavorite = isFavourite,
        minInstrumentNominals = 1,
        lotInstrumentSize = 1,
        minTradeNominals = 1,
        lastPrice = price,
        dailyVariationPercent = percentageChange,
        askPrice = price,
        bidPrice = price,
        percentageMovement = BigDecimal("0.15"),
    )

private fun security(
    id: String,
    isFavourite: Boolean = false,
    price: String = "100.00",
    percentageChange: String = "1.00",
): MarketSecurity = MarketSecurity(
    id = id,
    symbol = id,
    name = id,
    rawPrice = price,
    rawPriceChange = "1.00",
    rawPercentageChange = percentageChange,
    currency = "Pesos",
    panel = "S&P Merval",
    sector = "Tecnologia",
    isFavourite = isFavourite,
)
