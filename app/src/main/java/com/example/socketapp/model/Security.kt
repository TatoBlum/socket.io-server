package com.example.socketapp.model

import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

@Immutable
data class Security(
    val id: String,
    val symbol: String,
    val name: String,
    @SerializedName("price") val rawPrice: String,
    @SerializedName("priceChange") val rawPriceChange: String,
    @SerializedName("percentageChange") val rawPercentageChange: String,
    val currency: String,
    val panel: String,
    val sector: String,
    val isFavourite: Boolean = false,
    @SerializedName("askPrice00") val rawAskPrice00: String? = null,
    @SerializedName("askPrice24") val rawAskPrice24: String? = null,
    @SerializedName("bidPrice00") val rawBidPrice00: String? = null,
    @SerializedName("bidPrice24") val rawBidPrice24: String? = null,
    @SerializedName("minBuyArsAmount") val rawMinBuyArsAmount: String? = null,
) {
    val price: BigDecimal
        get() = rawPrice.toBigDecimal()

    val priceChange: BigDecimal
        get() = rawPriceChange.toBigDecimal()

    val percentageChange: BigDecimal
        get() = rawPercentageChange.toBigDecimal()

    val askPrice00: BigDecimal
        get() = rawAskPrice00.toBigDecimalOrZero()

    val askPrice24: BigDecimal
        get() = rawAskPrice24.toBigDecimalOrZero()

    val bidPrice00: BigDecimal
        get() = rawBidPrice00.toBigDecimalOrZero()

    val bidPrice24: BigDecimal
        get() = rawBidPrice24.toBigDecimalOrZero()

    val minBuyArsAmount: BigDecimal
        get() = rawMinBuyArsAmount.toBigDecimalOrZero()

    private fun String?.toBigDecimalOrZero(): BigDecimal =
        this?.toBigDecimalOrNull() ?: BigDecimal.ZERO
}
