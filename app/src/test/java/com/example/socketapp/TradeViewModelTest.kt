package com.example.socketapp

import com.example.socketapp.data.SecuritiesRepository
import com.example.socketapp.Security as BuyableSecurity
import com.example.socketapp.model.Security as MarketSecurity
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeViewModelTest {
    @Test
    fun `initial state has no instrument`() {
        val viewModel = TradeViewModel(FakeBuySecuritiesRepository(), TradeValidator())

        assertEquals(null, viewModel.uiState.instrument)
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy limit rejects only prices above ask plus movement`() {
        val viewModel = tradeViewModel()

        viewModel.onOrderTypeChange(BuyOrderType.Limit)
        viewModel.onLimitPriceChange("23.000,00")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("supera el maximo permitido")
            },
        )
    }

    @Test
    fun `sell limit rejects only prices below bid minus movement`() {
        val viewModel = tradeViewModel()

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(BuyOrderType.Limit)
        viewModel.onLimitPriceChange("16.000,00")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("por debajo del minimo permitido")
            },
        )
    }

    @Test
    fun `sell by amount rejects amounts over max sellable`() {
        val viewModel = tradeViewModel()
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(availableNominals = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(BuyOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("200.000")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("monto maximo vendible")
            },
        )
    }

    @Test
    fun `buy by amount computes nominals rounded down to lot size`() {
        val viewModel = tradeViewModel()

        viewModel.onOrderTypeChange(BuyOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("40.000")

        assertEquals("19610.00", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("2", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("40000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("2", viewModel.uiState.quantityInput.toPlainString())
        assertEquals("40000.00", viewModel.uiState.amountInput.toPlainString())
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy by quantity computes trade amount`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("3")

        assertEquals("3", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("58830.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("3", viewModel.uiState.quantityInput.toPlainString())
        assertEquals("58830.00", viewModel.uiState.amountInput.toPlainString())
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy rejects amount below minimum trade amount`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("50")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(viewModel.uiState.inputHelper.toLegacyMessage().contains("debitar"))
        assertTrue(viewModel.uiState.inputError?.toLegacyMessage()?.contains("no alcanza") == true)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("monto minimo")
            },
        )
    }

    @Test
    fun `amount helper shows available balance before input`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)

        assertEquals("Saldo disponible $1159000.00", viewModel.uiState.inputHelper.toLegacyMessage())
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `quantity helper shows available buying balance before input`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)

        assertEquals("Disponible para comprar $1159000.00", viewModel.uiState.inputHelper.toLegacyMessage())
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `buy helper shows approximate debit after valid amount`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("40000")

        assertEquals("Valor aproximado a debitar $40000.00", viewModel.uiState.inputHelper.toLegacyMessage())
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `sell helper shows available nominals before input`() {
        val viewModel = tradeViewModel()
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(availableNominals = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)

        assertEquals("Nominales disponibles 10", viewModel.uiState.inputHelper.toLegacyMessage())
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `sell helper shows approximate credit after valid quantity`() {
        val viewModel = tradeViewModel()
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(availableNominals = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("1")

        assertEquals("Valor aproximado a acreditar $19580.00", viewModel.uiState.inputHelper.toLegacyMessage())
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `buy rejects quantity over max instrument nominals`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("100")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("cantidad maxima")
            },
        )
    }

    @Test
    fun `buy market rejects missing ask price without derived amount errors`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(askPrice = BigDecimal.ZERO))

        viewModel.onOrderTypeChange(BuyOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("100")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("precio valido")
            },
        )
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("lamina minima") || error.toLegacyMessage().contains("monto minimo")
            },
        )
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradePrice)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
    }

    @Test
    fun `sell market rejects missing bid price without derived amount errors`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(bidPrice = BigDecimal.ZERO))

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(BuyOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("100")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("precio valido")
            },
        )
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("lamina minima") || error.toLegacyMessage().contains("monto minimo")
            },
        )
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradePrice)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
    }

    @Test
    fun `quantity mode rejects zero nominals`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("0")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("mayor a cero")
            },
        )
    }

    @Test
    fun `quantity mode rejects nominals below minimum`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(minInstrumentNominals = 5),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("3")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("cantidad minima")
            },
        )
    }

    @Test
    fun `quantity mode rejects nominals not multiple of lot size`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(lotInstrumentSize = 5),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("7")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("multiplo de 5")
            },
        )
    }

    @Test
    fun `sell by quantity rejects nominals over available holdings`() {
        val viewModel = tradeViewModel()
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(availableNominals = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("11")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("nominales disponibles")
            },
        )
    }

    @Test
    fun `limit price rejects invalid multiple for stock range`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(liderMerval = true))

        viewModel.onOrderTypeChange(BuyOrderType.Limit)
        viewModel.onLimitPriceChange("19.210,25")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("multiplo de 1")
            },
        )
    }

    @Test
    fun `limit price accepts valid multiple for letras`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(type = "Letras"),
        )

        viewModel.onOrderTypeChange(BuyOrderType.Limit)
        viewModel.onLimitPriceChange("19.210,123")

        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("multiplo")
            },
        )
    }

    @Test
    fun `buy ars rejects insufficient ars balance by trade amount`() {
        val viewModel = tradeViewModel()
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
                error.toLegacyMessage().contains("Saldo insuficiente para operar")
            },
        )
    }

    @Test
    fun `buy usd validates usd amount and ars fee balances`() {
        val viewModel = tradeViewModel()
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
                error.toLegacyMessage().contains("pesos para comisiones")
            },
        )
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("dolares")
            },
        )
    }

    @Test
    fun `sell usd validates ars fee balance`() {
        val viewModel = tradeViewModel()
        viewModel.replaceTradeCurrency("USD")
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(
                accountBalanceArs = BigDecimal("5.00"),
                tradeFee = BigDecimal("10.00"),
                maxTradeAmount = BigDecimal("9999999.00"),
                availableNominals = 10,
            ),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("1")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("pesos para comisiones")
            },
        )
    }

    @Test
    fun `ars total trade amount includes fee`() {
        val viewModel = tradeViewModel()
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
        val viewModel = tradeViewModel()
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
        val viewModel = tradeViewModel()
        viewModel.replaceAccountContext(
            viewModel.uiState.accountContext.copy(maxTradeAmount = BigDecimal("1000.00")),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("20000")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("monto maximo")
            },
        )
    }

    @Test
    fun `amount parser handles thousand dots as whole number separators`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("40.000")

        assertEquals("40.000", viewModel.uiState.amountInputText)
        assertEquals(BigDecimal("40000"), viewModel.uiState.activeInput)
        assertEquals("40000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `quantity parser handles thousand dots as whole number separators`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange("1.000")

        assertEquals("1.000", viewModel.uiState.quantityInputText)
        assertEquals(BigDecimal("1000"), viewModel.uiState.activeInput)
        assertEquals("19610000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `amount parser drops unsupported decimal comma`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("40.000,50")

        assertEquals("40.00050", viewModel.uiState.amountInputText)
        assertEquals(BigDecimal("4000050"), viewModel.uiState.activeInput)
    }

    @Test
    fun `amount parser ignores comma-only decimal separator`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("1,")

        assertEquals("1", viewModel.uiState.amountInputText)
        assertEquals(BigDecimal.ONE, viewModel.uiState.activeInput)
    }

    @Test
    fun `amount parser keeps trailing thousand dot as visual input but parses digits`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange("0.")

        assertEquals("0.", viewModel.uiState.amountInputText)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.activeInput)
        assertEquals(BigDecimal.ZERO.setScale(2), viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.validation.canContinue)
    }
}

private fun tradeViewModel(): TradeViewModel =
    TradeViewModel(FakeBuySecuritiesRepository(), TradeValidator()).also { viewModel ->
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

private fun TradeInputHelper.toLegacyMessage(): String =
    when (this) {
        TradeInputHelper.None -> ""
        is TradeInputHelper.AvailableBalance -> "Saldo disponible $${amount.toMoneyStringForTest()}"
        is TradeInputHelper.AvailableToBuy -> "Disponible para comprar $${amount.toMoneyStringForTest()}"
        is TradeInputHelper.AvailableNominals -> "Nominales disponibles $quantity"
        is TradeInputHelper.ApproximateDebit -> "Valor aproximado a debitar $${amount.toMoneyStringForTest()}"
        is TradeInputHelper.ApproximateCredit -> "Valor aproximado a acreditar $${amount.toMoneyStringForTest()}"
    }

private fun TradeValidationError.toLegacyMessage(): String =
    when (this) {
        TradeValidationError.InvalidLimitPrice -> "Ingresa un precio limite valido"
        is TradeValidationError.LimitPriceOutOfBandBuy -> "El precio limite supera el maximo permitido"
        is TradeValidationError.LimitPriceOutOfBandSell -> "El precio limite esta por debajo del minimo permitido"
        is TradeValidationError.LimitPriceNotMultiple -> "El precio limite debe respetar el multiplo de ${step.toPlainString()}"
        TradeValidationError.MissingTradePrice -> "No se pudo obtener un precio valido para la operacion"
        TradeValidationError.AmountNotEnoughForMin -> "El monto ingresado no alcanza para la lamina minima"
        TradeValidationError.NominalsInvalid -> "La cantidad debe ser mayor a cero"
        is TradeValidationError.NominalsBelowMin -> "La cantidad minima es ${minNominals.toPlainString()} nominales"
        is TradeValidationError.NominalsNotMultiple -> "La cantidad debe ser multiplo de ${lotSize.toPlainString()}"
        is TradeValidationError.NominalsOverMax -> "La cantidad maxima para operar es ${maxNominals.toPlainString()} nominales"
        is TradeValidationError.NominalsOverAvailable -> "Tenes ${availableNominals.toPlainString()} nominales disponibles"
        is TradeValidationError.AmountOverMaxSellable -> "El monto maximo vendible es $${maxAmount.toMoneyStringForTest()}"
        is TradeValidationError.InsufficientArs -> "Saldo insuficiente para operar"
        TradeValidationError.InsufficientArsForFee -> "Saldo insuficiente en pesos para comisiones"
        is TradeValidationError.InsufficientUsd -> "Saldo insuficiente en dolares"
        is TradeValidationError.TotalBelowMinAmount -> "El monto minimo para operar es $${minAmount.toMoneyStringForTest()}"
        is TradeValidationError.TotalAboveMaxAmount -> "El monto maximo para operar es $${maxAmount.toMoneyStringForTest()}"
    }

private fun BigDecimal.toMoneyStringForTest(): String =
    setScale(2).toPlainString()
