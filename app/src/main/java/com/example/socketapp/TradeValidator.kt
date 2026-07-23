package com.example.socketapp

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class TradeValidator @Inject constructor() {
    fun validate(state: TradeViewModelState): TradeValidationResult {
        val instrument = state.instrument
            ?: return TradeValidationResult()
        val errors = mutableListOf<TradeValidationError>()

        val limitPrice = TradeInputParser.parseLimitPriceInput(state.limitPriceInput)
        val hasInvalidLimitPrice = limitPrice == null || limitPrice <= BigDecimal.ZERO
        val hasActiveInput = state.activeInput?.let { it > BigDecimal.ZERO } ?: false

        if (state.orderType == TradeOrderType.Limit) {
            when {
                hasInvalidLimitPrice && hasActiveInput -> errors += TradeValidationError.InvalidLimitPrice
                limitPrice != null && !state.isLimitPriceOnly -> {
                    errors += validateLimitPriceBand(
                        tradeType = state.tradeType,
                        limitPrice = limitPrice,
                        askPrice = instrument.askPriceFor(state.settlementTerm),
                        bidPrice = instrument.bidPriceFor(state.settlementTerm),
                        percentageMovement = instrument.percentageMovement,
                    )
                }
            }
            if (limitPrice != null) {
                errors += validateLimitPriceMultiple(
                    instrumentSubType = instrument.type,
                    isLiderMerval = instrument.liderMerval,
                    limitPrice = limitPrice,
                )
            }
        }

        val tradePrice = resolveTradePrice(
            orderType = state.orderType,
            tradeType = state.tradeType,
            limitPrice = limitPrice,
            instrument = instrument,
            settlementTerm = state.settlementTerm,
        )
        if (tradePrice == null) {
            if (errors.isEmpty()) {
                errors += TradeValidationError.MissingTradePrice
            }
            return TradeValidationResult(
                tradePrice = BigDecimal.ZERO,
                errors = errors,
            )
        }

        val activeInput = state.activeInput?.stripTrailingZeros()
        val minNominals = instrument.minInstrumentNominals.toBigDecimal()
        val lotSize = instrument.lotInstrumentSize.toBigDecimal()
        val maxAffordableNominals = computeMaxAffordableNominals(state, tradePrice)
        val maxNominals = when (state.tradeType) {
            TradeType.Buy -> maxAffordableNominals
            TradeType.Sell -> instrument.holdingQuantity.toBigDecimal()
        }
        val minimumAmountForOperation = computeMinimumAmountForOperation(
            minNominals = minNominals,
            lotSize = lotSize,
            tradePrice = tradePrice,
        )
        val minimumTradeAmount = minimumTradeAmount(
            tradeType = state.tradeType,
            instrument = instrument,
        )
        errors += validateSelectedAccountCurrency(state)

        if (activeInput == null) {
            return TradeValidationResult(
                tradePrice = tradePrice,
                maxNominals = maxNominals,
                errors = errors,
            )
        }

        if (
            state.inputMode == BuyInputMode.Amount &&
            activeInput < minimumTradeAmount
        ) {
            errors += TradeValidationError.OperationAmountBelowMin(minimumTradeAmount)
            return TradeValidationResult(
                tradePrice = tradePrice,
                tradeAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                maxNominals = maxNominals,
                errors = errors,
            )
        }

        val (tradeNominals, tradeAmount) = computeTrade(
            inputMode = state.inputMode,
            activeInput = activeInput,
            tradePrice = tradePrice,
            lotSize = lotSize,
        )

        if (state.inputMode == BuyInputMode.Amount && tradeNominals < minNominals.roundUpToMultipleOf(lotSize)) {
            errors += TradeValidationError.AmountNotEnoughForMin(minimumAmountForOperation)
            return TradeValidationResult(
                tradePrice = tradePrice,
                tradeNominals = tradeNominals,
                tradeAmount = tradeAmount,
                maxNominals = maxNominals,
                errors = errors,
            )
        }

        errors += validateNominals(
            inputMode = state.inputMode,
            tradeType = state.tradeType,
            tradeNominals = tradeNominals,
            minNominals = minNominals,
            lotSize = lotSize,
            maxAffordableNominals = maxAffordableNominals,
            holdingQuantity = instrument.holdingQuantity.toBigDecimal(),
        )

        if (state.tradeType == TradeType.Sell && state.inputMode == BuyInputMode.Amount) {
            errors += validateSellByAmountMax(
                orderType = state.orderType,
                tradeAmount = activeInput,
                holdingQuantity = instrument.holdingQuantity.toBigDecimal(),
                bidPrice = instrument.bidPriceFor(state.settlementTerm),
                limitPrice = limitPrice,
            )
        }

        errors += validateBalances(
            tradeType = state.tradeType,
            currency = state.tradeCurrency,
            operationMode = state.inputMode,
            tradeAmount = tradeAmount,
            balance = state.selectedBalanceFor(
                orderType = state.orderType,
                settlementTerm = state.settlementTerm,
            ),
        )

        val maxOperationAmount = maxNominals.multiply(tradePrice).toMoneyAmount()
        val minimumNominalsForTradeAmount = computeMinimumNominalsForAmount(
            minAmount = minimumTradeAmount,
            tradePrice = tradePrice,
            lotSize = lotSize,
        )
        errors += validateOperationAmountLimits(
            tradeType = state.tradeType,
            inputMode = state.inputMode,
            operationAmount = tradeAmount,
            minOperationAmount = minimumTradeAmount,
            minOperationNominals = minimumNominalsForTradeAmount,
            maxOperationAmount = maxOperationAmount,
        )

        return TradeValidationResult(
            tradePrice = tradePrice,
            tradeNominals = tradeNominals,
            tradeAmount = tradeAmount,
            maxNominals = maxNominals,
            errors = errors,
        )
    }

    fun validateConfirmation(
        state: TradeViewModelState,
        validation: TradeValidationResult,
        feeQuote: TradeFeeQuote,
    ): TradeConfirmationState {
        if (!validation.canContinue) return TradeConfirmationState()

        val subTotal = feeQuote.subTotal.toMoneyAmount()
        val taxes = feeQuote.taxes.toMoneyAmount()
        val marketFee = feeQuote.marketFee.toMoneyAmount()
        val operationFee = feeQuote.operationFee.toMoneyAmount()
        val bonusDiscount = feeQuote.bonusDiscount.toMoneyAmount()
        val estimatedAmount = feeQuote.estimatedAmount.toMoneyAmount()
        val totalDeductions = feeQuote.totalDeductions.toMoneyAmount()
        val finalFee = feeQuote.finalFee.toMoneyAmount()
        val errors = validateConfirmationBalances(
            state = state,
            feeQuote = feeQuote.copy(
                subTotal = subTotal,
                taxes = taxes,
                marketFee = marketFee,
                operationFee = operationFee,
                bonusDiscount = bonusDiscount,
                estimatedAmount = estimatedAmount,
                totalDeductions = totalDeductions,
                finalFee = finalFee,
            ),
        )

        return TradeConfirmationState(
            subTotal = subTotal,
            taxes = taxes,
            marketFee = marketFee,
            operationFee = operationFee,
            bonusDiscount = bonusDiscount,
            nominals = feeQuote.nominals,
            estimatedAmount = estimatedAmount,
            totalDeductions = totalDeductions,
            finalFee = finalFee,
            feePercent = feeQuote.feePercent,
            finalFeePercent = feeQuote.finalFeePercent,
            errors = errors,
        )
    }

    private fun resolveTradePrice(
        orderType: TradeOrderType,
        tradeType: TradeType,
        limitPrice: BigDecimal?,
        instrument: Security,
        settlementTerm: SettlementType,
    ): BigDecimal? {
        val price = when (orderType) {
            TradeOrderType.Limit -> limitPrice
            TradeOrderType.Market -> instrument.marketPriceFor(tradeType, settlementTerm)
        }
        return price?.takeIf { it > BigDecimal.ZERO }
    }

    private fun computeTrade(
        inputMode: BuyInputMode,
        activeInput: BigDecimal,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): Pair<BigDecimal, BigDecimal> {
        val tradeNominals = when (inputMode) {
            BuyInputMode.Quantity -> activeInput
            BuyInputMode.Amount -> computeNominalsFromAmount(activeInput, tradePrice, lotSize)
        }
        return tradeNominals to tradeNominals.multiply(tradePrice).toMoneyAmount()
    }

    private fun validateLimitPriceBand(
        tradeType: TradeType,
        limitPrice: BigDecimal,
        askPrice: BigDecimal,
        bidPrice: BigDecimal,
        percentageMovement: BigDecimal,
    ): List<TradeValidationError> {
        val priceBandMovement = percentageMovement.toPriceBandMovement()
        return when (tradeType) {
            TradeType.Buy -> {
                val maxAllowed = askPrice
                    .multiply(BigDecimal.ONE.add(priceBandMovement))
                    .toMoneyAmount()
                if (limitPrice > maxAllowed) {
                    listOf(TradeValidationError.LimitPriceOutOfBandBuy(maxAllowed))
                } else {
                    emptyList()
                }
            }

            TradeType.Sell -> {
                val minAllowed = bidPrice
                    .multiply(BigDecimal.ONE.subtract(priceBandMovement))
                    .toMoneyAmount()
                if (limitPrice < minAllowed) {
                    listOf(TradeValidationError.LimitPriceOutOfBandSell(minAllowed))
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun validateLimitPriceMultiple(
        instrumentSubType: String,
        isLiderMerval: Boolean,
        limitPrice: BigDecimal,
    ): List<TradeValidationError> {
        val priceStep = resolvePriceMultiple(instrumentSubType, isLiderMerval, limitPrice)
        val isMultiple = limitPrice.isMultipleOf(priceStep)
        return if (!isMultiple) {
            listOf(TradeValidationError.LimitPriceNotMultiple(priceStep))
        } else {
            emptyList()
        }
    }

    private fun resolvePriceMultiple(
        instrumentSubType: String,
        isLiderMerval: Boolean,
        limitPrice: BigDecimal,
    ): BigDecimal {
        val normalizedSubType = instrumentSubType.trim().lowercase()
        return when {
            normalizedSubType in TREASURY_BILL_SUBTYPES -> BigDecimal("0.001")
            normalizedSubType in BOND_SUBTYPES -> bondPriceStep(limitPrice)

            normalizedSubType in VARIABLE_INCOME_LEADER_SUBTYPES ->
                variableIncomeLeaderPriceStep(limitPrice)

            normalizedSubType == STOCK_SUBTYPE && isLiderMerval ->
                variableIncomeLeaderPriceStep(limitPrice)

            normalizedSubType == STOCK_SUBTYPE -> stockNonLeaderPriceStep(limitPrice)
            else -> BigDecimal("0.01")
        }
    }

    private fun bondPriceStep(limitPrice: BigDecimal): BigDecimal =
        when {
            limitPrice <= BigDecimal("50") -> BigDecimal("0.001")
            limitPrice <= BigDecimal("100") -> BigDecimal("0.01")
            limitPrice <= BigDecimal("500") -> BigDecimal("0.05")
            limitPrice <= BigDecimal("1000") -> BigDecimal("0.10")
            limitPrice <= BigDecimal("5000") -> BigDecimal("0.50")
            limitPrice <= BigDecimal("10000") -> BigDecimal("1.00")
            limitPrice <= BigDecimal("50000") -> BigDecimal("5.00")
            else -> BigDecimal("10.00")
        }

    private fun variableIncomeLeaderPriceStep(limitPrice: BigDecimal): BigDecimal =
        when {
            limitPrice <= BigDecimal("1") -> BigDecimal("0.001")
            limitPrice <= BigDecimal("5") -> BigDecimal("0.005")
            limitPrice <= BigDecimal("50") -> BigDecimal("0.01")
            limitPrice <= BigDecimal("100") -> BigDecimal("0.10")
            limitPrice <= BigDecimal("500") -> BigDecimal("0.25")
            limitPrice <= BigDecimal("1000") -> BigDecimal("0.50")
            limitPrice <= BigDecimal("2500") -> BigDecimal("1.00")
            limitPrice <= BigDecimal("5000") -> BigDecimal("2.50")
            limitPrice <= BigDecimal("10000") -> BigDecimal("5.00")
            limitPrice <= BigDecimal("25000") -> BigDecimal("10.00")
            limitPrice <= BigDecimal("50000") -> BigDecimal("20.00")
            else -> BigDecimal("25.00")
        }

    private fun stockNonLeaderPriceStep(limitPrice: BigDecimal): BigDecimal =
        when {
            limitPrice <= BigDecimal("1") -> BigDecimal("0.001")
            limitPrice <= BigDecimal("5") -> BigDecimal("0.005")
            limitPrice <= BigDecimal("10") -> BigDecimal("0.01")
            limitPrice <= BigDecimal("25") -> BigDecimal("0.05")
            limitPrice <= BigDecimal("50") -> BigDecimal("0.10")
            limitPrice <= BigDecimal("100") -> BigDecimal("0.20")
            limitPrice <= BigDecimal("400") -> BigDecimal("0.50")
            limitPrice <= BigDecimal("800") -> BigDecimal("1.00")
            limitPrice <= BigDecimal("1000") -> BigDecimal("2.50")
            limitPrice <= BigDecimal("5000") -> BigDecimal("5.00")
            limitPrice <= BigDecimal("10000") -> BigDecimal("10.00")
            limitPrice <= BigDecimal("25000") -> BigDecimal("25.00")
            else -> BigDecimal("50.00")
        }

    private fun computeNominalsFromAmount(
        tradeAmount: BigDecimal,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): BigDecimal {
        if (tradePrice <= BigDecimal.ZERO || lotSize <= BigDecimal.ZERO) return BigDecimal.ZERO
        val rawNominals = tradeAmount.divide(tradePrice, 8, RoundingMode.DOWN)
        return rawNominals.divide(lotSize, 0, RoundingMode.DOWN).multiply(lotSize)
    }

    private fun computeMinimumAmountForOperation(
        minNominals: BigDecimal,
        lotSize: BigDecimal,
        tradePrice: BigDecimal,
    ): BigDecimal {
        if (tradePrice <= BigDecimal.ZERO || lotSize <= BigDecimal.ZERO) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        val minimumNominals = minNominals.roundUpToMultipleOf(lotSize)

        return minimumNominals.multiply(tradePrice).toMoneyAmount()
    }

    private fun computeMaxAffordableNominals(
        state: TradeViewModelState,
        tradePrice: BigDecimal,
    ): BigDecimal {
        val instrument = state.instrument ?: return BigDecimal.ZERO
        if (tradePrice <= BigDecimal.ZERO) return BigDecimal.ZERO
        val balance = state.selectedBalanceFor(
            orderType = state.orderType,
            settlementTerm = state.settlementTerm,
        )
        return computeNominalsFromAmount(balance, tradePrice, instrument.lotInstrumentSize.toBigDecimal())
    }

    private fun computeMinimumNominalsForAmount(
        minAmount: BigDecimal,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): BigDecimal {
        if (minAmount <= BigDecimal.ZERO || tradePrice <= BigDecimal.ZERO || lotSize <= BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }

        val rawNominals = minAmount.divide(tradePrice, 8, RoundingMode.UP)
        return rawNominals.roundUpToMultipleOf(lotSize)
    }

    private fun validateNominals(
        inputMode: BuyInputMode,
        tradeType: TradeType,
        tradeNominals: BigDecimal,
        minNominals: BigDecimal,
        lotSize: BigDecimal,
        maxAffordableNominals: BigDecimal,
        holdingQuantity: BigDecimal,
    ): List<TradeValidationError> {
        val errors = mutableListOf<TradeValidationError>()
        if (tradeNominals <= BigDecimal.ZERO) {
            errors += TradeValidationError.NominalsInvalid
        } else if (tradeNominals < minNominals) {
            errors += TradeValidationError.NominalsBelowMin(minNominals)
        }
        if (lotSize > BigDecimal.ZERO && tradeNominals.remainder(lotSize).compareTo(BigDecimal.ZERO) != 0) {
            errors += TradeValidationError.NominalsNotMultiple(lotSize)
        }
        if (tradeType == TradeType.Sell && tradeNominals > holdingQuantity) {
            errors += TradeValidationError.NotEnoughNominals(holdingQuantity)
        }
        if (tradeType == TradeType.Buy && inputMode == BuyInputMode.Quantity && tradeNominals > maxAffordableNominals) {
            errors += TradeValidationError.NominalsOverAvailableBalance(maxAffordableNominals)
        }
        return errors
    }

    private fun validateConfirmationBalances(
        state: TradeViewModelState,
        feeQuote: TradeFeeQuote,
    ): List<TradeValidationError> {
        val feeBalance = state.feeBalanceFor(
            orderType = state.orderType,
            settlementTerm = state.settlementTerm,
        )
        val selectedTradeBalance = state.selectedBalanceFor(
            orderType = state.orderType,
            settlementTerm = state.settlementTerm,
        )
        val errors = mutableListOf<TradeValidationError>()

        if (
            state.tradeType == TradeType.Buy &&
            state.tradeCurrency == ARS_CURRENCY &&
            selectedTradeBalance < feeQuote.estimatedAmount
        ) {
            errors += TradeValidationError.InsufficientArsForFee
        }

        if (state.tradeCurrency == USD_CURRENCY) {
            when {
                state.availableFeeCommissionAccounts.isEmpty() ->
                    errors += TradeValidationError.MissingArsFeeAccount

                feeBalance == null ->
                    errors += TradeValidationError.FeeAccountNotSelected

                feeBalance < feeQuote.totalDeductions ->
                    errors += TradeValidationError.InsufficientArsForFee
            }

            if (
                state.tradeType == TradeType.Buy &&
                selectedTradeBalance < feeQuote.subTotal
            ) {
                errors += TradeValidationError.InsufficientUsd(state.inputMode)
            }
        }

        return errors
    }

    private fun validateSellByAmountMax(
        orderType: TradeOrderType,
        tradeAmount: BigDecimal,
        holdingQuantity: BigDecimal,
        bidPrice: BigDecimal,
        limitPrice: BigDecimal?,
    ): List<TradeValidationError> {
        val price = if (orderType == TradeOrderType.Limit) limitPrice ?: BigDecimal.ZERO else bidPrice
        val maxAmount = holdingQuantity.multiply(price).setScale(2, RoundingMode.HALF_UP)
        return if (tradeAmount > maxAmount) {
            listOf(TradeValidationError.NotEnoughAvailableAmount(maxAmount))
        } else {
            emptyList()
        }
    }

    private fun validateSelectedAccountCurrency(state: TradeViewModelState): List<TradeValidationError> {
        val selectedCurrency = state.selectedAccount?.currency?.normalizedCurrency()
        return if (selectedCurrency != null && selectedCurrency != state.tradeCurrency) {
            listOf(TradeValidationError.SelectedAccountCurrencyMismatch)
        } else {
            emptyList()
        }
    }

    private fun validateBalances(
        tradeType: TradeType,
        currency: String,
        operationMode: BuyInputMode,
        tradeAmount: BigDecimal,
        balance: BigDecimal,
    ): List<TradeValidationError> {
        val errors = mutableListOf<TradeValidationError>()
        if (tradeType == TradeType.Buy && currency == ARS_CURRENCY && balance < tradeAmount) {
            errors += TradeValidationError.InsufficientArs(operationMode)
        }
        if (tradeType == TradeType.Buy && currency == USD_CURRENCY && balance < tradeAmount) {
            errors += TradeValidationError.InsufficientUsd(operationMode)
        }
        return errors
    }

    private fun minimumTradeAmount(
        tradeType: TradeType,
        instrument: Security,
    ): BigDecimal =
        if (tradeType == TradeType.Buy) {
            instrument.minTradeAmount.toMoneyAmount()
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

    private fun validateOperationAmountLimits(
        tradeType: TradeType,
        inputMode: BuyInputMode,
        operationAmount: BigDecimal,
        minOperationAmount: BigDecimal,
        minOperationNominals: BigDecimal,
        maxOperationAmount: BigDecimal,
    ): List<TradeValidationError> {
        val errors = mutableListOf<TradeValidationError>()
        if (inputMode == BuyInputMode.Quantity && operationAmount < minOperationAmount) {
            errors += TradeValidationError.OperationAmountBelowMin(
                minAmount = minOperationAmount,
                minNominals = minOperationNominals,
            )
        }
        if (tradeType == TradeType.Buy && operationAmount > maxOperationAmount) {
            errors += TradeValidationError.OperationAmountAboveMax(maxOperationAmount)
        }
        return errors
    }

    private fun BigDecimal.roundUpToMultipleOf(step: BigDecimal): BigDecimal {
        if (step <= BigDecimal.ZERO || this <= BigDecimal.ZERO) return BigDecimal.ZERO
        return divide(step, 0, RoundingMode.UP).multiply(step)
    }

    private fun BigDecimal.toMoneyAmount(): BigDecimal =
        setScale(2, RoundingMode.HALF_UP)

    private fun BigDecimal.isMultipleOf(step: BigDecimal): Boolean {
        if (step <= BigDecimal.ZERO) return false
        val scale = maxOf(scale(), step.scale()).coerceAtLeast(0)
        val multiplier = BigDecimal.TEN.pow(scale)
        val scaledValue = multiply(multiplier).setScale(0, RoundingMode.HALF_UP)
        val scaledStep = step.multiply(multiplier).setScale(0, RoundingMode.HALF_UP)
        return scaledValue.remainder(scaledStep).compareTo(BigDecimal.ZERO) == 0
    }

    private companion object {
        const val ARS_CURRENCY = "ARS"
        const val USD_CURRENCY = "USD"
        const val STOCK_SUBTYPE = "acciones"
        val TREASURY_BILL_SUBTYPES = setOf("letras", "letras del tesoro")
        val BOND_SUBTYPES = setOf("bonos", "obligaciones negociables", "on")
        val VARIABLE_INCOME_LEADER_SUBTYPES = setOf("cedears", "cedear", "etfs", "etf")
    }
}

internal fun BigDecimal.toPriceBandMovement(): BigDecimal {
    val positiveMovement = abs()
    return if (positiveMovement > BigDecimal.ONE) {
        positiveMovement.divide(BigDecimal("100"), 8, RoundingMode.HALF_UP)
    } else {
        positiveMovement
    }
}
