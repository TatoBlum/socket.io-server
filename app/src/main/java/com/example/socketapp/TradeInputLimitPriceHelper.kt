package com.example.socketapp

import java.math.BigDecimal

sealed interface TradeInputLimitPriceHelper {
    data object None : TradeInputLimitPriceHelper
    data class MaxAllowed(
        val amount: BigDecimal, val currencySymbol: String,
    ) : TradeInputLimitPriceHelper

    data class MinAllowed(
        val amount: BigDecimal, val currencySymbol: String,
    ) : TradeInputLimitPriceHelper
}
