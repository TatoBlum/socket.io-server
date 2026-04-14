package com.example.socketapp

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CombinedStreamMessage(
    val stream: String?,
    val data: TickerData?,
)
