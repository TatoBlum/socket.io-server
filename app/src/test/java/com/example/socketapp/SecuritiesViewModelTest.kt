package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.socketapp.data.SecuritiesRepository
import com.example.socketapp.model.Security
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
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
    fun `polling refresh keeps pending unfavourite update`() = runTest {
        val repository = FakeSecuritiesRepository(
            security(id = "AAPL", isFavourite = true),
        )
        val store = ViewModelStore()
        val vm = securitiesViewModel(store, repository)

        assertEquals(true, vm.uiState.items.single { item -> item.id == "AAPL" }.isFavourite)

        vm.onFavouriteClick("AAPL")
        assertEquals(false, vm.uiState.items.single { item -> item.id == "AAPL" }.isFavourite)

        repository.securities = listOf(security(id = "AAPL", isFavourite = true))
        vm.startPollingIfNeeded()
        advanceTimeBy(60_000)

        assertEquals(false, vm.uiState.items.single { item -> item.id == "AAPL" }.isFavourite)
        store.clear()
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
    security: Security,
) : SecuritiesRepository {

    var securities = listOf(security)
    private var cache: List<Security>? = null

    override fun getCachedSecurities(): List<Security>? = cache

    override suspend fun refreshSecurities(): List<Security> {
        cache = securities
        return securities
    }

    override suspend fun getBuyableInstruments(): List<BuyableInstrument> =
        securities.map { security -> security.toBuyableInstrument() }

    override suspend fun getBuyableInstrument(id: String): BuyableInstrument? =
        securities.firstOrNull { security -> security.id == id }?.toBuyableInstrument()
}

private fun Security.toBuyableInstrument(): BuyableInstrument =
    BuyableInstrument(
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
        holdingQuantity = 10,
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
    isFavourite: Boolean,
): Security = Security(
    id = id,
    symbol = id,
    name = id,
    rawPrice = "100.00",
    rawPriceChange = "1.00",
    rawPercentageChange = "1.00",
    currency = "Pesos",
    panel = "S&P Merval",
    sector = "Tecnologia",
    isFavourite = isFavourite,
)
