package com.example.socketapp

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class TradeValidator @Inject constructor() {
    fun validate(state: BuySecurityUiState): BuyValidationResult {
        val instrument = state.instrument
            ?: return BuyValidationResult()
        val context = state.accountContext
        val errors = mutableListOf<TradeValidationError>()

        val limitPrice = TradeInputParser.parseLimitPriceInput(state.limitPriceInput)
        if (state.orderType == BuyOrderType.Limit) {
            if (limitPrice == null || limitPrice <= BigDecimal.ZERO) {
                errors += TradeValidationError.InvalidLimitPrice
            } else {
                errors += validateLimitPriceBand(
                    tradeType = state.tradeType,
                    limitPrice = limitPrice,
                    askPrice = instrument.askPrice,
                    bidPrice = instrument.bidPrice,
                    percentageMovement = instrument.percentageMovement,
                )
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
        )
        if (tradePrice == null) {
            if (errors.isEmpty()) {
                errors += TradeValidationError.MissingTradePrice
            }
            return BuyValidationResult(
                tradePrice = BigDecimal.ZERO,
                errors = errors,
            )
        }

        val activeInput = state.activeInput?.stripTrailingZeros()
        val minNominals = instrument.minInstrumentNominals.toBigDecimal()
        val lotSize = instrument.lotInstrumentSize.toBigDecimal()
        val maxNominals = computeEffectiveMaxNominals(state, tradePrice)
        val minimumAmountForOperation = computeMinimumAmountForOperation(
            minNominals = minNominals,
            lotSize = lotSize,
            tradePrice = tradePrice,
        )
        val minimumTotalAmount = minimumChannelAmount(
            tradeType = state.tradeType,
            currency = state.tradeCurrency,
        )
        val effectiveMinimumAmount = maxOf(minimumAmountForOperation, minimumTotalAmount)

        if (activeInput == null) {
            return BuyValidationResult(
                tradePrice = tradePrice,
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
            errors += TradeValidationError.AmountNotEnoughForMin(effectiveMinimumAmount)
            return BuyValidationResult(
                tradePrice = tradePrice,
                tradeNominals = tradeNominals,
                tradeAmount = tradeAmount,
                maxNominals = maxNominals,
                errors = errors,
            )
        }

        errors += validateNominals(
            tradeType = state.tradeType,
            tradeNominals = tradeNominals,
            minNominals = minNominals,
            lotSize = lotSize,
            maxNominals = maxNominals,
            holdingQuantity = instrument.holdingQuantity.toBigDecimal(),
        )

        if (state.tradeType == TradeType.Sell && state.inputMode == BuyInputMode.Amount) {
            errors += validateSellByAmountMax(
                orderType = state.orderType,
                tradeAmount = activeInput,
                holdingQuantity = instrument.holdingQuantity.toBigDecimal(),
                bidPrice = instrument.bidPrice,
                limitPrice = limitPrice,
            )
        }

        errors += validateBalances(
            tradeType = state.tradeType,
            currency = state.tradeCurrency,
            operationMode = state.inputMode,
            tradeAmount = tradeAmount,
            balance = context.balanceFor(
                currency = state.tradeCurrency,
                orderType = state.orderType,
                settlementTerm = state.settlementTerm,
            ),
        )

        val maxOperationAmount = context.balanceFor(
            currency = state.tradeCurrency,
            orderType = state.orderType,
            settlementTerm = state.settlementTerm,
        )
        errors += validateOperationAmountLimits(
            inputMode = state.inputMode,
            operationAmount = tradeAmount,
            minOperationAmount = minimumTotalAmount,
            maxOperationAmount = maxOperationAmount,
            minimumAmountForOperation = effectiveMinimumAmount,
        )

        return BuyValidationResult(
            tradePrice = tradePrice,
            tradeNominals = tradeNominals,
            tradeAmount = tradeAmount,
            maxNominals = maxNominals,
            errors = errors,
        )
    }

    private fun resolveTradePrice(
        orderType: BuyOrderType,
        tradeType: TradeType,
        limitPrice: BigDecimal?,
        instrument: Security,
    ): BigDecimal? {
        val price = when (orderType) {
            BuyOrderType.Limit -> limitPrice
            BuyOrderType.Market -> when (tradeType) {
                TradeType.Buy -> instrument.askPrice
                TradeType.Sell -> instrument.bidPrice
            }
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
    ): List<TradeValidationError> =
        when (tradeType) {
            TradeType.Buy -> {
                val maxAllowed = askPrice.multiply(BigDecimal.ONE.add(percentageMovement))
                if (limitPrice > maxAllowed) {
                    listOf(TradeValidationError.LimitPriceOutOfBandBuy(maxAllowed))
                } else {
                    emptyList()
                }
            }

            TradeType.Sell -> {
                val minAllowed = bidPrice.multiply(BigDecimal.ONE.subtract(percentageMovement))
                if (limitPrice < minAllowed) {
                    listOf(TradeValidationError.LimitPriceOutOfBandSell(minAllowed))
                } else {
                    emptyList()
                }
            }
        }

    private fun validateLimitPriceMultiple(
        instrumentSubType: String,
        isLiderMerval: Boolean,
        limitPrice: BigDecimal,
    ): List<TradeValidationError> {
        val priceStep = resolvePriceMultiple(instrumentSubType, isLiderMerval, limitPrice)
        return if (!limitPrice.isMultipleOf(priceStep)) {
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
            normalizedSubType in setOf("letras", "letras del tesoro") -> BigDecimal("0.001")
            normalizedSubType in setOf("bonos", "obligaciones negociables", "on") ->
                bondPriceStep(limitPrice)

            normalizedSubType in setOf("cedears", "cedear", "etfs", "etf") ->
                variableIncomeLeaderPriceStep(limitPrice)

            normalizedSubType == "acciones" && isLiderMerval ->
                variableIncomeLeaderPriceStep(limitPrice)

            normalizedSubType == "acciones" -> stockNonLeaderPriceStep(limitPrice)
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

    private fun computeEffectiveMaxNominals(
        state: BuySecurityUiState,
        tradePrice: BigDecimal,
    ): BigDecimal {
        val instrument = state.instrument ?: return BigDecimal.ZERO
        if (tradePrice <= BigDecimal.ZERO) return BigDecimal.ZERO
        val configuredMaxNominals = instrument.maxInstrumentNominals.toBigDecimal()
        val operationalMaxNominals = when (state.tradeType) {
            TradeType.Sell -> configuredMaxNominals
            TradeType.Buy -> {
                val balance = state.accountContext.balanceFor(
                    currency = state.tradeCurrency,
                    orderType = state.orderType,
                    settlementTerm = state.settlementTerm,
                )
                computeNominalsFromAmount(balance, tradePrice, instrument.lotInstrumentSize.toBigDecimal())
            }
        }
        return minOf(configuredMaxNominals, operationalMaxNominals)
    }

    private fun validateNominals(
        tradeType: TradeType,
        tradeNominals: BigDecimal,
        minNominals: BigDecimal,
        lotSize: BigDecimal,
        maxNominals: BigDecimal,
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
        if (tradeNominals > maxNominals) {
            errors += TradeValidationError.NominalsOverMax(maxNominals)
        }
        return errors
    }

    private fun validateSellByAmountMax(
        orderType: BuyOrderType,
        tradeAmount: BigDecimal,
        holdingQuantity: BigDecimal,
        bidPrice: BigDecimal,
        limitPrice: BigDecimal?,
    ): List<TradeValidationError> {
        val price = if (orderType == BuyOrderType.Limit) limitPrice ?: BigDecimal.ZERO else bidPrice
        val maxAmount = holdingQuantity.multiply(price).setScale(2, RoundingMode.HALF_UP)
        return if (tradeAmount > maxAmount) {
            listOf(TradeValidationError.NotEnoughAvailableAmount(maxAmount))
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
        if (tradeType == TradeType.Buy && currency == "ARS" && balance < tradeAmount) {
            errors += TradeValidationError.InsufficientArs(operationMode)
        }
        if (tradeType == TradeType.Buy && currency == "USD" && balance < tradeAmount) {
            errors += TradeValidationError.InsufficientUsd(operationMode)
        }
        return errors
    }

    private fun minimumChannelAmount(
        tradeType: TradeType,
        currency: String,
    ): BigDecimal =
        if (tradeType == TradeType.Buy && currency == "ARS") {
            MIN_BUY_ARS_AMOUNT
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

    private fun validateOperationAmountLimits(
        inputMode: BuyInputMode,
        operationAmount: BigDecimal,
        minOperationAmount: BigDecimal,
        maxOperationAmount: BigDecimal,
        minimumAmountForOperation: BigDecimal,
    ): List<TradeValidationError> {
        val errors = mutableListOf<TradeValidationError>()
        if (operationAmount < minOperationAmount && inputMode == BuyInputMode.Amount) {
            errors += TradeValidationError.AmountNotEnoughForMin(minimumAmountForOperation)
        } else if (operationAmount < minOperationAmount) {
            errors += TradeValidationError.OperationAmountBelowMin(minOperationAmount)
        }
        if (operationAmount > maxOperationAmount) {
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
        val MIN_BUY_ARS_AMOUNT: BigDecimal = BigDecimal("100.00")
    }
}
