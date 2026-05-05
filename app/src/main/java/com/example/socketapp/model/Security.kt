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
) {
    val price: BigDecimal
        get() = rawPrice.toBigDecimal()

    val priceChange: BigDecimal
        get() = rawPriceChange.toBigDecimal()

    val percentageChange: BigDecimal
        get() = rawPercentageChange.toBigDecimal()
}
