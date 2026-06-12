package com.example.socketapp

import java.math.BigDecimal

sealed interface TradeInputHelper {
    data object None : TradeInputHelper
    data class AvailableBalance(val amount: BigDecimal) : TradeInputHelper
    data class AvailableToBuy(val amount: BigDecimal) : TradeInputHelper
    data class AvailableNominalsToBuy(val quantity: BigDecimal) : TradeInputHelper
    data class AvailableNominals(val quantity: Int) : TradeInputHelper
    data class EquivalentAmount(val amount: BigDecimal) : TradeInputHelper
    data class EquivalentNominals(
        val amount: BigDecimal,
        val quantity: BigDecimal,
    ) : TradeInputHelper
    data class ApproximateDebit(val amount: BigDecimal) : TradeInputHelper
    data class ApproximateCredit(val amount: BigDecimal) : TradeInputHelper
}
