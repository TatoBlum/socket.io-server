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
        val limitErrors = validateLimitOrder(
            state = state,
            instrument = instrument,
            limitPrice = limitPrice,
            hasActiveInput = activeInput?.let { it > BigDecimal.ZERO } ?: false,
        )

        val tradePrice = resolveTradePrice(
            orderType = state.orderType,
            tradeType = state.tradeType,
            limitPrice = limitPrice,
            instrument = instrument,
            settlementTerm = state.settlementTerm,
        )
        if (tradePrice == null) {
            return TradeValidationResult(
                tradePrice = BigDecimal.ZERO,
                errors = limitErrors.ifEmpty { listOf(TradeValidationError.MissingTradePrice) },
            )
        }

        val lotSize = instrument.lotInstrumentSize.toBigDecimal()
        val holdingNominals = instrument.holdingQuantity.toBigDecimal()
        val selectedBalance = state.selectedBalanceFor(
            orderType = state.orderType,
            settlementTerm = state.settlementTerm,
        )
        val minimums = calculateTradeMinimumRequirements(
            tradeType = state.tradeType,
            instrument = instrument,
            tradePrice = tradePrice,
            lotSize = lotSize,
        )
        val maxAffordableNominals = calculateTradableNominals(selectedBalance, tradePrice, lotSize)
        val maxNominals = when (state.tradeType) {
            TradeType.Buy -> maxAffordableNominals
            TradeType.Sell -> holdingNominals
        }
        if (activeInput == null) {
            return TradeValidationResult(
                tradePrice = tradePrice,
                maxNominals = maxNominals,
                errors = limitErrors + listOfNotNull(validateSelectedAccountCurrency(state)),
            )
        }

        val trade = calculateTrade(
            inputMode = state.inputMode,
            activeInput = activeInput,
            tradePrice = tradePrice,
            lotSize = lotSize,
        )
        val context = TradeValidationContext(
            state = state,
            instrument = instrument,
            activeInput = activeInput,
            limitPrice = limitPrice,
            tradePrice = tradePrice,
            lotSize = lotSize,
            holdingNominals = holdingNominals,
            selectedBalance = selectedBalance,
            minimums = minimums,
            maxAffordableNominals = maxAffordableNominals,
            maxNominals = maxNominals,
            trade = trade,
        )
        val errors = limitErrors + collectTradeErrors(context)

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

    private fun calculateTradeMinimumRequirements(
        tradeType: TradeType,
        instrument: Security,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): TradeMinimumRequirements {
        val tradableNominals = instrument.minInstrumentNominals
            .toBigDecimal()
            .roundUpToMultipleOf(lotSize)
        val configuredAmount = configuredMinimumTradeAmount(tradeType, instrument)
        val amountBasedNominals = calculateMinimumNominalsForAmount(
            minAmount = configuredAmount,
            tradePrice = tradePrice,
            lotSize = lotSize,
        )
        val requiredNominals = maxOf(tradableNominals, amountBasedNominals)

        return TradeMinimumRequirements(
            tradableNominals = tradableNominals,
            instrumentAmount = tradableNominals.multiply(tradePrice).toMoneyAmount(),
            configuredAmount = configuredAmount,
            requiredNominals = requiredNominals,
            effectiveAmount = requiredNominals.multiply(tradePrice).toMoneyAmount(),
        )
    }

    private fun collectTradeErrors(context: TradeValidationContext): List<TradeValidationError> {
        val primaryInputError = findPrimaryInputError(context)
        val accountCurrencyError = validateSelectedAccountCurrency(context.state)

        if (primaryInputError != null && !shouldContinueAfterPrimaryInputError(context)) {
            return listOfNotNull(primaryInputError, accountCurrencyError)
        }

        return listOfNotNull(
            primaryInputError,
            validateNominalAvailability(context),
            validateSellAmountAvailability(context),
            validateMaximumAffordableAmount(context),
            validateMinimumAmountAfterLotRounding(context),
            accountCurrencyError,
            validateAvailableTradeBalance(context),
        )
    }

    private fun findPrimaryInputError(context: TradeValidationContext): TradeValidationError? =
        when (context.state.inputMode) {
            TradeInputMode.Amount -> firstErrorInOrder(
                { validateEnteredAmountMinimum(context) },
                { validateAmountCoversMinimumNominals(context) },
            )

            TradeInputMode.Quantity -> firstErrorInOrder(
                { validatePositiveNominals(context) },
                { validateMinimumNominals(context) },
                { validateLotMultiple(context) },
                { validateQuantityMinimumAmount(context) },
            )
        }

    private fun firstErrorInOrder(
        vararg rules: () -> TradeValidationError?,
    ): TradeValidationError? = rules.firstNotNullOfOrNull { rule -> rule() }

    private fun validateEnteredAmountMinimum(
        context: TradeValidationContext,
    ): TradeValidationError.OperationAmountBelowMin? =
        minimumAmountError(context.activeInput, context.minimums)

    private fun validateAmountCoversMinimumNominals(
        context: TradeValidationContext,
    ): TradeValidationError.AmountNotEnoughForMin? =
        if (context.trade.nominals < context.minimums.tradableNominals) {
            TradeValidationError.AmountNotEnoughForMin(context.minimums.instrumentAmount)
        } else {
            null
        }

    private fun validatePositiveNominals(
        context: TradeValidationContext,
    ): TradeValidationError? =
        TradeValidationError.NominalsInvalid.takeIf {
            context.trade.nominals <= BigDecimal.ZERO
        }

    private fun validateMinimumNominals(
        context: TradeValidationContext,
    ): TradeValidationError.NominalsBelowMin? =
        if (context.trade.nominals < context.minimums.tradableNominals) {
            TradeValidationError.NominalsBelowMin(context.minimums.tradableNominals)
        } else {
            null
        }

    private fun validateLotMultiple(
        context: TradeValidationContext,
    ): TradeValidationError.NominalsNotMultiple? =
        if (
            context.lotSize > BigDecimal.ZERO &&
            context.trade.nominals.remainder(context.lotSize).compareTo(BigDecimal.ZERO) != 0
        ) {
            TradeValidationError.NominalsNotMultiple(context.lotSize)
        } else {
            null
        }

    private fun validateQuantityMinimumAmount(
        context: TradeValidationContext,
    ): TradeValidationError.OperationAmountBelowMin? =
        if (context.state.tradeType == TradeType.Buy) {
            minimumAmountError(context.trade.amount, context.minimums)
        } else {
            null
        }

    private fun minimumAmountError(
        amount: BigDecimal,
        minimums: TradeMinimumRequirements,
    ): TradeValidationError.OperationAmountBelowMin? =
        if (amount < minimums.configuredAmount) {
            minimums.toOperationAmountBelowMinError()
        } else {
            null
        }

    private fun TradeMinimumRequirements.toOperationAmountBelowMinError():
        TradeValidationError.OperationAmountBelowMin =
            TradeValidationError.OperationAmountBelowMin(
                minAmount = effectiveAmount,
                minNominals = requiredNominals,
            )

    private fun shouldContinueAfterPrimaryInputError(
        context: TradeValidationContext,
    ): Boolean =
        context.state.tradeType == TradeType.Sell &&
            context.state.inputMode == TradeInputMode.Quantity

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

    private fun calculateTrade(
        inputMode: TradeInputMode,
        activeInput: BigDecimal,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): CalculatedTrade {
        val tradeNominals = when (inputMode) {
            TradeInputMode.Quantity -> activeInput
            TradeInputMode.Amount -> calculateTradableNominals(activeInput, tradePrice, lotSize)
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

    private fun calculateTradableNominals(
        tradeAmount: BigDecimal,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): BigDecimal {
        if (tradePrice <= BigDecimal.ZERO || lotSize <= BigDecimal.ZERO) return BigDecimal.ZERO
        val rawNominals = tradeAmount.divide(tradePrice, 8, RoundingMode.DOWN)
        return rawNominals.divide(lotSize, 0, RoundingMode.DOWN).multiply(lotSize)
    }

    private fun calculateMinimumNominalsForAmount(
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

    private fun validateNominalAvailability(
        context: TradeValidationContext,
    ): TradeValidationError? =
        when (context.state.tradeType) {
            TradeType.Sell ->
                if (context.trade.nominals > context.holdingNominals) {
                    TradeValidationError.NotEnoughNominals(context.holdingNominals)
                } else {
                    null
                }

            TradeType.Buy ->
                if (
                    context.state.inputMode == TradeInputMode.Quantity &&
                    context.trade.nominals > context.maxAffordableNominals
                ) {
                    TradeValidationError.NominalsOverAvailableBalance(context.maxAffordableNominals)
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

    private fun validateSellAmountAvailability(
        context: TradeValidationContext,
    ): TradeValidationError.NotEnoughAvailableAmount? {
        if (
            context.state.tradeType != TradeType.Sell ||
            context.state.inputMode != TradeInputMode.Amount
        ) {
            return null
        }

        val price = if (context.state.orderType == TradeOrderType.Limit) {
            context.limitPrice ?: BigDecimal.ZERO
        } else {
            context.instrument.bidPriceFor(context.state.settlementTerm)
        }
        val maxAmount = context.holdingNominals.multiply(price).toMoneyAmount()
        return if (context.activeInput > maxAmount) {
            TradeValidationError.NotEnoughAvailableAmount(maxAmount)
        } else {
            null
        }
    }

    private fun validateMinimumAmountAfterLotRounding(
        context: TradeValidationContext,
    ): TradeValidationError.OperationAmountBelowMin? =
        if (
            context.state.tradeType == TradeType.Buy &&
            context.state.inputMode == TradeInputMode.Amount
        ) {
            minimumAmountError(context.trade.amount, context.minimums)
        } else {
            null
        }

    private fun validateSelectedAccountCurrency(
        state: TradeViewModelState,
    ): TradeValidationError? {
        val selectedCurrency = state.selectedAccount?.currency?.normalizedCurrency()
        return if (selectedCurrency != null && selectedCurrency != state.tradeCurrency) {
            TradeValidationError.SelectedAccountCurrencyMismatch
        } else {
            null
        }
    }

    private fun validateAvailableTradeBalance(
        context: TradeValidationContext,
    ): TradeValidationError? =
        when {
            context.state.tradeType != TradeType.Buy -> null
            context.selectedBalance >= context.trade.amount -> null
            context.state.tradeCurrency == ARS_CURRENCY ->
                TradeValidationError.InsufficientArs(context.state.inputMode)
            context.state.tradeCurrency == USD_CURRENCY ->
                TradeValidationError.InsufficientUsd(context.state.inputMode)
            else -> null
        }

    private fun configuredMinimumTradeAmount(
        tradeType: TradeType,
        instrument: Security,
    ): BigDecimal =
        if (tradeType == TradeType.Buy) {
            instrument.minTradeAmount.toMoneyAmount()
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

    private fun validateMaximumAffordableAmount(
        context: TradeValidationContext,
    ): TradeValidationError.OperationAmountAboveMax? {
        if (context.state.tradeType != TradeType.Buy) return null

        val maxOperationAmount = context.maxNominals
            .multiply(context.tradePrice)
            .toMoneyAmount()
        return if (context.trade.amount > maxOperationAmount) {
            TradeValidationError.OperationAmountAboveMax(maxOperationAmount)
        } else {
            null
        }
    }

    private data class TradeValidationContext(
        val state: TradeViewModelState,
        val instrument: Security,
        val activeInput: BigDecimal,
        val limitPrice: BigDecimal?,
        val tradePrice: BigDecimal,
        val lotSize: BigDecimal,
        val holdingNominals: BigDecimal,
        val selectedBalance: BigDecimal,
        val minimums: TradeMinimumRequirements,
        val maxAffordableNominals: BigDecimal,
        val maxNominals: BigDecimal,
        val trade: CalculatedTrade,
    )

    private data class CalculatedTrade(
        val nominals: BigDecimal,
        val amount: BigDecimal,
    )

    private data class TradeMinimumRequirements(
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
