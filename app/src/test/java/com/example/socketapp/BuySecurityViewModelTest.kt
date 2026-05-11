package com.example.socketapp

import com.example.socketapp.data.SecuritiesRepository
import com.example.socketapp.Security as BuyableSecurity
import com.example.socketapp.model.Security as MarketSecurity
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuySecurityViewModelTest {
    @Test
    fun `initial state has no instrument`() {
        val viewModel = PurchaseViewModel(FakeBuySecuritiesRepository())

        assertEquals(null, viewModel.uiState.instrument)
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy limit rejects only prices above ask plus movement`() {
        val viewModel = buySecurityViewModel()

        viewModel.onOrderTypeChange(BuyOrderType.Limit)
        viewModel.onLimitPriceChange("23.000,00")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("supera el maximo permitido")
            },
        )
    }

    @Test
    fun `sell limit rejects only prices below bid minus movement`() {
        val viewModel = buySecurityViewModel()

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(BuyOrderType.Limit)
        viewModel.onLimitPriceChange("16.000,00")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("por debajo del minimo permitido")
            },
        )
    }

    @Test
    fun `sell by amount rejects amounts over max sellable`() {
        val viewModel = buySecurityViewModel()

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(BuyOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("200.000,00")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("monto maximo vendible")
            },
        )
    }

    @Test
    fun `buy by amount computes nominals rounded down to lot size`() {
        val viewModel = buySecurityViewModel()

        viewModel.onOrderTypeChange(BuyOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("40.000,00")

        assertEquals("19610.00", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("2", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("40000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy by quantity computes trade amount`() {
        val viewModel = buySecurityViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("3")

        assertEquals("3", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("58830.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy rejects amount below minimum trade amount`() {
        val viewModel = buySecurityViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("50")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("monto minimo")
            },
        )
    }

    @Test
    fun `buy rejects quantity over max instrument nominals`() {
        val viewModel = buySecurityViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("100")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("cantidad maxima")
            },
        )
    }

    @Test
    fun `buy market rejects missing ask price without derived amount errors`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(askPrice = BigDecimal.ZERO))

        viewModel.onOrderTypeChange(BuyOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("100")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("precio valido")
            },
        )
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("lamina minima") || error.contains("monto minimo")
            },
        )
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradePrice)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
    }

    @Test
    fun `sell market rejects missing bid price without derived amount errors`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(bidPrice = BigDecimal.ZERO))

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(BuyOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("100")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("precio valido")
            },
        )
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("lamina minima") || error.contains("monto minimo")
            },
        )
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradePrice)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
    }

    @Test
    fun `quantity mode rejects zero nominals`() {
        val viewModel = buySecurityViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("0")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("mayor a cero")
            },
        )
    }

    @Test
    fun `quantity mode rejects nominals below minimum`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(minInstrumentNominals = 5),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("3")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("cantidad minima")
            },
        )
    }

    @Test
    fun `quantity mode rejects nominals not multiple of lot size`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(lotInstrumentSize = 5),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("7")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("multiplo de 5")
            },
        )
    }

    @Test
    fun `sell by quantity rejects nominals over available holdings`() {
        val viewModel = buySecurityViewModel()

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("11")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("nominales disponibles")
            },
        )
    }

    @Test
    fun `limit price rejects invalid multiple for stock range`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(liderMerval = true))

        viewModel.onOrderTypeChange(BuyOrderType.Limit)
        viewModel.onLimitPriceChange("19.210,25")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("multiplo de 1")
            },
        )
    }

    @Test
    fun `limit price accepts valid multiple for letras`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(type = "Letras"),
        )

        viewModel.onOrderTypeChange(BuyOrderType.Limit)
        viewModel.onLimitPriceChange("19.210,123")

        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("multiplo")
            },
        )
    }

    @Test
    fun `buy ars rejects insufficient ars balance by trade amount`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(
                accountBalanceArs = BigDecimal("1000.00"),
                maxTradeAmount = BigDecimal("9999999.00"),
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("1")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("Saldo insuficiente para operar")
            },
        )
    }

    @Test
    fun `buy usd validates usd amount and ars fee balances`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceTradeCurrency("USD")
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(
                accountBalanceArs = BigDecimal("5.00"),
                accountBalanceUsd = BigDecimal("50.00"),
                tradeFee = BigDecimal("10.00"),
                maxTradeAmount = BigDecimal("9999999.00"),
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("100")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("pesos para comisiones")
            },
        )
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("dolares")
            },
        )
    }

    @Test
    fun `sell usd validates ars fee balance`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceTradeCurrency("USD")
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(
                accountBalanceArs = BigDecimal("5.00"),
                tradeFee = BigDecimal("10.00"),
                maxTradeAmount = BigDecimal("9999999.00"),
            ),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("1")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("pesos para comisiones")
            },
        )
    }

    @Test
    fun `ars total trade amount includes fee`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(
                tradeFee = BigDecimal("10.00"),
                maxTradeAmount = BigDecimal("9999999.00"),
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("20000")

        assertEquals("20010.00", viewModel.uiState.validation.totalTradeAmount.toPlainString())
    }

    @Test
    fun `usd total trade amount excludes fee`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceTradeCurrency("USD")
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(
                tradeFee = BigDecimal("10.00"),
                maxTradeAmount = BigDecimal("9999999.00"),
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("20000")

        assertEquals("20000.00", viewModel.uiState.validation.totalTradeAmount.toPlainString())
    }

    @Test
    fun `buy rejects total above maximum amount`() {
        val viewModel = buySecurityViewModel()
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(maxTradeAmount = BigDecimal("1000.00")),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("20000")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("monto maximo")
            },
        )
    }

    @Test
    fun `amount parser handles decimal point as decimal separator`() {
        val viewModel = buySecurityViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("40000.50")

        assertEquals("40000,50", viewModel.uiState.amountInput)
        assertEquals(BigDecimal("40000.50"), viewModel.uiState.activeInput)
        assertEquals("40000.50", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `amount parser handles single digit decimal point as decimal separator`() {
        val viewModel = buySecurityViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("1.2")

        assertEquals("1,2", viewModel.uiState.amountInput)
        assertEquals(BigDecimal("1.2"), viewModel.uiState.activeInput)
        assertEquals("1.20", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `amount parser handles local thousands and decimal separators`() {
        val viewModel = buySecurityViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("40.000,50")

        assertEquals("40000,50", viewModel.uiState.amountInput)
        assertEquals("40000.50", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `amount parser keeps pending comma decimal input from validating as whole number`() {
        val viewModel = buySecurityViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("1,")

        assertEquals("1,", viewModel.uiState.amountInput)
        assertEquals(null, viewModel.uiState.activeInput)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.validation.canContinue)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("lamina minima") || error.contains("monto minimo")
            },
        )
    }

    @Test
    fun `amount parser keeps pending dot decimal input from validating as whole number`() {
        val viewModel = buySecurityViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("0.")

        assertEquals("0,", viewModel.uiState.amountInput)
        assertEquals(null, viewModel.uiState.activeInput)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.validation.canContinue)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error.contains("lamina minima") || error.contains("monto minimo")
            },
        )
    }
}

