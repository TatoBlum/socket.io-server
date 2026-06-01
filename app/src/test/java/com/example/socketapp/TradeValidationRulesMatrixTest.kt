package com.example.socketapp

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeValidationRulesMatrixTest {
    private val validator = TradeValidator()

    @Test
    fun `01 validate and build order by amount returns canonical rounded values`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("100.00"),
                    minInstrumentNominals = 2,
                    lotInstrumentSize = 2,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "250",
            ),
        )

        assertEquals("100.00", result.tradePrice.toPlainString())
        assertEquals("2", result.tradeNominals.toPlainString())
        assertEquals("200.00", result.tradeAmount.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `02 validate and build order by quantity with limit uses limit price`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("100.00"),
                    percentageMovement = BigDecimal("1.00"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "90",
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "3",
            ),
        )

        assertEquals("90", result.tradePrice.toPlainString())
        assertEquals("3", result.tradeNominals.toPlainString())
        assertEquals("270.00", result.tradeAmount.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `03 validate and build order by amount rejects amount below first tradable lot`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("100.00"),
                    minInstrumentNominals = 2,
                    lotInstrumentSize = 2,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "199",
            ),
        )

        val error = result.errors.single() as TradeValidationError.AmountNotEnoughForMin
        assertEquals("200.00", error.minAmount.toPlainString())
        assertEquals("0.00", result.tradeAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `03b buy ars minimum amount uses instrument server value`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("10.00"),
                    minBuyArsAmount = BigDecimal("250.00"),
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "200",
            ),
        )

        val error = result.errors.single() as TradeValidationError.OperationAmountBelowMin
        assertEquals("250.00", error.minAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `04 sell by amount max for market uses holding quantity and bid price`() {
        val result = validator.validate(
            state(
                tradeType = TradeType.Sell,
                instrument = instrument(
                    bidPrice00 = BigDecimal("100.00"),
                    holdingQuantity = 10,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "1001",
            ),
        )

        val error = result.errors.single() as TradeValidationError.NotEnoughAvailableAmount
        assertEquals("1000.00", error.availableAmount.toPlainString())
        assertEquals("1000.00", result.tradeAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `05 sell by amount max for limit uses holding quantity and limit price`() {
        val result = validator.validate(
            state(
                tradeType = TradeType.Sell,
                instrument = instrument(
                    bidPrice00 = BigDecimal("100.00"),
                    holdingQuantity = 10,
                    percentageMovement = BigDecimal("0.20"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "90",
                inputMode = BuyInputMode.Amount,
                amountInputText = "901",
            ),
        )

        val error = result.errors.single() as TradeValidationError.NotEnoughAvailableAmount
        assertEquals("900.00", error.availableAmount.toPlainString())
        assertEquals("900.00", result.tradeAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `06 limit price band for buy rejects price above ask plus movement`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("100.00"),
                    percentageMovement = BigDecimal("0.10"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "111",
            ),
        )

        val error = result.errors.single() as TradeValidationError.LimitPriceOutOfBandBuy
        assertEquals("110.00", error.maxAllowed.toPlainString())
    }

    @Test
    fun `07 limit price band for sell rejects price below bid minus movement`() {
        val result = validator.validate(
            state(
                tradeType = TradeType.Sell,
                instrument = instrument(
                    bidPrice00 = BigDecimal("100.00"),
                    percentageMovement = BigDecimal("0.10"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "89",
            ),
        )

        val error = result.errors.single() as TradeValidationError.LimitPriceOutOfBandSell
        assertEquals("90.00", error.minAllowed.toPlainString())
    }

    @Test
    fun `08 limit price multiple rejects price outside tick size`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    type = "Acciones",
                    liderMerval = false,
                    askPrice00 = BigDecimal("100.00"),
                    percentageMovement = BigDecimal("0.50"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "75,10",
            ),
        )

        val error = result.errors.single() as TradeValidationError.LimitPriceNotMultiple
        assertEquals("0.20", error.step.toPlainString())
    }

    @Test
    fun `09 minimum amount is derived from minimum nominal rounded to lot`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("10.00"),
                    minInstrumentNominals = 2,
                    lotInstrumentSize = 5,
                    currency = "USD",
                ),
                accountContext = accountContext(selectedCurrency = "USD"),
                inputMode = BuyInputMode.Amount,
                amountInputText = "49",
            ),
        )

        val error = result.errors.single() as TradeValidationError.AmountNotEnoughForMin
        assertEquals("50.00", error.minAmount.toPlainString())
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(result.tradeAmount))
        assertFalse(result.canContinue)
    }

    @Test
    fun `09b buy ars amount below channel minimum rejects with channel minimum`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("30.00"),
                    minInstrumentNominals = 1,
                    lotInstrumentSize = 1,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "30",
            ),
        )

        val error = result.errors.single() as TradeValidationError.OperationAmountBelowMin
        assertEquals("100.00", error.minAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `09c buy ars amount below first nominal and channel minimum reports channel minimum`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("30.00"),
                    minInstrumentNominals = 1,
                    lotInstrumentSize = 1,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "20",
            ),
        )

        val error = result.errors.single() as TradeValidationError.OperationAmountBelowMin
        assertEquals("100.00", error.minAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `09c2 buy ars exact channel minimum fails by channel rule`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("100.00"),
                    minInstrumentNominals = 1,
                    lotInstrumentSize = 1,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "100",
            ),
        )

        val error = result.errors.single() as TradeValidationError.OperationAmountBelowMin
        assertEquals("100.00", error.minAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `09c3 buy ars amount one cent below channel minimum fails by channel rule`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("99.99"),
                    minInstrumentNominals = 1,
                    lotInstrumentSize = 1,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "99,99",
            ),
        )

        val error = result.errors.single() as TradeValidationError.OperationAmountBelowMin
        assertEquals("100.00", error.minAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `09c3b buy ars amount input above channel minimum does not fail when rounded operation amount is below channel minimum`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("79.17"),
                    minInstrumentNominals = 1,
                    lotInstrumentSize = 1,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "101",
            ),
        )

        assertEquals(emptyList<TradeValidationError>(), result.errors)
        assertEquals("1", result.tradeNominals.toPlainString())
        assertEquals("79.17", result.tradeAmount.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `09c4 buy ars amount equal channel minimum reports channel minimum even when instrument minimum is higher`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("98.00"),
                    minInstrumentNominals = 2,
                    lotInstrumentSize = 1,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "100",
            ),
        )

        val error = result.errors.single() as TradeValidationError.OperationAmountBelowMin
        assertEquals("100.00", error.minAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `09c5 buy ars amount above channel minimum reports instrument operation minimum when higher`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("84.56"),
                    minInstrumentNominals = 2,
                    lotInstrumentSize = 1,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "101",
            ),
        )

        val error = result.errors.single() as TradeValidationError.AmountNotEnoughForMin
        assertEquals("169.12", error.minAmount.toPlainString())
        assertEquals("1", result.tradeNominals.toPlainString())
        assertEquals("84.56", result.tradeAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `09d buy ars quantity below channel minimum rejects with operation minimum`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("30.00"),
                    minInstrumentNominals = 1,
                    lotInstrumentSize = 1,
                ),
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "1",
            ),
        )

        val error = result.errors.single() as TradeValidationError.OperationAmountBelowMin
        assertEquals("100.00", error.minAmount.toPlainString())
        assertEquals("30.00", result.tradeAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `10 operation amount limits reject amount above maximum`() {
        val result = validator.validate(
            state(
                instrument = instrument(askPrice00 = BigDecimal("100.00")),
                accountContext = accountContext(
                    arsBalance = BigDecimal("1000.00"),
                ),
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "11",
            ),
        )

        val error = result.errors.filterIsInstance<TradeValidationError.OperationAmountAboveMax>().single()
        assertEquals("1000.00", error.maxAmount.toPlainString())
        assertEquals("1100.00", result.tradeAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `11 buy market without ask price returns missing trade price`() {
        val result = validator.validate(
            state(
                instrument = instrument(askPrice00 = BigDecimal.ZERO),
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "1",
            ),
        )

        assertEquals(listOf(TradeValidationError.MissingTradePrice), result.errors)
        assertEquals("0", result.tradePrice.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `12 sell market without bid price returns missing trade price`() {
        val result = validator.validate(
            state(
                tradeType = TradeType.Sell,
                instrument = instrument(bidPrice00 = BigDecimal.ZERO),
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "1",
            ),
        )

        assertEquals(listOf(TradeValidationError.MissingTradePrice), result.errors)
        assertEquals("0", result.tradePrice.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `13 invalid limit price cannot build valid order`() {
        val result = validator.validate(
            state(
                orderType = TradeOrderType.Limit,
                limitPriceInput = "0",
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "10",
            ),
        )

        assertEquals(listOf(TradeValidationError.InvalidLimitPrice), result.errors)
        assertEquals("0", result.tradePrice.toPlainString())
        assertEquals("0", result.tradeNominals.toPlainString())
        assertEquals("0", result.tradeAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `13b empty limit price is invalid if validator receives it`() {
        val result = validator.validate(
            state(
                orderType = TradeOrderType.Limit,
                limitPriceInput = "",
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "10",
            ),
        )

        assertEquals(listOf(TradeValidationError.InvalidLimitPrice), result.errors)
        assertEquals("0", result.tradePrice.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `14 sell market by amount accepts exact max sellable amount`() {
        val result = validator.validate(
            state(
                tradeType = TradeType.Sell,
                instrument = instrument(
                    bidPrice00 = BigDecimal("100.00"),
                    holdingQuantity = 10,
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "1000",
            ),
        )

        assertEquals(emptyList<TradeValidationError>(), result.errors)
        assertEquals("10", result.tradeNominals.toPlainString())
        assertEquals("1000.00", result.tradeAmount.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `14b sell does not validate operation amount against selected account cash balance`() {
        val result = validator.validate(
            state(
                tradeType = TradeType.Sell,
                instrument = instrument(
                    bidPrice00 = BigDecimal("100.00"),
                    holdingQuantity = 10,
                ),
                accountContext = accountContext(
                    arsBalance = BigDecimal("1.00"),
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "500",
            ),
        )

        assertFalse(result.errors.any { error -> error is TradeValidationError.OperationAmountAboveMax })
        assertEquals("500.00", result.tradeAmount.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `15 buy ars validates trade amount against ars balance`() {
        val result = validator.validate(
            state(
                instrument = instrument(askPrice00 = BigDecimal("100.00")),
                accountContext = accountContext(arsBalance = BigDecimal("500.00")),
                inputMode = BuyInputMode.Amount,
                amountInputText = "1000",
            ),
        )

        val error = result.errors.filterIsInstance<TradeValidationError.InsufficientArs>().single()
        assertEquals(BuyInputMode.Amount, error.operationMode)
        assertEquals("1000.00", result.tradeAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `16 buy usd validates trade amount against selected usd balance`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("100.00"),
                    currency = "USD",
                ),
                accountContext = accountContext(
                    usdBalance = BigDecimal("500.00"),
                    selectedCurrency = "USD",
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "1000",
            ),
        )

        assertTrue(result.errors.any { error -> error is TradeValidationError.InsufficientUsd })
        assertEquals("1000.00", result.tradeAmount.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `16a buy rejects selected account with different currency than instrument`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("100.00"),
                    currency = "USD",
                ),
                accountContext = accountContext(
                    arsBalance = BigDecimal("1000.00"),
                    selectedCurrency = "ARS",
                ),
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "1",
            ),
        )

        assertEquals("100.00", result.tradeAmount.toPlainString())
        assertTrue(result.errors.contains(TradeValidationError.SelectedAccountCurrencyMismatch))
        assertFalse(result.canContinue)
    }

    @Test
    fun `16b currency comparisons are case insensitive`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("100.00"),
                    currency = "usd",
                ),
                accountContext = TradeAccountContext(
                    selectedAccount = accountWithBalances(
                        currency = "usd",
                        balances = tradingBalances(BigDecimal("500.00")),
                    ),
                    availableArsAccounts = listOf(
                        accountWithBalances(
                            currency = "ars",
                            balances = tradingBalances(BigDecimal("999999999.99")),
                        ),
                    ),
                ),
                inputMode = BuyInputMode.Amount,
                amountInputText = "1000",
            ),
        )

        assertTrue(result.errors.any { error -> error is TradeValidationError.InsufficientUsd })
        assertFalse(result.canContinue)
    }

    @Test
    fun `17 buy limit accepts exact upper price band`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("100.00"),
                    percentageMovement = BigDecimal("0.10"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "110",
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "1",
            ),
        )

        assertFalse(result.errors.any { error -> error is TradeValidationError.LimitPriceOutOfBandBuy })
        assertEquals("110", result.tradePrice.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `18 sell limit accepts exact lower price band`() {
        val result = validator.validate(
            state(
                tradeType = TradeType.Sell,
                instrument = instrument(
                    bidPrice00 = BigDecimal("100.00"),
                    holdingQuantity = 10,
                    percentageMovement = BigDecimal("0.10"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "90",
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "1",
            ),
        )

        assertFalse(result.errors.any { error -> error is TradeValidationError.LimitPriceOutOfBandSell })
        assertEquals("90", result.tradePrice.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `18b buy limit normalizes negative percentage movement`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("102.42"),
                    percentageMovement = BigDecimal("-1.82"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "105",
            ),
        )

        val error = result.errors.single() as TradeValidationError.LimitPriceOutOfBandBuy
        assertEquals("104.28", error.maxAllowed.toPlainString())
        assertFalse(result.canContinue)
    }

    @Test
    fun `18c buy limit accepts displayed rounded upper price band`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("122.52"),
                    percentageMovement = BigDecimal("0.15"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "140,90",
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "1",
            ),
        )

        assertFalse(result.errors.any { error -> error is TradeValidationError.LimitPriceOutOfBandBuy })
        assertEquals("140.90", result.tradePrice.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `18d buy limit accepts price below displayed rounded upper price band`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    askPrice00 = BigDecimal("122.52"),
                    percentageMovement = BigDecimal("0.15"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "140,89",
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "1",
            ),
        )

        assertFalse(result.errors.any { error -> error is TradeValidationError.LimitPriceOutOfBandBuy })
        assertEquals("140.89", result.tradePrice.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `18e sell limit accepts displayed rounded lower price band`() {
        val result = validator.validate(
            state(
                tradeType = TradeType.Sell,
                instrument = instrument(
                    bidPrice00 = BigDecimal("165.77"),
                    holdingQuantity = 10,
                    percentageMovement = BigDecimal("0.15"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "140,90",
                inputMode = BuyInputMode.Quantity,
                quantityInputText = "1",
            ),
        )

        assertFalse(result.errors.any { error -> error is TradeValidationError.LimitPriceOutOfBandSell })
        assertEquals("140.90", result.tradePrice.toPlainString())
        assertTrue(result.canContinue)
    }

    @Test
    fun `19 bond tick size below 100 uses lower range step`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    type = "Bonos",
                    askPrice00 = BigDecimal("100.00"),
                    percentageMovement = BigDecimal("0.50"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "99,99",
            ),
        )

        assertFalse(result.errors.any { error -> error is TradeValidationError.LimitPriceNotMultiple })
    }

    @Test
    fun `20 bond tick size just above 100 uses next range step`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    type = "Bonos",
                    askPrice00 = BigDecimal("101.00"),
                    percentageMovement = BigDecimal("0.50"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "100,01",
            ),
        )

        val error = result.errors.single() as TradeValidationError.LimitPriceNotMultiple
        assertEquals("0.05", error.step.toPlainString())
    }

    @Test
    fun `21 treasury bill accepts three decimal price multiple`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    type = "Letras",
                    askPrice00 = BigDecimal("100.00"),
                    percentageMovement = BigDecimal("0.50"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "50,001",
            ),
        )

        assertFalse(result.errors.any { error -> error is TradeValidationError.LimitPriceNotMultiple })
    }

    @Test
    fun `22 bond rejects three decimals when range step is one cent`() {
        val result = validator.validate(
            state(
                instrument = instrument(
                    type = "Bonos",
                    askPrice00 = BigDecimal("100.00"),
                    percentageMovement = BigDecimal("0.50"),
                ),
                orderType = TradeOrderType.Limit,
                limitPriceInput = "50,001",
            ),
        )

        val error = result.errors.single() as TradeValidationError.LimitPriceNotMultiple
        assertEquals("0.01", error.step.toPlainString())
    }

    @Test
    fun `21 market today uses market now balance`() {
        val result = validator.validate(
            state(
                accountContext = accountContext(
                    arsBalances = TradingBalanceSet(marketNow = BigDecimal("200.00")),
                ),
                orderType = TradeOrderType.Market,
                settlementTerm = SettlementType.TODAY,
                inputMode = BuyInputMode.Amount,
                amountInputText = "200",
            ),
        )

        assertEquals(emptyList<TradeValidationError>(), result.errors)
        assertTrue(result.canContinue)
    }

    @Test
    fun `22 market 24 hours uses market 24 balance`() {
        val result = validator.validate(
            state(
                accountContext = accountContext(
                    arsBalances = TradingBalanceSet(market24 = BigDecimal("200.00")),
                ),
                orderType = TradeOrderType.Market,
                settlementTerm = SettlementType.TWENTY_FOUR_HOURS,
                inputMode = BuyInputMode.Amount,
                amountInputText = "200",
            ),
        )

        assertEquals(emptyList<TradeValidationError>(), result.errors)
        assertTrue(result.canContinue)
    }

    @Test
    fun `23 limit today uses limit now balance`() {
        val result = validator.validate(
            state(
                accountContext = accountContext(
                    arsBalances = TradingBalanceSet(limitNow = BigDecimal("200.00")),
                ),
                orderType = TradeOrderType.Limit,
                settlementTerm = SettlementType.TODAY,
                limitPriceInput = "100",
                inputMode = BuyInputMode.Amount,
                amountInputText = "200",
            ),
        )

        assertEquals(emptyList<TradeValidationError>(), result.errors)
        assertTrue(result.canContinue)
    }

    @Test
    fun `24 limit 24 hours uses limit 24 balance`() {
        val result = validator.validate(
            state(
                accountContext = accountContext(
                    arsBalances = TradingBalanceSet(limit24 = BigDecimal("200.00")),
                ),
                orderType = TradeOrderType.Limit,
                settlementTerm = SettlementType.TWENTY_FOUR_HOURS,
                limitPriceInput = "100",
                inputMode = BuyInputMode.Amount,
                amountInputText = "200",
            ),
        )

        assertEquals(emptyList<TradeValidationError>(), result.errors)
        assertTrue(result.canContinue)
    }

    @Test
    fun `25 buy validation excludes fee and confirmation validation includes fee`() {
        val state = state(
            instrument = instrument(askPrice00 = BigDecimal("100.00")),
            inputMode = BuyInputMode.Quantity,
            quantityInputText = "10",
        )

        val buyValidation = validator.validate(state)
        val confirmationValidation = validator.validateConfirmation(
            state = state,
            validation = buyValidation,
            fee = BigDecimal("7.02"),
            marketFee = BigDecimal("5.00"),
            vat = BigDecimal("300.00"),
        )

        assertEquals("1000.00", buyValidation.tradeAmount.toPlainString())
        assertEquals("7.02", confirmationValidation.fee.toPlainString())
        assertEquals("5.00", confirmationValidation.marketFee.toPlainString())
        assertEquals("300.00", confirmationValidation.vat.toPlainString())
        assertEquals("312.02", confirmationValidation.totalFees.toPlainString())
        assertEquals("1312.02", confirmationValidation.amountWithFee.toPlainString())
        assertTrue(buyValidation.canContinue)
        assertTrue(confirmationValidation.canConfirm)
    }

    @Test
    fun `26 buy usd confirmation validation requires ars balance for fee`() {
        val state = state(
            instrument = instrument(
                askPrice00 = BigDecimal("100.00"),
                currency = "USD",
            ),
            accountContext = accountContext(
                arsBalance = BigDecimal("7.01"),
                usdBalance = BigDecimal("1000.00"),
                selectedCurrency = "USD",
            ),
            inputMode = BuyInputMode.Quantity,
            quantityInputText = "10",
        )

        val buyValidation = validator.validate(state)
        val confirmationValidation = validator.validateConfirmation(
            state = state,
            validation = buyValidation,
            fee = BigDecimal("7.02"),
            marketFee = BigDecimal("5.00"),
            vat = BigDecimal("300.00"),
        )

        assertEquals("1000.00", buyValidation.tradeAmount.toPlainString())
        assertEquals("1000.00", confirmationValidation.amountWithFee.toPlainString())
        assertTrue(buyValidation.canContinue)
        assertTrue(confirmationValidation.errors.contains(TradeValidationError.InsufficientArsForFee))
        assertFalse(confirmationValidation.canConfirm)
    }

    @Test
    fun `26a buy usd confirmation validation requires an ars fee account`() {
        val state = state(
            instrument = instrument(
                askPrice00 = BigDecimal("100.00"),
                currency = "USD",
            ),
            accountContext = accountContext(
                usdBalance = BigDecimal("1000.00"),
                selectedCurrency = "USD",
                arsBalances = null,
            ),
            inputMode = BuyInputMode.Quantity,
            quantityInputText = "10",
        )

        val buyValidation = validator.validate(state)
        val confirmationValidation = validator.validateConfirmation(
            state = state,
            validation = buyValidation,
            fee = BigDecimal("7.02"),
            marketFee = BigDecimal("5.00"),
            vat = BigDecimal("300.00"),
        )

        assertTrue(buyValidation.canContinue)
        assertTrue(confirmationValidation.errors.contains(TradeValidationError.MissingArsFeeAccount))
        assertFalse(confirmationValidation.canConfirm)
    }

    @Test
    fun `26b buy usd confirmation validation requires fee account selection when several ars accounts exist`() {
        val arsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("999999999.99")))
        val anotherArsAccount = accountWithBalances("ARS", tradingBalances(BigDecimal("999999999.99"))).copy(
            number = "ARS-002",
        )
        val state = state(
            instrument = instrument(
                askPrice00 = BigDecimal("100.00"),
                currency = "USD",
            ),
            accountContext = TradeAccountContext(
                selectedAccount = accountWithBalances("USD", tradingBalances(BigDecimal("1000.00"))),
                availableArsAccounts = listOf(arsAccount, anotherArsAccount),
                selectedFeeAccount = null,
            ),
            inputMode = BuyInputMode.Quantity,
            quantityInputText = "10",
        )

        val buyValidation = validator.validate(state)
        val confirmationValidation = validator.validateConfirmation(
            state = state,
            validation = buyValidation,
            fee = BigDecimal("7.02"),
            marketFee = BigDecimal("5.00"),
            vat = BigDecimal("300.00"),
        )

        assertTrue(buyValidation.canContinue)
        assertTrue(confirmationValidation.errors.contains(TradeValidationError.FeeAccountNotSelected))
        assertFalse(confirmationValidation.canConfirm)
    }

    private fun state(
        tradeType: TradeType = TradeType.Buy,
        orderType: TradeOrderType = TradeOrderType.Market,
        settlementTerm: SettlementType = SettlementType.TODAY,
        inputMode: BuyInputMode = BuyInputMode.Amount,
        instrument: Security = instrument(),
        accountContext: TradeAccountContext = accountContext(),
        limitPriceInput: String = "100",
        amountInputText: String = "",
        quantityInputText: String = "",
    ): TradeViewModelState =
        TradeViewModelState(
            instrument = instrument,
            accountContext = accountContext,
            tradeType = tradeType,
            orderType = orderType,
            settlementTerm = settlementTerm,
            inputMode = inputMode,
            limitPriceInput = limitPriceInput,
            amountInputText = amountInputText,
            quantityInputText = quantityInputText,
        )

    private fun instrument(
        type: String = "Letras",
        liderMerval: Boolean = false,
        minInstrumentNominals: Int = 1,
        maxInstrumentNominals: Int = 999999,
        lotInstrumentSize: Int = 1,
        holdingQuantity: Int = 0,
        askPrice00: BigDecimal = BigDecimal("100.00"),
        bidPrice00: BigDecimal = BigDecimal("99.00"),
        percentageMovement: BigDecimal = BigDecimal("0.10"),
        currency: String = "ARS",
        minBuyArsAmount: BigDecimal = BigDecimal("100.00"),
    ): Security =
        Security(
            id = 1,
            ticker = "TEST",
            currency = currency,
            type = type,
            liderMerval = liderMerval,
            minInstrumentNominals = minInstrumentNominals,
            maxInstrumentNominals = maxInstrumentNominals,
            lotInstrumentSize = lotInstrumentSize,
            holdingQuantity = holdingQuantity,
            askPrice00 = askPrice00,
            askPrice24 = askPrice00,
            bidPrice00 = bidPrice00,
            bidPrice24 = bidPrice00,
            percentageMovement = percentageMovement,
            minBuyArsAmount = minBuyArsAmount,
        )

    private fun accountContext(
        arsBalance: BigDecimal = BigDecimal("999999999.99"),
        usdBalance: BigDecimal = BigDecimal("999999999.99"),
        arsBalances: TradingBalanceSet? = tradingBalances(arsBalance),
        usdBalances: TradingBalanceSet = tradingBalances(usdBalance),
        selectedCurrency: String = "ARS",
    ): TradeAccountContext =
        TradeAccountContext(
            selectedAccount = if (selectedCurrency.normalizedCurrency() == "USD") {
                accountWithBalances("USD", usdBalances)
            } else {
                accountWithBalances("ARS", requireNotNull(arsBalances))
            },
            availableArsAccounts = arsBalances?.let { listOf(accountWithBalances("ARS", it)) }.orEmpty(),
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

    private fun tradingBalances(balance: BigDecimal): TradingBalanceSet =
        TradingBalanceSet(
            limitNow = balance,
            limit24 = balance,
            marketNow = balance,
            market24 = balance,
        )
}
