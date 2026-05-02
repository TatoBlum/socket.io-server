package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.socketapp.data.SecuritiesRepository
import com.example.socketapp.model.Security
import com.example.socketapp.model.SecurityCurrency
import com.example.socketapp.model.SecurityPanel
import com.example.socketapp.model.SecuritySector
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
                    repository = repository,
                    ioDispatcher = testDispatcher,
                    defaultDispatcher = testDispatcher,
                ) as T
        }

        return ViewModelProvider(store, factory)[SecuritiesViewModel::class.java]
    }
}

private class FakeSecuritiesRepository(
    security: Security,
) : SecuritiesRepository {

    var securities = listOf(security)

    override suspend fun getSecurities(): List<Security> = securities
}

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
    currency = SecurityCurrency.Pesos,
    panel = SecurityPanel.Merval,
    sector = SecuritySector.Technology,
    isFavourite = isFavourite,
)
