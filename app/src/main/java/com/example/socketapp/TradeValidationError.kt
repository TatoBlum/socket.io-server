package com.example.socketapp

import java.math.BigDecimal

sealed interface TradeValidationError {
    data object InvalidLimitPrice : TradeValidationError
    data class LimitPriceOutOfBandBuy(val maxAllowed: BigDecimal) : TradeValidationError
    data class LimitPriceOutOfBandSell(val minAllowed: BigDecimal) : TradeValidationError
    data class LimitPriceNotMultiple(val step: BigDecimal) : TradeValidationError
    data object MissingTradePrice : TradeValidationError
    data object AmountNotEnoughForMin : TradeValidationError
    data object NominalsInvalid : TradeValidationError
    data class NominalsBelowMin(val minNominals: BigDecimal) : TradeValidationError
    data class NominalsNotMultiple(val lotSize: BigDecimal) : TradeValidationError
    data class NominalsOverMax(val maxNominals: BigDecimal) : TradeValidationError
    data class NominalsOverAvailable(val availableNominals: BigDecimal) : TradeValidationError
    data class AmountOverMaxSellable(val maxAmount: BigDecimal) : TradeValidationError
    data class InsufficientArs(val operationMode: BuyInputMode) : TradeValidationError
    data object InsufficientArsForFee : TradeValidationError
    data class InsufficientUsd(val operationMode: BuyInputMode) : TradeValidationError
    data class TotalBelowMinAmount(val minAmount: BigDecimal) : TradeValidationError
    data class TotalAboveMaxAmount(val maxAmount: BigDecimal) : TradeValidationError
}

fun TradeValidationError.isLimitPriceError(): Boolean =
    when (this) {
        TradeValidationError.InvalidLimitPrice,
        is TradeValidationError.LimitPriceOutOfBandBuy,
        is TradeValidationError.LimitPriceOutOfBandSell,
        is TradeValidationError.LimitPriceNotMultiple,
        -> true

        else -> false
    }
