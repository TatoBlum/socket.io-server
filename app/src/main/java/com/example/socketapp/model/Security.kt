package com.example.socketapp.model

data class Security(
    val symbol: String,
    val name: String,
    val price: String,
    val priceChange: String,
    val percentageChange : String,
    val currency: String,
    val market: String,
    val imageUrl: String,
    val previousPrice: String? = null,
)
