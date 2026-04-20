package com.example.socketapp.model

import androidx.compose.runtime.Immutable

@Immutable
data class StockTicker(
    val symbol: String,
    val displayName: String,
    val price: String,
    val previousPrice: String? = null,
    val priceDirection: PriceDirection? = null,
    val percentChange: String = "0.00",
)
