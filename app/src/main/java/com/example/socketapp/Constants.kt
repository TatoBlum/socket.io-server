package com.example.socketapp

object Constants {
    @Deprecated("Use combinedStreamUrl()")
    const val WEB_SOCKET_URL = "wss://stream.binance.com:9443/ws/btceur@ticker"

    val SYMBOLS: List<String> = listOf(
        // Top 100 USDT pairs by 24h volume (Binance, 2026-04-14)
        "btcusdt",    "ethusdt",    "solusdt",    "bnbusdt",    "xrpusdt",
        "dogeusdt",   "pepeusdt",   "linkusdt",   "adausdt",    "avaxusdt",
        "trxusdt",    "suiusdt",    "taousdt",    "enjusdt",    "wldusdt",
        "nearusdt",   "aaveusdt",   "ltcusdt",    "enausdt",    "dotusdt",
        "fetusdt",    "uniusdt",    "penguusdt",  "ldousdt",    "tonusdt",
        "bonkusdt",   "hbarusdt",   "shibusdt",   "ondousdt",   "aptusdt",
        "algousdt",   "renderusdt", "virtualusdt","arbusdt",    "wifusdt",
        "xlmusdt",    "polusdt",    "etcusdt",    "crvusdt",    "zrousdt",
        "opusdt",     "strkusdt",   "flokiusdt",  "ordiusdt",   "icpusdt",
        "eigenusdt",  "arusdt",     "pnutusdt",   "compusdt",   "seiusdt",
        "chzusdt",    "axsusdt",    "galausdt",   "turbousdt",  "sandusdt",
        "jupusdt",    "flowusdt",   "tiausdt",    "atomusdt",   "bomeusdt",
        "dydxusdt",   "injusdt",    "iotausdt",   "vetusdt",    "wusdt",
        "cfxusdt",    "theusdt",    "pythusdt",   "lptusdt",    "scrusdt",
        "ctsiusdt",   "filusdt",    "magicusdt",  "stxusdt",    "ftmusdt",
        "runeusdt",   "ankrusdt",   "bbusdt",     "eosusdt",    "kaiausdt",
        "mantausdt",  "snxusdt",    "grtusdt",    "ksmusdt",    "zrxusdt",
        "notusdt",    "apeusdt",    "mkrusdt",    "woousdt",    "moveusdt",
        "oceanusdt",  "cotiusdt",   "manausdt",   "gmxusdt",    "spellusdt",
        "yfiusdt",    "sklusdt",    "balusdt",    "bandusdt",   "perpusdt",
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
