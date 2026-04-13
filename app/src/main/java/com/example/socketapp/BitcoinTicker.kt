package com.example.socketapp

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BitcoinTicker(@Json(name = "c") val price: String?)
