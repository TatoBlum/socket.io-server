package com.example.socketapp

import androidx.compose.runtime.Immutable

@Immutable
data class CryptoTicker(
    val symbol: String,
    val displayName: String,
    val price: String,
    val previousPrice: String? = null,
    val priceDirection: PriceDirection = PriceDirection.NEUTRAL,
    val percentChange: String = "0.00",
)
