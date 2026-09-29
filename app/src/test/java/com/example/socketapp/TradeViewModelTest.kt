package com.example.socketapp

import com.example.socketapp.data.MockSecuritiesRepository
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
        assertEquals(null, viewModel.uiState.selectedAccount)
        assertTrue(viewModel.uiState.accounts.isNotEmpty())
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `settlement term is stored in view model state`() {
        val viewModel = TradeViewModel(FakeBuySecuritiesRepository(), TradeValidator())

        viewModel.onSettlementTermChange(SettlementType.TWENTY_FOUR_HOURS)

        assertEquals(SettlementType.TWENTY_FOUR_HOURS, viewModel.uiState.settlementTerm)
    }

    @Test
    fun `buy market uses ask price for selected settlement term`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                askPrice24 = BigDecimal("110.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1")

        assertEquals("100.00", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("100.00", viewModel.uiState.validation.tradeAmount.toPlainString())

        viewModel.onSettlementTermChange(SettlementType.TWENTY_FOUR_HOURS)

        assertEquals("110.00", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("110.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `sell market uses bid price for selected settlement term`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                bidPrice00 = BigDecimal("95.00"),
                bidPrice24 = BigDecimal("90.00"),
                holdingQuantity = 10,
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1")

        assertEquals("95.00", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("95.00", viewModel.uiState.validation.tradeAmount.toPlainString())

        viewModel.onSettlementTermChange(SettlementType.TWENTY_FOUR_HOURS)

        assertEquals("90.00", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("90.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `security defaults allow missing server values but ui state blocks missing market rules`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            Security(
                id = 1,
                ticker = "TEST",
                askPrice00 = BigDecimal("100.00"),
                bidPrice00 = BigDecimal("99.00"),
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1")

        assertFalse(viewModel.uiState.instrument?.hasRequiredTradingConfiguration == true)
        assertFalse(viewModel.uiState.canContinue)
    }

    @Test
    fun `buy limit rejects only prices above ask plus movement`() {
        val viewModel = tradeViewModel()

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("23.000,00")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("El maximo permitido es")
            },
        )
    }

    @Test
    fun `buy limit exposes max allowed helper and field error`() {
        val viewModel = tradeViewModel()

        viewModel.onOrderTypeChange(TradeOrderType.Limit)

        val helper = viewModel.uiState.limitPriceHelper as TradeInputLimitPriceHelper.MaxAllowed
        assertEquals(BigDecimal("22551.50"), helper.amount)
        assertEquals("\$", helper.currencySymbol)

        viewModel.onLimitPriceChange("23.000,00")

        assertEquals(null, viewModel.uiState.inputError)
        assertTrue(viewModel.uiState.limitPriceError is TradeValidationError.LimitPriceOutOfBandBuy)
    }

    @Test
    fun `buy limit price band uses ask price for selected settlement term`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                askPrice24 = BigDecimal("200.00"),
                percentageMovement = BigDecimal("0.10"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("150")

        assertTrue(viewModel.uiState.limitPriceError is TradeValidationError.LimitPriceOutOfBandBuy)

        viewModel.onSettlementTermChange(SettlementType.TWENTY_FOUR_HOURS)

        assertEquals(null, viewModel.uiState.limitPriceError)
        assertEquals(
            BigDecimal("220.00"),
            (viewModel.uiState.limitPriceHelper as TradeInputLimitPriceHelper.MaxAllowed).amount,
        )
    }

    @Test
    fun `limit price helper exposes instrument currency symbol`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                currency = "USD",
                askPrice00 = BigDecimal("100.00"),
                percentageMovement = BigDecimal("0.10"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)

        val helper = viewModel.uiState.limitPriceHelper as TradeInputLimitPriceHelper.MaxAllowed
        assertEquals(BigDecimal("110.00"), helper.amount)
        assertEquals("USD ", helper.currencySymbol)
    }

    @Test
    fun `instrument without market price forces limit order`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal.ZERO,
                percentageMovement = BigDecimal("0.10"),
            ),
        )

        assertTrue(viewModel.uiState.isLimitPriceOnly)
        assertEquals(TradeOrderType.Limit, viewModel.uiState.orderType)
        assertEquals(listOf(TradeOrderType.Limit), viewModel.uiState.availableOrderTypes)

        viewModel.onOrderTypeChange(TradeOrderType.Market)

        assertEquals(TradeOrderType.Limit, viewModel.uiState.orderType)
    }

    @Test
    fun `limit only instrument stays limit only when settlement term changes`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal.ZERO,
                askPrice24 = BigDecimal("200.00"),
                percentageMovement = BigDecimal("0.10"),
            ),
        )

        viewModel.onSettlementTermChange(SettlementType.TWENTY_FOUR_HOURS)

        assertTrue(viewModel.uiState.isLimitPriceOnly)
        assertEquals(TradeOrderType.Limit, viewModel.uiState.orderType)
        assertEquals(listOf(TradeOrderType.Limit), viewModel.uiState.availableOrderTypes)
    }

    @Test
    fun `instrument with missing market price in another settlement term is limit only`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                askPrice24 = BigDecimal.ZERO,
                percentageMovement = BigDecimal("0.10"),
            ),
        )

        assertTrue(viewModel.uiState.isLimitPriceOnly)
        assertEquals(TradeOrderType.Limit, viewModel.uiState.orderType)
        assertEquals(listOf(TradeOrderType.Limit), viewModel.uiState.availableOrderTypes)
    }

    @Test
    fun `limit only instrument uses limit price and minimum amount only`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Acciones",
                askPrice00 = BigDecimal.ZERO,
                percentageMovement = BigDecimal("0.10"),
                minTradeAmount = BigDecimal("100.00"),
            ),
        )

        viewModel.onLimitPriceChange("102,50")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1")

        assertEquals(TradeOrderType.Limit, viewModel.uiState.orderType)
        assertEquals(null, viewModel.uiState.limitPriceError)
        assertEquals(null, viewModel.uiState.inputError)
        assertEquals("102.50", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("102.50", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertTrue(viewModel.uiState.canContinue)
    }

    @Test
    fun `limit price stays empty and is not validated until user edits it`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Acciones",
                liderMerval = false,
                askPrice00 = BigDecimal("102.42"),
                percentageMovement = BigDecimal("1.00"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)

        assertEquals("", viewModel.uiState.limitPriceInput)
        assertEquals(null, viewModel.uiState.inputError)
        assertEquals(null, viewModel.uiState.limitPriceError)

        viewModel.onLimitPriceChange("102,42")

        assertTrue(viewModel.uiState.limitPriceError is TradeValidationError.LimitPriceNotMultiple)
    }

    @Test
    fun `limit price input keeps at most three decimals`() {
        val viewModel = tradeViewModel()

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("1000,0001")

        assertEquals("1.000,000", viewModel.uiState.limitPriceInput)
    }

    @Test
    fun `buy limit helper normalizes negative percentage movement`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("102.42"),
                percentageMovement = BigDecimal("-1.82"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)

        assertEquals(
            BigDecimal("104.28"),
            (viewModel.uiState.limitPriceHelper as TradeInputLimitPriceHelper.MaxAllowed).amount,
        )
    }

    @Test
    fun `sell limit rejects only prices below bid minus movement`() {
        val viewModel = tradeViewModel()

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("16.000,00")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("El minimo permitido es")
            },
        )
    }

    @Test
    fun `sell limit exposes min allowed helper and field error`() {
        val viewModel = tradeViewModel()

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)

        val helper = viewModel.uiState.limitPriceHelper as TradeInputLimitPriceHelper.MinAllowed
        assertEquals(BigDecimal("16643.00"), helper.amount)
        assertEquals("\$", helper.currencySymbol)

        viewModel.onLimitPriceChange("16.000,00")

        assertEquals(null, viewModel.uiState.inputError)
        assertTrue(viewModel.uiState.limitPriceError is TradeValidationError.LimitPriceOutOfBandSell)
    }

    @Test
    fun `sell limit price band uses bid price for selected settlement term`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                bidPrice00 = BigDecimal("200.00"),
                bidPrice24 = BigDecimal("100.00"),
                percentageMovement = BigDecimal("0.10"),
                holdingQuantity = 10,
            ),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("150")

        assertTrue(viewModel.uiState.limitPriceError is TradeValidationError.LimitPriceOutOfBandSell)

        viewModel.onSettlementTermChange(SettlementType.TWENTY_FOUR_HOURS)

        assertEquals(null, viewModel.uiState.limitPriceError)
        assertEquals(
            BigDecimal("90.00"),
            (viewModel.uiState.limitPriceHelper as TradeInputLimitPriceHelper.MinAllowed).amount,
        )
    }

    @Test
    fun `sell by amount rejects amounts over max sellable`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(holdingQuantity = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "200.000")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("Supera tu disponible")
            },
        )
    }

    @Test
    fun `sell market by amount validates max sellable amount using bid price`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("101.00"),
                bidPrice00 = BigDecimal("100.00"),
            ),
        )
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(holdingQuantity = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1001")

        val error = viewModel.uiState.inputError as TradeValidationError.NotEnoughAvailableAmount
        assertEquals("1000.00", error.availableAmount.toPlainString())
        assertEquals("1000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy by amount computes nominals rounded down to lot size`() {
        val viewModel = tradeViewModel()

        viewModel.onOrderTypeChange(TradeOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "40.000")

        assertEquals("19610.00", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("2", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("39220.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("2", viewModel.uiState.quantityInput.toPlainString())
        assertEquals("39220.00", viewModel.uiState.amountInput.toPlainString())
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy by quantity computes trade amount`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "3")

        assertEquals("3", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("58830.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("3", viewModel.uiState.quantityInput.toPlainString())
        assertEquals("58830.00", viewModel.uiState.amountInput.toPlainString())
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy limit by amount computes nominals using limit price`() {
        val viewModel = tradeViewModel()

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("20.000,00")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "59000")

        assertEquals("20000.00", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("2", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("40000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `buy limit by amount rounds minimum using limit price`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Letras",
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 2,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("51,68")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "101")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals("51.68", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("103.36", error.minAmount.toPlainString())
        assertEquals(null, viewModel.uiState.limitPriceError)
    }

    @Test
    fun `buy limit by amount accepts exact rounded minimum using limit price`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Letras",
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("51,68")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "103,36")

        assertEquals("2", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("103.36", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals(null, viewModel.uiState.inputError)
        assertEquals(null, viewModel.uiState.limitPriceError)
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy limit keeps limit price error separate from amount error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("51.68"),
                liderMerval = true,
                minInstrumentNominals = 2,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("51,68")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "101")

        assertTrue(viewModel.uiState.inputError is TradeValidationError.AmountNotEnoughForMin)
        assertTrue(viewModel.uiState.limitPriceError is TradeValidationError.LimitPriceNotMultiple)
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy limit by quantity computes trade amount using limit price`() {
        val viewModel = tradeViewModel()

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("20.000,00")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "3")

        assertEquals("20000.00", viewModel.uiState.validation.tradePrice.toPlainString())
        assertEquals("3", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("60000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `buy limit by quantity below operation minimum keeps quantity semantics`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Letras",
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("51,68")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1")

        assertEquals("51.68", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertTrue(viewModel.uiState.inputError is TradeValidationError.OperationAmountBelowMin)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.AmountNotEnoughForMin
            },
        )
    }

    @Test
    fun `buy limit by quantity validates lot multiple with limit price amount`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("20.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 5,
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("20")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "7")

        assertEquals("140.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertTrue(viewModel.uiState.inputError is TradeValidationError.NominalsNotMultiple)
    }

    @Test
    fun `buy limit by quantity shows equivalent amount helper`() {
        val viewModel = tradeViewModel()

        viewModel.onTradeOptionsChange(TradeOption.ADVANCE)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("100")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "102")

        assertEquals("102", viewModel.uiState.quantityInputText)
        assertEquals("102", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("10200.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("Estimado a debitar: $10200.00", viewModel.uiState.inputHelper.toLegacyMessage())
    }

    @Test
    fun `sell limit by amount rejects input above sellable amount using limit price`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("101.00"),
                bidPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 4,
                lotInstrumentSize = 1,
                percentageMovement = BigDecimal("0.80"),
            ),
        )
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(holdingQuantity = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("100")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1001")

        val error = viewModel.uiState.inputError as TradeValidationError.NotEnoughAvailableAmount
        assertEquals("1000.00", error.availableAmount.toPlainString())
        assertEquals("1000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals(null, viewModel.uiState.limitPriceError)
    }

    @Test
    fun `sell limit by amount accepts exact sellable amount using limit price`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("101.00"),
                bidPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 4,
                lotInstrumentSize = 1,
                percentageMovement = BigDecimal("0.80"),
            ),
        )
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(holdingQuantity = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("100")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1000")

        assertEquals("10", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("1000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals(null, viewModel.uiState.inputError)
        assertEquals(null, viewModel.uiState.limitPriceError)
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `sell limit by amount below minimum uses limit price for amount error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("101.00"),
                bidPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 4,
                lotInstrumentSize = 1,
                percentageMovement = BigDecimal("0.80"),
            ),
        )
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(holdingQuantity = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("30")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "99")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals("90.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("120.00", error.minAmount.toPlainString())
        assertEquals(null, viewModel.uiState.limitPriceError)
    }

    @Test
    fun `sell limit by quantity validates lot and available nominals`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("101.00"),
                bidPrice00 = BigDecimal("100.00"),
                lotInstrumentSize = 5,
                percentageMovement = BigDecimal("0.50"),
            ),
        )
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(holdingQuantity = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("100")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "12")

        assertEquals("1200.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsNotMultiple
            },
        )
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NotEnoughNominals
            },
        )
        assertEquals(null, viewModel.uiState.limitPriceError)
    }

    @Test
    fun `switching from amount to quantity does not reuse amount as quantity`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "120")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("120")

        assertEquals("120", viewModel.uiState.amountInputText)
        assertEquals("", viewModel.uiState.quantityInputText)
        assertEquals(null, viewModel.uiState.activeInput)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeNominals)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.validation.errors.any { it is TradeValidationError.NominalsInvalid })
    }

    @Test
    fun `quantity limit order without touched limit price hides limit price error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("403.79"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "2")

        assertEquals(TradeInputHelper.None, viewModel.uiState.inputHelper)
        assertEquals(null, viewModel.uiState.limitPriceError)
        assertEquals(listOf(TradeValidationError.InvalidLimitPrice), viewModel.uiState.validation.errors)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `quantity limit order with touched empty limit price shows limit price error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("403.79"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "2")

        assertEquals(TradeInputHelper.None, viewModel.uiState.inputHelper)
        assertEquals(TradeValidationError.InvalidLimitPrice, viewModel.uiState.limitPriceError)
        assertEquals("Ingresa un precio limite valido", viewModel.uiState.limitPriceError?.toLegacyMessage())
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `quantity limit order with zero limit price shows limit price error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("403.79"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("0")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "2")

        assertEquals(TradeInputHelper.None, viewModel.uiState.inputHelper)
        assertEquals(TradeValidationError.InvalidLimitPrice, viewModel.uiState.limitPriceError)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `quantity limit order with limit price shows equivalent amount helper`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("403.79"),
            ),
        )

        viewModel.onTradeOptionsChange(TradeOption.ADVANCE)
        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("350")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "2")

        val helper = viewModel.uiState.inputHelper as TradeInputHelper.ApproximateDebit
        assertEquals("700.00", helper.amount.toPlainString())
        assertEquals("700.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `switching from quantity to amount clears amount validation`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("403.79"),
                percentageMovement = BigDecimal("0.20"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("350")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "2")
        assertEquals("700.00", viewModel.uiState.validation.tradeAmount.toPlainString())

        viewModel.onInputModeChange(BuyInputMode.Amount)

        assertEquals(BuyInputMode.Amount, viewModel.uiState.inputMode)
        assertEquals("", viewModel.uiState.amountInputText)
        assertEquals(null, viewModel.uiState.activeInput)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeNominals)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.inputHelper is TradeInputHelper.ApproximateDebit)
    }

    @Test
    fun `stale quantity input event after switching to amount does not update amount`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("403.79"),
                percentageMovement = BigDecimal("0.20"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("350")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(BuyInputMode.Quantity, "2")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(BuyInputMode.Quantity, "2")

        assertEquals(BuyInputMode.Amount, viewModel.uiState.inputMode)
        assertEquals("", viewModel.uiState.amountInputText)
        assertEquals("2", viewModel.uiState.quantityInputText)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.inputHelper is TradeInputHelper.ApproximateDebit)
    }

    @Test
    fun `same numeric value typed after switching to amount is treated as amount`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("403.79"),
                percentageMovement = BigDecimal("0.20"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("350")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(BuyInputMode.Quantity, "2")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(BuyInputMode.Amount, "2")

        assertEquals(BuyInputMode.Amount, viewModel.uiState.inputMode)
        assertEquals("2", viewModel.uiState.amountInputText)
        assertEquals("2", viewModel.uiState.quantityInputText)
        assertTrue(viewModel.uiState.validation.tradeAmount.compareTo(BigDecimal.ZERO) == 0)
        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(viewModel.uiState.inputError is TradeValidationError.OperationAmountBelowMin)
    }

    @Test
    fun `different amount typed after switching from quantity is accepted`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("403.79"),
                percentageMovement = BigDecimal("0.20"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("350")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(BuyInputMode.Quantity, "2")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(BuyInputMode.Amount, "20")

        assertEquals("20", viewModel.uiState.amountInputText)
        assertTrue(viewModel.uiState.inputError is TradeValidationError.OperationAmountBelowMin)
    }

    @Test
    fun `buy rejects amount below minimum trade amount`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "50")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(viewModel.uiState.inputError?.toLegacyMessage()?.contains("monto minimo") == true)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("monto minimo")
            },
        )
    }

    @Test
    fun `buy derives minimum amount from instrument minimum and lot size`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("30.00"),
                minInstrumentNominals = 2,
                lotInstrumentSize = 5,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "120")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals(BigDecimal("150.00"), error.minAmount)
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `amount mode below one tradable lot returns only amount error`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "313,25")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(viewModel.uiState.inputError is TradeValidationError.AmountNotEnoughForMin)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsInvalid ||
                    error is TradeValidationError.OperationAmountBelowMin
            },
        )
    }

    @Test
    fun `amount mode minimum uses lot size when lot is above nominal minimum`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 5,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "499")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals("500.00", error.minAmount.toPlainString())
    }

    @Test
    fun `amount mode minimum rounds up to next tradable amount`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Letras",
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 2,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "101")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals("103.36", error.minAmount.toPlainString())
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.OperationAmountBelowMin
            },
        )
    }

    @Test
    fun `amount mode above ars minimum but below first nominal shows operation minimum`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                ticker = "EDN21",
                description = "Edenor",
                askPrice00 = BigDecimal("597.83"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
                minTradeAmount = BigDecimal("100.00"),
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "101")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals("597.83", error.minAmount.toPlainString())
        assertFalse(
            viewModel.uiState.validation.errors.any { validationError ->
                validationError is TradeValidationError.OperationAmountBelowMin
            },
        )
    }

    @Test
    fun `amount mode below first nominal shows rounded tradable minimum`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Letras",
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 2,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "50")

        val error = viewModel.uiState.inputError as TradeValidationError.OperationAmountBelowMin
        assertEquals("100.00", error.minAmount.toPlainString())
        assertEquals(BigDecimal.ZERO.setScale(2), viewModel.uiState.validation.tradeAmount)
    }

    @Test
    fun `amount mode accepts exact rounded tradable minimum`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Letras",
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 2,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "103,36")

        assertEquals("2", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("103.36", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `amount mode rejects one cent below rounded tradable minimum`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 2,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "103,35")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals("103.36", error.minAmount.toPlainString())
        assertEquals("1", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals("51.68", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `amount mode rounds minimum to lot size`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("21.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 5,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "101")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals("105.00", error.minAmount.toPlainString())
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsInvalid ||
                    error is TradeValidationError.NominalsBelowMin ||
                    error is TradeValidationError.NominalsNotMultiple
            },
        )
    }

    @Test
    fun `amount mode below instrument minimum nominals reports amount error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("11.00"),
                minInstrumentNominals = 7,
                lotInstrumentSize = 5,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "101")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals("110.00", error.minAmount.toPlainString())
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsBelowMin
            },
        )
    }

    @Test
    fun `quantity mode validates nominal minimum without amount error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 2,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1")

        assertTrue(viewModel.uiState.inputError is TradeValidationError.NominalsBelowMin)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.AmountNotEnoughForMin
            },
        )
    }

    @Test
    fun `amount helper shows available balance before input`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)

        assertEquals("Disponible: $1159000.00", viewModel.uiState.inputHelper.toLegacyMessage())
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `quantity helper shows available balance before input`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)

        assertEquals("Disponible: $1159000.00", viewModel.uiState.inputHelper.toLegacyMessage())
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `quantity helper shows available balance when input is zero`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "0")

        assertEquals("Disponible: $1159000.00", viewModel.uiState.inputHelper.toLegacyMessage())
        assertTrue(viewModel.uiState.inputError is TradeValidationError.NominalsInvalid)
    }

    @Test
    fun `buy helper shows equivalent nominals after valid amount`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "40000")

        assertEquals(
            "Estimado a debitar: $39220.00\n(equivale a 2 nominales)",
            viewModel.uiState.inputHelper.toLegacyMessage(),
        )
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `sell helper shows available nominals before input`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(holdingQuantity = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)

        assertEquals("Nominales disponibles 10", viewModel.uiState.inputHelper.toLegacyMessage())
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `sell helper shows equivalent amount after valid quantity`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(holdingQuantity = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1")

        assertEquals("Equivale a $19580.00", viewModel.uiState.inputHelper.toLegacyMessage())
        assertEquals(null, viewModel.uiState.inputError)
    }

    @Test
    fun `buy ignores max instrument nominals and validates against available balance`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(maxInstrumentNominals = 50),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "100")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(viewModel.uiState.inputError is TradeValidationError.NominalsOverAvailableBalance)
    }

    @Test
    fun `buy market rejects missing ask price without derived amount errors`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal.ZERO,
                percentageMovement = BigDecimal.ZERO,
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "100")

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
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                bidPrice00 = BigDecimal.ZERO,
                percentageMovement = BigDecimal.ZERO,
            ),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onOrderTypeChange(TradeOrderType.Market)
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "100")

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
    fun `limit with empty price and trade input shows limit price error only`() {
        val viewModel = tradeViewModel()

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1000")

        assertEquals(TradeValidationError.InvalidLimitPrice, viewModel.uiState.limitPriceError)
        assertEquals(null, viewModel.uiState.inputError)
        assertEquals(listOf(TradeValidationError.InvalidLimitPrice), viewModel.uiState.validation.errors)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradePrice)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.validation.tradeAmount)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.AmountNotEnoughForMin ||
                    error is TradeValidationError.OperationAmountBelowMin ||
                    error is TradeValidationError.NominalsInvalid
            },
        )
    }

    @Test
    fun `quantity mode rejects zero nominals`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "0")

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
        viewModel.onInputChange(viewModel.uiState.inputMode, "3")

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
        viewModel.onInputChange(viewModel.uiState.inputMode, "7")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("multiplo de 5")
            },
        )
    }

    @Test
    fun `quantity mode treats dot as thousand separator`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "2.500")

        assertEquals(BigDecimal("2500"), viewModel.uiState.activeInput)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsNotMultiple
            },
        )
    }

    @Test
    fun `quantity mode ignores instrument maximum when account balance is enough`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("30.00"),
                maxInstrumentNominals = 5,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "6")

        assertEquals(null, viewModel.uiState.inputError)
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `quantity mode rejects nominals above account balance maximum`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                maxInstrumentNominals = 100,
                lotInstrumentSize = 1,
            ),
        )
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("500.00")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "6")

        val error = viewModel.uiState.inputError as TradeValidationError.NominalsOverAvailableBalance
        assertEquals(BigDecimal("5"), error.maxAffordableNominals)
        assertEquals("Supera tu saldo disponible", error.toLegacyMessage())
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `quantity mode uses raw balance for ars fima accounts`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("10.00"),
                lotInstrumentSize = 1,
                minTradeAmount = BigDecimal("100.00"),
            ),
        )
        val fimaAccount = Account(
            number = "FIMA-ARS-001",
            type = "FIMA",
            currency = "ARS",
            balance = BigDecimal("500000000.93"),
        )
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = fimaAccount,
            availableArsAccounts = listOf(fimaAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10")

        assertEquals(null, viewModel.uiState.inputError)
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `quantity mode rejects nominals above account balance even when instrument maximum is lower`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                maxInstrumentNominals = 4,
                lotInstrumentSize = 1,
            ),
        )
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("300.00")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "6")

        val error = viewModel.uiState.inputError as TradeValidationError.NominalsOverAvailableBalance
        assertEquals(BigDecimal("3"), error.maxAffordableNominals)
        assertEquals("Supera tu saldo disponible", error.toLegacyMessage())
        assertFalse(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `sell by quantity rejects nominals over available holdings`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(holdingQuantity = 10),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "11")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(viewModel.uiState.inputError is TradeValidationError.NotEnoughNominals)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("Supera tu disponible")
            },
        )
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsOverMax
            },
        )
    }

    @Test
    fun `limit price rejects invalid multiple for stock range`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(liderMerval = true))

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("19.210,25")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("multiplo de 10.00")
            },
        )
    }

    @Test
    fun `limit price accepts valid multiple for letras`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(type = "Letras"),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("19.210,12")

        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("multiplo")
            },
        )
    }

    @Test
    fun `limit price validates bond multiple by price range`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(type = "Bonos"))

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("100,03")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("multiplo de 0.05")
            },
        )
    }

    @Test
    fun `limit price validates cedear and etf multiple by price range`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(type = "CEDEARs"))

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("150,10")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("multiplo de 0.25")
            },
        )
    }

    @Test
    fun `limit price validates non leader stock multiple by price range`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(TestBuyableInstrument.copy(type = "Acciones", liderMerval = false))

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("75,10")

        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("multiplo de 0.20")
            },
        )
    }

    @Test
    fun `limit price accepts valid high range multiple for non leader stock`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Acciones",
                liderMerval = false,
                askPrice00 = BigDecimal("60000.00"),
                percentageMovement = BigDecimal("1.00"),
            ),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("50.050")

        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.LimitPriceNotMultiple
            },
        )
    }

    @Test
    fun `buy ars rejects insufficient ars balance by trade amount`() {
        val viewModel = tradeViewModel()
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("1000.00")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("Supera tu saldo disponible")
            },
        )
    }

    @Test
    fun `buy usd validates usd amount balance`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            viewModel.uiState.instrument!!.copy(currency = "USD"),
        )
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("50.00"))),
            availableArsAccounts = emptyList(),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "40000")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("dolares")
            },
        )
    }

    @Test
    fun `buy usd amount within balance and max nominals does not show max quantity error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                ticker = "ALUA22",
                description = "Aluar",
                currency = "USD",
                askPrice00 = BigDecimal("5993.26"),
                minInstrumentNominals = 1,
                maxInstrumentNominals = 4,
                lotInstrumentSize = 1,
            ),
        )
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("25000.00"))),
            availableArsAccounts = listOf(accountWithBalances("ARS", tradingBalances(BigDecimal("1000.00")))),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10.000")

        assertEquals("1", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsOverMax
            },
        )
        assertEquals(null, viewModel.uiState.inputError)
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy amount ignores instrument nominal maximum when account balance is enough`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                ticker = "ALUA22",
                description = "Aluar",
                currency = "USD",
                askPrice00 = BigDecimal("2000.00"),
                minInstrumentNominals = 1,
                maxInstrumentNominals = 4,
                lotInstrumentSize = 1,
            ),
        )
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("25000.00"))),
            availableArsAccounts = listOf(accountWithBalances("ARS", tradingBalances(BigDecimal("1000.00")))),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10.000")

        assertEquals("5", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertEquals(null, viewModel.uiState.inputError)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsOverMax
            },
        )
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy usd large amount shows max amount error instead of minimum amount error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                ticker = "SUPV20",
                description = "Grupo Supervielle",
                currency = "USD",
                askPrice00 = BigDecimal("70.61"),
                minInstrumentNominals = 1,
                maxInstrumentNominals = 999999999,
                lotInstrumentSize = 2,
            ),
        )
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("25000.00"))),
            availableArsAccounts = listOf(accountWithBalances("ARS", tradingBalances(BigDecimal("1000.00")))),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10.000.000")

        assertTrue(viewModel.uiState.inputError is TradeValidationError.OperationAmountAboveMax)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.AmountNotEnoughForMin
            },
        )
    }

    @Test
    fun `buy ars large amount shows max amount error instead of minimum amount error`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                ticker = "CEPU7",
                description = "Central Puerto",
                currency = "ARS",
                askPrice00 = BigDecimal("2496.33"),
                minInstrumentNominals = 2,
                maxInstrumentNominals = 999999999,
                lotInstrumentSize = 2,
            ),
        )
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("89690294.9278")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "100.000.000")

        assertTrue(viewModel.uiState.inputError is TradeValidationError.OperationAmountAboveMax)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.AmountNotEnoughForMin
            },
        )
    }

    @Test
    fun `buy usd quantity accepts exact maximum nominals`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                ticker = "ALUA22",
                description = "Aluar",
                currency = "USD",
                askPrice00 = BigDecimal("5993.26"),
                minInstrumentNominals = 1,
                maxInstrumentNominals = 4,
                lotInstrumentSize = 1,
            ),
        )
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("25000.00"))),
            availableArsAccounts = listOf(accountWithBalances("ARS", tradingBalances(BigDecimal("1000.00")))),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "4")

        assertEquals("4", viewModel.uiState.validation.tradeNominals.toPlainString())
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsOverMax
            },
        )
        assertEquals(null, viewModel.uiState.inputError)
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `quantity with thousand dots validates multiple against parsed digits`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                ticker = "TXAR17",
                description = "Ternium Argentina",
                askPrice00 = BigDecimal("53.71"),
                minInstrumentNominals = 1,
                maxInstrumentNominals = 999999999,
                lotInstrumentSize = 2,
            ),
        )
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("999999999.00")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10.000.000")

        assertEquals(BigDecimal("10000000"), viewModel.uiState.activeInput)
        assertFalse(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.NominalsNotMultiple
            },
        )
    }

    @Test
    fun `selecting usd account keeps fee commission account unselected`() {
        val viewModel = tradeViewModel()
        val usdAccount = accountWithBalances("USD", tradingBalances(BigDecimal("1000.00")))
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("7.01")))
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
                currency = "USD",
            ),
        )

        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = usdAccount,
            availableArsAccounts = listOf(arsAccount),
        )
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10")
        viewModel.prepareConfirmation()

        assertEquals(usdAccount, viewModel.uiState.selectedAccount)
        assertEquals(listOf(arsAccount), viewModel.uiState.availableFeeCommissionAccounts)
        assertEquals(null, viewModel.uiState.selectedFeeCommissionAccount)
        assertTrue(viewModel.uiState.validation.canContinue)
        assertTrue(viewModel.uiState.confirmation.errors.contains(TradeValidationError.FeeAccountNotSelected))
    }

    @Test
    fun `selecting fee account revalidates confirmation when several ars accounts exist`() {
        val viewModel = tradeViewModel()
        val firstArsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("312.01")))
        val secondArsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("312.02"))).copy(number = "ARS-002")
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
                currency = "USD",
            ),
        )
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("1000.00"))),
            availableArsAccounts = listOf(firstArsAccount, secondArsAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10")
        viewModel.prepareConfirmation()

        assertTrue(viewModel.uiState.confirmation.errors.contains(TradeValidationError.FeeAccountNotSelected))

        viewModel.onFeeCommissionAccountSelected(secondArsAccount)

        assertEquals(secondArsAccount, viewModel.uiState.selectedFeeCommissionAccount)
        assertTrue(viewModel.uiState.confirmation.canConfirm)
    }

    @Test
    fun `buy usd limit by amount uses limit price and excludes fee from minimum`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 2,
                lotInstrumentSize = 1,
                currency = "USD",
            ),
        )
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("1000.00"))),
            availableArsAccounts = emptyList(),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("51,68")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "100")

        val error = viewModel.uiState.inputError as TradeValidationError.AmountNotEnoughForMin
        assertEquals("103.36", error.minAmount.toPlainString())
        assertEquals("51.68", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `buy usd limit by amount accepts exact rounded minimum`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                type = "Letras",
                askPrice00 = BigDecimal("51.68"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
                currency = "USD",
            ),
        )
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("1000.00"))),
            availableArsAccounts = emptyList(),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("51,68")
        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "103,36")

        assertEquals("103.36", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals(null, viewModel.uiState.inputError)
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `trade amount excludes fee in input validation`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "20000")

        assertEquals("19610.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `buy rejects operation amount above maximum amount`() {
        val viewModel = tradeViewModel()
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("1000.00")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "20000")

        assertFalse(viewModel.uiState.validation.canContinue)
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error.toLegacyMessage().contains("Supera tu saldo disponible")
            },
        )
    }

    @Test
    fun `buy quantity limit rejects operation amount above maximum amount`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                percentageMovement = BigDecimal("1.00"),
            ),
        )
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("500.00")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("100")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "6")

        assertEquals("600.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.OperationAmountAboveMax
            },
        )
        assertEquals(null, viewModel.uiState.limitPriceError)
    }

    @Test
    fun `buy quantity limit accepts exact maximum amount`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                percentageMovement = BigDecimal("1.00"),
            ),
        )
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("500.00")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onOrderTypeChange(TradeOrderType.Limit)
        viewModel.onLimitPriceChange("100")
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "5")

        assertEquals("500.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals(null, viewModel.uiState.inputError)
        assertTrue(viewModel.uiState.validation.canContinue)
    }

    @Test
    fun `buy amount rejects operation amount one cent above maximum without fee`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("30.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
            ),
        )
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("89.99")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "101")

        assertEquals("90.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertTrue(
            viewModel.uiState.validation.errors.any { error ->
                error is TradeValidationError.OperationAmountAboveMax
            },
        )
    }

    @Test
    fun `buy ars confirmation adds fee to amount with fee`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
            ),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10")
        viewModel.prepareConfirmation()

        assertEquals("1000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("7.02", viewModel.uiState.confirmation.operationFee.toPlainString())
        assertEquals("5.00", viewModel.uiState.confirmation.marketFee.toPlainString())
        assertEquals("300.00", viewModel.uiState.confirmation.taxes.toPlainString())
        assertEquals("312.02", viewModel.uiState.confirmation.totalDeductions.toPlainString())
        assertEquals("1000.00", viewModel.uiState.confirmation.subTotal.toPlainString())
        assertEquals("1312.02", viewModel.uiState.confirmation.estimatedAmount.toPlainString())
        assertTrue(viewModel.uiState.confirmation.canConfirm)
    }

    @Test
    fun `buy ars confirmation requires ars balance for amount plus fees`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
            ),
        )
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("1000.00")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = arsAccount,
            availableArsAccounts = listOf(arsAccount),
        )

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10")
        viewModel.prepareConfirmation()

        assertTrue(viewModel.uiState.validation.canContinue)
        assertEquals("1312.02", viewModel.uiState.confirmation.estimatedAmount.toPlainString())
        assertTrue(viewModel.uiState.confirmation.errors.contains(TradeValidationError.InsufficientArsForFee))
        assertFalse(viewModel.uiState.confirmation.canConfirm)
    }

    @Test
    fun `sell ars confirmation subtracts all fees from estimated amount`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                bidPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
                holdingQuantity = 20,
            ),
        )

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10")
        viewModel.prepareConfirmation()

        assertTrue(viewModel.uiState.validation.canContinue)
        assertEquals("1000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("312.02", viewModel.uiState.confirmation.totalDeductions.toPlainString())
        assertEquals("687.98", viewModel.uiState.confirmation.estimatedAmount.toPlainString())
        assertTrue(viewModel.uiState.confirmation.canConfirm)
    }

    @Test
    fun `buy usd confirmation does not add fees to amount and requires ars balance for fees`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                askPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
                currency = "USD",
            ),
        )
        val arsFeeAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("7.01")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("1000.00"))),
            availableArsAccounts = listOf(arsFeeAccount),
        )
        viewModel.onFeeCommissionAccountSelected(arsFeeAccount)

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10")
        viewModel.prepareConfirmation()

        assertTrue(viewModel.uiState.validation.canContinue)
        assertEquals("1000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("7.02", viewModel.uiState.confirmation.operationFee.toPlainString())
        assertEquals("312.02", viewModel.uiState.confirmation.totalDeductions.toPlainString())
        assertEquals("1000.00", viewModel.uiState.confirmation.subTotal.toPlainString())
        assertTrue(viewModel.uiState.confirmation.errors.contains(TradeValidationError.InsufficientArsForFee))
        assertFalse(viewModel.uiState.confirmation.canConfirm)
    }

    @Test
    fun `sell usd confirmation requires ars balance for fees`() {
        val viewModel = tradeViewModel()
        viewModel.replaceInstrument(
            TestBuyableInstrument.copy(
                bidPrice00 = BigDecimal("100.00"),
                minInstrumentNominals = 1,
                lotInstrumentSize = 1,
                holdingQuantity = 20,
                currency = "USD",
            ),
        )
        val arsFeeAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("7.01")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("1000.00"))),
            availableArsAccounts = listOf(arsFeeAccount),
        )
        viewModel.onFeeCommissionAccountSelected(arsFeeAccount)

        viewModel.onTradeTypeChange(TradeType.Sell)
        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "10")
        viewModel.prepareConfirmation()

        assertTrue(viewModel.uiState.validation.canContinue)
        assertEquals("1000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
        assertEquals("312.02", viewModel.uiState.confirmation.totalDeductions.toPlainString())
        assertEquals("1000.00", viewModel.uiState.confirmation.subTotal.toPlainString())
        assertTrue(viewModel.uiState.confirmation.errors.contains(TradeValidationError.InsufficientArsForFee))
        assertFalse(viewModel.uiState.confirmation.canConfirm)
    }

    @Test
    fun `amount parser handles thousand dots as whole number separators`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "40.000")

        assertEquals("40.000", viewModel.uiState.amountInputText)
        assertEquals(BigDecimal("40000"), viewModel.uiState.activeInput)
        assertEquals("39220.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `quantity parser handles thousand dots as whole number separators`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1.000")

        assertEquals("1.000", viewModel.uiState.quantityInputText)
        assertEquals(BigDecimal("1000"), viewModel.uiState.activeInput)
        assertEquals("19610000.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `amount parser treats dot as thousand separator`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "313.0")

        assertEquals("313.0", viewModel.uiState.amountInputText)
        assertEquals(BigDecimal("3130"), viewModel.uiState.activeInput)
        assertEquals("0.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `amount parser accepts comma decimal amount input`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "313,25")

        assertEquals("313,25", viewModel.uiState.amountInputText)
        assertEquals(BigDecimal("313.25"), viewModel.uiState.activeInput)
        assertEquals("0.00", viewModel.uiState.validation.tradeAmount.toPlainString())
    }

    @Test
    fun `quantity parser treats dot as thousand separator`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "313.0")

        assertEquals("313.0", viewModel.uiState.quantityInputText)
        assertEquals(BigDecimal("3130"), viewModel.uiState.activeInput)
    }

    @Test
    fun `quantity parser drops decimal comma`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Quantity)
        viewModel.onInputChange(viewModel.uiState.inputMode, "1,5")

        assertEquals("15", viewModel.uiState.quantityInputText)
        assertEquals(BigDecimal("15"), viewModel.uiState.activeInput)
    }

    @Test
    fun `amount parser keeps trailing thousand dot as visual input but parses digits`() {
        val viewModel = tradeViewModel()

        viewModel.onInputModeChange(BuyInputMode.Amount)
        viewModel.onInputChange(viewModel.uiState.inputMode, "0.")

        assertEquals("0.", viewModel.uiState.amountInputText)
        assertEquals(BigDecimal.ZERO, viewModel.uiState.activeInput)
        assertEquals(BigDecimal.ZERO.setScale(2), viewModel.uiState.validation.tradeAmount)
        assertFalse(viewModel.uiState.validation.canContinue)
    }
}

private fun tradeViewModel(): TradeViewModel =
    TradeViewModel(FakeBuySecuritiesRepository(), TradeValidator()).also { viewModel ->
        viewModel.replaceInstrument(TestBuyableInstrument)
        val selectedAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("1159000.00")))
        viewModel.onTradeMonetaryAccountSelected(
            selectedAccount = selectedAccount,
            availableArsAccounts = listOf(selectedAccount),
        )
    }

private val TestBuyableInstrument = Security(
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
    maxInstrumentNominals = 999999999,
    lotInstrumentSize = 1,
    price = BigDecimal("40005.75"),
    dailyVariationPercent = BigDecimal("0.27"),
    askPrice00 = BigDecimal("19610.00"),
    bidPrice00 = BigDecimal("19580.00"),
    askPrice24 = BigDecimal("19610.00"),
    bidPrice24 = BigDecimal("19580.00"),
    percentageMovement = BigDecimal("0.15"),
    minTradeAmount = BigDecimal("100.00"),
)

private class FakeBuySecuritiesRepository : MockSecuritiesRepository() {
    override fun getCachedSecurities(): List<Security>? = null

    override suspend fun refreshSecurities(): List<Security> = emptyList()

    override suspend fun getBuyableInstruments(): List<Security> = emptyList()

    override suspend fun getBuyableInstrument(
        codeType: String,
        codeValue: String,
    ): Security? = null
}

private fun tradingBalances(balance: BigDecimal): TradingBalanceSet =
    TradingBalanceSet(
        limitNow = balance,
        limit24 = balance,
        marketNow = balance,
        market24 = balance,
    )

private fun accountWithBalances(
    currency: String,
    balances: TradingBalanceSet,
): Account =
    Account(
        number = "$currency-001",
        currency = currency,
        balanceLimitNow = balances.limitNow.toPlainString(),
        balanceLimit24 = balances.limit24.toPlainString(),
        balanceMarketNow = balances.marketNow.toPlainString(),
        balanceMarket24 = balances.market24.toPlainString(),
    )

private fun TradeViewModel.onTradeMonetaryAccountSelected(
    selectedAccount: Account,
    availableArsAccounts: List<Account>,
) {
    replaceAccounts((listOf(selectedAccount) + availableArsAccounts).distinctBy { account -> account.number })
    onTradeMonetaryAccountSelected(selectedAccount)
}

private fun TradeInputHelper.toLegacyMessage(): String =
    when (this) {
        TradeInputHelper.None -> ""
        is TradeInputHelper.AvailableBalance -> "Saldo disponible $${amount.toMoneyStringForTest()}"
        is TradeInputHelper.AvailableToBuy -> "Disponible: $${amount.toMoneyStringForTest()}"
        is TradeInputHelper.AvailableNominalsToBuy -> "Disponible ${quantity.toNominalsStringForTest()} nominales"
        is TradeInputHelper.AvailableNominals -> "Nominales disponibles $quantity"
        is TradeInputHelper.EquivalentAmount -> "Equivale a $${amount.toMoneyStringForTest()}"
        is TradeInputHelper.EquivalentNominals ->
            "Estimado a debitar: $${amount.toMoneyStringForTest()}\n" +
                    "(equivale a ${quantity.toNominalsStringForTest()} nominales)"
        is TradeInputHelper.ApproximateDebit -> "Estimado a debitar: $${amount.toMoneyStringForTest()}"
        is TradeInputHelper.ApproximateCredit -> "Valor aproximado a acreditar $${amount.toMoneyStringForTest()}"
    }

private fun TradeValidationError.toLegacyMessage(): String =
    when (this) {
        TradeValidationError.InvalidLimitPrice -> "Ingresa un precio limite valido"
        is TradeValidationError.LimitPriceOutOfBandBuy -> "El maximo permitido es: $${maxAllowed.toMoneyStringForTest()}"
        is TradeValidationError.LimitPriceOutOfBandSell -> "El minimo permitido es $${minAllowed.toMoneyStringForTest()}"
        is TradeValidationError.LimitPriceNotMultiple -> "El monto debe ser multiplo de ${step.toPlainString()}"
        TradeValidationError.MissingTradePrice -> "No se pudo obtener un precio valido para la operacion"
        is TradeValidationError.AmountNotEnoughForMin -> "El monto minimo a comprar es $${minAmount.toMoneyStringForTest()}"
        TradeValidationError.NominalsInvalid -> "La cantidad debe ser mayor a cero"
        is TradeValidationError.NominalsBelowMin -> "La cantidad minima a comprar es ${minNominals.toPlainString()}"
        is TradeValidationError.NominalsNotMultiple -> "El valor debe ser multiplo de ${lotSize.toPlainString()}"
        is TradeValidationError.NominalsOverMax -> "La cantidad maxima a comprar es ${maxNominals.toPlainString()} nominales"
        is TradeValidationError.NominalsOverAvailableBalance -> "Supera tu saldo disponible"
        is TradeValidationError.NotEnoughNominals -> "Supera tu disponible"
        is TradeValidationError.NotEnoughAvailableAmount -> "Supera tu disponible"
        is TradeValidationError.InsufficientArs -> "Supera tu saldo disponible"
        TradeValidationError.InsufficientArsForFee -> "Saldo insuficiente en pesos para fee"
        TradeValidationError.MissingArsFeeAccount -> "No hay cuenta en pesos para debitar el fee"
        TradeValidationError.FeeAccountNotSelected -> "Selecciona una cuenta en pesos para debitar el fee"
        is TradeValidationError.InsufficientUsd -> "Saldo insuficiente en dolares"
        TradeValidationError.SelectedAccountCurrencyMismatch -> "La cuenta seleccionada no corresponde a la moneda del instrumento"
        is TradeValidationError.OperationAmountBelowMin ->
            minNominals?.let { "La cantidad minima a comprar es ${it.toPlainString()}" }
                ?: "El monto minimo a comprar es $${minAmount.toMoneyStringForTest()}"
        is TradeValidationError.OperationAmountAboveMax -> "Supera tu saldo disponible"
    }

private fun BigDecimal.toMoneyStringForTest(): String =
    setScale(2).toPlainString()

private fun BigDecimal.toNominalsStringForTest(): String =
    stripTrailingZeros().toPlainString()
