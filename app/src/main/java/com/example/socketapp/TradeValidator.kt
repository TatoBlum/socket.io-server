package com.example.socketapp

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class TradeValidator @Inject constructor() {
    fun validate(state: TradeViewModelState): TradeValidationResult {
        val instrument = state.instrument
            ?: return TradeValidationResult()
        val activeInput = state.activeInput?.stripTrailingZeros()
        val limitPrice = TradeInputParser.parseLimitPriceInput(state.limitPriceInput)
        val errors = validateLimitOrder(
            state = state,
            instrument = instrument,
            limitPrice = limitPrice,
            hasActiveInput = activeInput?.let { it > BigDecimal.ZERO } ?: false,
        ).toMutableList()

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

        val lotSize = instrument.lotInstrumentSize.toBigDecimal()
        val holdingQuantity = instrument.holdingQuantity.toBigDecimal()
        val selectedBalance = state.selectedBalanceFor(
            orderType = state.orderType,
            settlementTerm = state.settlementTerm,
        )
        val minimums = computeTradeMinimums(
            tradeType = state.tradeType,
            instrument = instrument,
            tradePrice = tradePrice,
            lotSize = lotSize,
        )
        val maxAffordableNominals = computeNominalsFromAmount(selectedBalance, tradePrice, lotSize)
        val maxNominals = when (state.tradeType) {
            TradeType.Buy -> maxAffordableNominals
            TradeType.Sell -> holdingQuantity
        }
        if (activeInput == null) {
            errors += validateSelectedAccountCurrency(state)
            return TradeValidationResult(
                tradePrice = tradePrice,
                maxNominals = maxNominals,
                errors = errors,
            )
        }

        val trade = computeTrade(
            inputMode = state.inputMode,
            activeInput = activeInput,
            tradePrice = tradePrice,
            lotSize = lotSize,
        )

        validateInputBeforeBalance(
            state = state,
            activeInput = activeInput,
            trade = trade,
            lotSize = lotSize,
            minimums = minimums,
        )?.let { error ->
            errors += error
            return trade.toValidationResult(tradePrice, maxNominals, errors)
        }

        val roundedAmountMinimumError = if (
            state.tradeType == TradeType.Buy &&
            state.inputMode == TradeInputMode.Amount &&
            trade.amount < minimums.configuredAmount
        ) {
            minimums.toOperationAmountBelowMinError()
        } else {
            null
        }

        errors += validateNominals(
            inputMode = state.inputMode,
            tradeType = state.tradeType,
            tradeNominals = trade.nominals,
            minimumNominals = minimums.tradableNominals,
            lotSize = lotSize,
            maxAffordableNominals = maxAffordableNominals,
            holdingQuantity = holdingQuantity,
        )

        if (state.tradeType == TradeType.Sell && state.inputMode == TradeInputMode.Amount) {
            errors += validateSellByAmountMax(
                orderType = state.orderType,
                tradeAmount = activeInput,
                holdingQuantity = holdingQuantity,
                bidPrice = instrument.bidPriceFor(state.settlementTerm),
                limitPrice = limitPrice,
            )
        }

        val maxOperationAmount = maxNominals.multiply(tradePrice).toMoneyAmount()
        errors += validateOperationAmountMaximum(
            tradeType = state.tradeType,
            operationAmount = trade.amount,
            maxOperationAmount = maxOperationAmount,
        )

        roundedAmountMinimumError?.let(errors::add)
        errors += validateSelectedAccountCurrency(state)

        errors += validateBalances(
            tradeType = state.tradeType,
            currency = state.tradeCurrency,
            operationMode = state.inputMode,
            tradeAmount = trade.amount,
            balance = selectedBalance,
        )

        return trade.toValidationResult(tradePrice, maxNominals, errors)
    }

    fun validateConfirmation(
        state: TradeViewModelState,
        validation: TradeValidationResult,
        feeQuote: SimulateFeeResponse,
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

    private fun validateLimitOrder(
        state: TradeViewModelState,
        instrument: Security,
        limitPrice: BigDecimal?,
        hasActiveInput: Boolean,
    ): List<TradeValidationError> {
        if (state.orderType != TradeOrderType.Limit) return emptyList()

        val priceError = when {
            (limitPrice == null || limitPrice <= BigDecimal.ZERO) && hasActiveInput ->
                TradeValidationError.InvalidLimitPrice

            limitPrice != null && !state.isLimitPriceOnly ->
                validateLimitPriceBand(
                    tradeType = state.tradeType,
                    limitPrice = limitPrice,
                    askPrice = instrument.askPriceFor(state.settlementTerm),
                    bidPrice = instrument.bidPriceFor(state.settlementTerm),
                    percentageMovement = instrument.percentageMovement,
                )

            else -> null
        }

        val multipleError = limitPrice?.let { price ->
            validateLimitPriceMultiple(
                instrumentSubType = instrument.type,
                isLiderMerval = instrument.liderMerval,
                limitPrice = price,
            )
        }

        return listOfNotNull(priceError, multipleError)
    }

    private fun computeTradeMinimums(
        tradeType: TradeType,
        instrument: Security,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): TradeMinimums {
        val tradableNominals = instrument.minInstrumentNominals
            .toBigDecimal()
            .roundUpToMultipleOf(lotSize)
        val configuredAmount = minimumTradeAmount(tradeType, instrument)
        val amountBasedNominals = computeMinimumNominalsForAmount(
            minAmount = configuredAmount,
            tradePrice = tradePrice,
            lotSize = lotSize,
        )
        val requiredNominals = maxOf(tradableNominals, amountBasedNominals)

        return TradeMinimums(
            tradableNominals = tradableNominals,
            instrumentAmount = tradableNominals.multiply(tradePrice).toMoneyAmount(),
            configuredAmount = configuredAmount,
            requiredNominals = requiredNominals,
            effectiveAmount = requiredNominals.multiply(tradePrice).toMoneyAmount(),
        )
    }

    private fun validateInputBeforeBalance(
        state: TradeViewModelState,
        activeInput: BigDecimal,
        trade: CalculatedTrade,
        lotSize: BigDecimal,
        minimums: TradeMinimums,
    ): TradeValidationError? =
        when (state.inputMode) {
            TradeInputMode.Amount -> validateAmountBeforeBalance(
                activeInput = activeInput,
                trade = trade,
                minimums = minimums,
            )

            TradeInputMode.Quantity -> validateBuyQuantityBeforeBalance(
                tradeType = state.tradeType,
                trade = trade,
                lotSize = lotSize,
                minimums = minimums,
            )
        }

    private fun validateAmountBeforeBalance(
        activeInput: BigDecimal,
        trade: CalculatedTrade,
        minimums: TradeMinimums,
    ): TradeValidationError? =
        validateMinimumAmount(activeInput, minimums)
            ?: if (trade.nominals < minimums.tradableNominals) {
                TradeValidationError.AmountNotEnoughForMin(minimums.instrumentAmount)
            } else {
                null
            }

    private fun validateBuyQuantityBeforeBalance(
        tradeType: TradeType,
        trade: CalculatedTrade,
        lotSize: BigDecimal,
        minimums: TradeMinimums,
    ): TradeValidationError? {
        if (tradeType != TradeType.Buy) return null

        return validateNominalRequirements(
            tradeNominals = trade.nominals,
            minimumNominals = minimums.tradableNominals,
            lotSize = lotSize,
        ) ?: validateMinimumAmount(trade.amount, minimums)
    }

    private fun validateMinimumAmount(
        amount: BigDecimal,
        minimums: TradeMinimums,
    ): TradeValidationError.OperationAmountBelowMin? =
        if (amount < minimums.configuredAmount) {
            minimums.toOperationAmountBelowMinError()
        } else {
            null
        }

    private fun TradeMinimums.toOperationAmountBelowMinError(): TradeValidationError.OperationAmountBelowMin =
        TradeValidationError.OperationAmountBelowMin(
            minAmount = effectiveAmount,
            minNominals = requiredNominals,
        )

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
        inputMode: TradeInputMode,
        activeInput: BigDecimal,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): CalculatedTrade {
        val tradeNominals = when (inputMode) {
            TradeInputMode.Quantity -> activeInput
            TradeInputMode.Amount -> computeNominalsFromAmount(activeInput, tradePrice, lotSize)
        }
        return CalculatedTrade(
            nominals = tradeNominals,
            amount = tradeNominals.multiply(tradePrice).toMoneyAmount(),
        )
    }

    private fun CalculatedTrade.toValidationResult(
        tradePrice: BigDecimal,
        maxNominals: BigDecimal,
        errors: List<TradeValidationError>,
    ): TradeValidationResult =
        TradeValidationResult(
            tradePrice = tradePrice,
            tradeNominals = nominals,
            tradeAmount = amount,
            maxNominals = maxNominals,
            errors = errors,
        )

    private fun validateLimitPriceBand(
        tradeType: TradeType,
        limitPrice: BigDecimal,
        askPrice: BigDecimal,
        bidPrice: BigDecimal,
        percentageMovement: BigDecimal,
    ): TradeValidationError? {
        val priceBandMovement = percentageMovement.toPriceBandMovement()
        return when (tradeType) {
            TradeType.Buy -> {
                val maxAllowed = askPrice
                    .multiply(BigDecimal.ONE.add(priceBandMovement))
                    .toMoneyAmount()
                if (limitPrice > maxAllowed) {
                    TradeValidationError.LimitPriceOutOfBandBuy(maxAllowed)
                } else {
                    null
                }
            }

            TradeType.Sell -> {
                val minAllowed = bidPrice
                    .multiply(BigDecimal.ONE.subtract(priceBandMovement))
                    .toMoneyAmount()
                if (limitPrice < minAllowed) {
                    TradeValidationError.LimitPriceOutOfBandSell(minAllowed)
                } else {
                    null
                }
            }
        }
    }

    private fun validateLimitPriceMultiple(
        instrumentSubType: String,
        isLiderMerval: Boolean,
        limitPrice: BigDecimal,
    ): TradeValidationError? {
        val priceStep = resolvePriceMultiple(instrumentSubType, isLiderMerval, limitPrice)
        return if (limitPrice.isMultipleOf(priceStep)) {
            null
        } else {
            TradeValidationError.LimitPriceNotMultiple(priceStep)
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

    private fun computeMinimumNominalsForAmount(
        minAmount: BigDecimal,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): BigDecimal {
        if (minAmount <= BigDecimal.ZERO || tradePrice <= BigDecimal.ZERO || lotSize <= BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }

        val amountPerLot = tradePrice.multiply(lotSize)
        val completeLots = minAmount.divide(amountPerLot, 0, RoundingMode.DOWN)
        val candidate = completeLots.multiply(lotSize)

        return if (candidate.multiply(tradePrice).toMoneyAmount() >= minAmount) {
            candidate
        } else {
            candidate.add(lotSize)
        }
    }

    private fun validateNominals(
        inputMode: TradeInputMode,
        tradeType: TradeType,
        tradeNominals: BigDecimal,
        minimumNominals: BigDecimal,
        lotSize: BigDecimal,
        maxAffordableNominals: BigDecimal,
        holdingQuantity: BigDecimal,
    ): List<TradeValidationError> {
        val requirementsError = validateNominalRequirements(
            tradeNominals = tradeNominals,
            minimumNominals = minimumNominals,
            lotSize = lotSize,
        )
        val availabilityError = validateNominalAvailability(
            inputMode = inputMode,
            tradeType = tradeType,
            tradeNominals = tradeNominals,
            maxAffordableNominals = maxAffordableNominals,
            holdingQuantity = holdingQuantity,
        )

        return listOfNotNull(requirementsError, availabilityError)
    }

    private fun validateNominalRequirements(
        tradeNominals: BigDecimal,
        minimumNominals: BigDecimal,
        lotSize: BigDecimal,
    ): TradeValidationError? =
        when {
            tradeNominals <= BigDecimal.ZERO -> TradeValidationError.NominalsInvalid
            tradeNominals < minimumNominals ->
                TradeValidationError.NominalsBelowMin(minimumNominals)
            lotSize > BigDecimal.ZERO &&
                tradeNominals.remainder(lotSize).compareTo(BigDecimal.ZERO) != 0 ->
                TradeValidationError.NominalsNotMultiple(lotSize)
            else -> null
        }

    private fun validateNominalAvailability(
        inputMode: TradeInputMode,
        tradeType: TradeType,
        tradeNominals: BigDecimal,
        maxAffordableNominals: BigDecimal,
        holdingQuantity: BigDecimal,
    ): TradeValidationError? =
        when (tradeType) {
            TradeType.Sell ->
                if (tradeNominals > holdingQuantity) {
                    TradeValidationError.NotEnoughNominals(holdingQuantity)
                } else {
                    null
                }

            TradeType.Buy ->
                if (inputMode == TradeInputMode.Quantity && tradeNominals > maxAffordableNominals) {
                    TradeValidationError.NominalsOverAvailableBalance(maxAffordableNominals)
                } else {
                    null
                }
        }

    private fun validateConfirmationBalances(
        state: TradeViewModelState,
        feeQuote: SimulateFeeResponse,
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
        operationMode: TradeInputMode,
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

    private fun validateOperationAmountMaximum(
        tradeType: TradeType,
        operationAmount: BigDecimal,
        maxOperationAmount: BigDecimal,
    ): List<TradeValidationError> =
        if (tradeType == TradeType.Buy && operationAmount > maxOperationAmount) {
            listOf(TradeValidationError.OperationAmountAboveMax(maxOperationAmount))
        } else {
            emptyList()
        }

    private data class CalculatedTrade(
        val nominals: BigDecimal,
        val amount: BigDecimal,
    )

    private data class TradeMinimums(
        val tradableNominals: BigDecimal,
        val instrumentAmount: BigDecimal,
        val configuredAmount: BigDecimal,
        val requiredNominals: BigDecimal,
        val effectiveAmount: BigDecimal,
    )

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