private fun buySecurityViewModel(): PurchaseViewModel =
    PurchaseViewModel(FakeBuySecuritiesRepository()).also { viewModel ->
        viewModel.replaceInstrument(TestBuyableInstrument)
    }

private val TestBuyableInstrument = BuyableSecurity(
    id = 66238,
    ticker = "PAMP",
    description = "PAMPA HOLDING SA ORD. 1V.",
    type = "Acciones",
    currency = "ARS",
    codeType = "CAJA_VALOR",
    codeValue = "457",
    industry = "Electric Utilities",
    liderMerval = false,
    indexationType = null,
    isFavorite = false,
    holdingQuantity = 10,
    minInstrumentNominals = 1,
    lotInstrumentSize = 1,
    minTradeNominals = 100,
    lastPrice = BigDecimal("40005.75"),
    dailyVariationPercent = BigDecimal("0.27"),
    askPrice = BigDecimal("19610.00"),
    bidPrice = BigDecimal("19580.00"),
    percentageMovement = BigDecimal("0.15"),
)

private class FakeBuySecuritiesRepository : SecuritiesRepository {
    override fun getCachedSecurities(): List<MarketSecurity>? = null

    override suspend fun refreshSecurities(): List<MarketSecurity> = emptyList()

    override suspend fun getBuyableInstruments(): List<BuyableSecurity> = emptyList()

    override suspend fun getBuyableInstrument(id: String): BuyableSecurity? = null
}
