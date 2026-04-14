package com.example.socketapp

object Constants {
    @Deprecated("Use combinedStreamUrl()")
    const val WEB_SOCKET_URL = "wss://stream.binance.com:9443/ws/btceur@ticker"

    val SYMBOLS: List<String> = listOf(
        "btcusdt", "ethusdt", "bnbusdt", "xrpusdt", "adausdt",
        "dogeusdt", "solusdt", "dotusdt", "maticusdt", "ltcusdt",
        "shibusdt", "trxusdt", "avaxusdt", "linkusdt", "uniusdt",
        "atomusdt", "xlmusdt", "etcusdt", "filusdt", "vetusdt",
        "icpusdt", "aptusdt", "nearusdt", "arbusdt",
    )

    fun combinedStreamUrl(): String {
        val streams = SYMBOLS.joinToString("/") { "${it}@ticker" }
        return "wss://stream.binance.com:9443/stream?streams=$streams"
    }

    fun iconUrl(symbol: String): String {
        val coin = symbol.uppercase().removeSuffix("USDT").lowercase()
        return "https://assets.coincap.io/assets/icons/${coin}@2x.png"
    }

    fun displayName(symbol: String): String {
        val upper = symbol.uppercase()
        return if (upper.endsWith("USDT")) {
            "${upper.removeSuffix("USDT")}/USDT"
        } else {
            upper
        }
    }
}
