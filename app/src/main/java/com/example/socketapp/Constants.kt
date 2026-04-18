package com.example.socketapp

object Constants {
    val SYMBOLS = listOf(
        "AAPL", "TSLA", "MSFT", "NVDA", "GOOGL", "AMZN", "META", "AMD", "NFLX", "INTC",
        "DIS", "BA", "JPM", "V", "MA", "KO", "PEP", "WMT", "XOM", "CVX",
        "PYPL", "UBER", "SHOP", "COIN", "SQ"
    )

    const val ALPACA_WS_URL = "wss://stream.data.alpaca.markets/v2/iex"

    fun displayName(symbol: String): String = symbol
}
