package com.example.socketapp

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TickerData(
    @Json(name = "s") val symbol: String?,
    @Json(name = "c") val price: String?,
    @Json(name = "P") val percentChange: String?,
)
