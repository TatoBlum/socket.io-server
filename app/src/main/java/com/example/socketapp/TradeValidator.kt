package com.example.socketapp

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class TradeValidator @Inject constructor() {
    fun validate(state: BuySecurityUiState): BuyValidationResult {
        val instrument = state.instrument
            ?: return BuyValidationResult(fee = state.accountContext.tradeFee)
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
                fee = context.tradeFee,
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
            minTradeAmount = instrument.minTradeAmount,
            tradeFee = context.tradeFee,
            currency = state.tradeCurrency,
        )

        if (activeInput == null) {
            return BuyValidationResult(
                tradePrice = tradePrice,
                maxNominals = maxNominals,
                fee = context.tradeFee,
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
            return BuyValidationResult(
                tradePrice = tradePrice,
                tradeNominals = tradeNominals,
                tradeAmount = tradeAmount,
                maxNominals = maxNominals,
                fee = context.tradeFee,
                errors = errors,
            )
        }

        errors += validateNominals(
            tradeType = state.tradeType,
            tradeNominals = tradeNominals,
            minNominals = minNominals,
            lotSize = lotSize,
            maxNominals = maxNominals,
            nominalsAvailable = context.availableNominals.toBigDecimal(),
        )

        if (state.tradeType == TradeType.Sell && state.inputMode == BuyInputMode.Amount) {
            errors += validateSellByAmountMax(
                orderType = state.orderType,
                tradeAmount = activeInput,
                nominalsAvailable = context.availableNominals.toBigDecimal(),
                bidPrice = instrument.bidPrice,
                limitPrice = limitPrice,
            )
        }

        errors += validateBalances(
            tradeType = state.tradeType,
            currency = state.tradeCurrency,
            operationMode = state.inputMode,
            tradeAmount = tradeAmount,
            tradeFee = context.tradeFee,
            balanceArs = context.accountBalanceArs,
            balanceUsd = context.accountBalanceUsd,
        )

        val totalTradeAmount = computeTotalTradeAmount(
            currency = state.tradeCurrency,
            tradeAmount = tradeAmount,
            tradeFee = context.tradeFee,
        )
        errors += validateMinMaxTotal(
            inputMode = state.inputMode,
            totalTradeAmount = totalTradeAmount,
            minTradeAmount = instrument.minTradeAmount,
            maxTradeAmount = instrument.maxTradeAmount,
            minimumAmountForOperation = minimumAmountForOperation,
        )

        return BuyValidationResult(
            tradePrice = tradePrice,
            tradeNominals = tradeNominals,
            tradeAmount = tradeAmount,
            totalTradeAmount = totalTradeAmount,
            maxNominals = maxNominals,
            fee = context.tradeFee,
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
            normalizedSubType in setOf("letras") -> BigDecimal("0.001")
            normalizedSubType in setOf("bonos", "obligaciones negociables", "on") ->
                rangedPriceStep(limitPrice,
                    BigDecimal("0.001"),
                    BigDecimal("0.01"),
                    BigDecimal("0.10"),
                    BigDecimal("1"),
                    BigDecimal("10"))

            normalizedSubType in setOf("cedears", "cedear", "etfs", "etf") ->
                rangedPriceStep(
                    limitPrice,
                    BigDecimal("0.01"),
                    BigDecimal("0.05"),
                    BigDecimal("0.10"),
                    BigDecimal("1"),
                    BigDecimal("10")
                )

            normalizedSubType == "acciones" && isLiderMerval ->
                rangedPriceStep(limitPrice,
                    BigDecimal("0.01"),
                    BigDecimal("0.05"),
                    BigDecimal("0.10"),
                    BigDecimal("1"),
                    BigDecimal("10"))

            normalizedSubType == "acciones" -> BigDecimal("0.01")
            else -> BigDecimal("0.01")
        }
    }

    private fun rangedPriceStep(
        limitPrice: BigDecimal,
        upTo50: BigDecimal,
        upTo500: BigDecimal,
        upTo5000: BigDecimal,
        upTo50000: BigDecimal,
        above50000: BigDecimal,
    ): BigDecimal =
        when {
            limitPrice <= BigDecimal("50") -> upTo50
            limitPrice <= BigDecimal("500") -> upTo500
            limitPrice <= BigDecimal("5000") -> upTo5000
            limitPrice <= BigDecimal("50000") -> upTo50000
            else -> above50000
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
        minTradeAmount: BigDecimal,
        tradeFee: BigDecimal,
        currency: String,
    ): BigDecimal {
        if (tradePrice <= BigDecimal.ZERO || lotSize <= BigDecimal.ZERO) {
            return minTradeAmount.setScale(2, RoundingMode.HALF_UP)
        }

        val minimumTradeAmountWithoutFee = if (currency == "ARS") {
            minTradeAmount.subtract(tradeFee).coerceAtLeast(BigDecimal.ZERO)
        } else {
            minTradeAmount
        }
        val minimumNominalsByAmount = minimumTradeAmountWithoutFee
            .divide(tradePrice, 8, RoundingMode.UP)
            .roundUpToMultipleOf(lotSize)
        val minimumNominals = maxOf(minNominals.roundUpToMultipleOf(lotSize), minimumNominalsByAmount)

        return minimumNominals.multiply(tradePrice).toMoneyAmount()
    }

    private fun computeEffectiveMaxNominals(
        state: BuySecurityUiState,
        tradePrice: BigDecimal,
    ): BigDecimal {
        val instrument = state.instrument ?: return BigDecimal.ZERO
        if (tradePrice <= BigDecimal.ZERO) return BigDecimal.ZERO
        val configuredMaxNominals = minOf(
            instrument.maxInstrumentNominals.toBigDecimal(),
            instrument.maxTradeNominals.toBigDecimal(),
        )
        val operationalMaxNominals = when (state.tradeType) {
            TradeType.Sell -> state.accountContext.availableNominals.toBigDecimal()
            TradeType.Buy -> {
                val balance = if (state.tradeCurrency == "USD") {
                    state.accountContext.accountBalanceUsd
                } else {
                    state.accountContext.accountBalanceArs
                }
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
        nominalsAvailable: BigDecimal,
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
        if (tradeNominals > maxNominals) {
            errors += TradeValidationError.NominalsOverMax(maxNominals)
        }
        if (tradeType == TradeType.Sell && tradeNominals > nominalsAvailable) {
            errors += TradeValidationError.NominalsOverAvailable(nominalsAvailable)
        }
        return errors
    }

    private fun validateSellByAmountMax(
        orderType: BuyOrderType,
        tradeAmount: BigDecimal,
        nominalsAvailable: BigDecimal,
        bidPrice: BigDecimal,
        limitPrice: BigDecimal?,
    ): List<TradeValidationError> {
        val price = if (orderType == BuyOrderType.Limit) limitPrice ?: BigDecimal.ZERO else bidPrice
        val maxAmount = nominalsAvailable.multiply(price).setScale(2, RoundingMode.HALF_UP)
        return if (tradeAmount > maxAmount) {
            listOf(TradeValidationError.AmountOverMaxSellable(maxAmount))
        } else {
            emptyList()
        }
    }

    private fun validateBalances(
        tradeType: TradeType,
        currency: String,
        operationMode: BuyInputMode,
        tradeAmount: BigDecimal,
        tradeFee: BigDecimal,
        balanceArs: BigDecimal,
        balanceUsd: BigDecimal,
    ): List<TradeValidationError> {
        val errors = mutableListOf<TradeValidationError>()
        if (tradeType == TradeType.Buy && currency == "ARS" && balanceArs < tradeAmount) {
            errors += TradeValidationError.InsufficientArs(operationMode)
        }
        if (tradeType == TradeType.Buy && currency == "USD") {
            if (balanceArs < tradeFee) errors += TradeValidationError.InsufficientArsForFee
            if (balanceUsd < tradeAmount) errors += TradeValidationError.InsufficientUsd(operationMode)
        }
        if (tradeType == TradeType.Sell && currency == "USD" && balanceArs < tradeFee) {
            errors += TradeValidationError.InsufficientArsForFee
        }
        return errors
    }

    private fun computeTotalTradeAmount(
        currency: String,
        tradeAmount: BigDecimal,
        tradeFee: BigDecimal,
    ): BigDecimal =
        if (currency == "ARS") {
            tradeAmount.add(tradeFee).toMoneyAmount()
        } else {
            tradeAmount.toMoneyAmount()
        }

    private fun validateMinMaxTotal(
        inputMode: BuyInputMode,
        totalTradeAmount: BigDecimal,
        minTradeAmount: BigDecimal,
        maxTradeAmount: BigDecimal,
        minimumAmountForOperation: BigDecimal,
    ): List<TradeValidationError> =
        when {
            totalTradeAmount < minTradeAmount && inputMode == BuyInputMode.Amount ->
                listOf(TradeValidationError.AmountNotEnoughForMin(minimumAmountForOperation))

            totalTradeAmount < minTradeAmount -> listOf(TradeValidationError.TotalBelowMinAmount(minTradeAmount))
            totalTradeAmount > maxTradeAmount -> listOf(TradeValidationError.TotalAboveMaxAmount(maxTradeAmount))
            else -> emptyList()
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
}
