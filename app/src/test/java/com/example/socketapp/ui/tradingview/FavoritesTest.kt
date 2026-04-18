package com.example.socketapp.ui.tradingview

import com.example.socketapp.model.StockTicker
import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesTest {

    private fun ticker(symbol: String) = StockTicker(
        symbol = symbol,
        displayName = symbol,
        price = "100.00",
    )

    @Test
    fun top5Favorites_withEmptyMap_returnsEmptyList() {
        val result = top5Favorites(emptyMap())
        assertEquals(emptyList<StockTicker>(), result)
    }

    @Test
    fun top5Favorites_respectsConstantsSymbolsOrder() {
        val map = mapOf(
            "AAPL" to ticker("AAPL"),
            "TSLA" to ticker("TSLA"),
            "MSFT" to ticker("MSFT"),
            "NVDA" to ticker("NVDA"),
            "GOOGL" to ticker("GOOGL"),
            "META" to ticker("META"),
            "AMD" to ticker("AMD"),
        )
        val result = top5Favorites(map)
        assertEquals(listOf("AAPL", "TSLA", "MSFT", "NVDA", "GOOGL"), result.map { it.symbol })
    }

    @Test
    fun top5Favorites_whenSomeTop5Missing_returnsOnlyPresent() {
        val map = mapOf(
            "AAPL" to ticker("AAPL"),
            "MSFT" to ticker("MSFT"),
        )
        val result = top5Favorites(map)
        assertEquals(listOf("AAPL", "MSFT"), result.map { it.symbol })
    }

    @Test
    fun top5Favorites_ignoresSymbolsOutsideTop5() {
        // META=index 6, AMD=index 7, NFLX=index 8 in Constants.SYMBOLS
        val map = mapOf(
            "META" to ticker("META"),
            "AMD" to ticker("AMD"),
            "NFLX" to ticker("NFLX"),
        )
        val result = top5Favorites(map)
        assertEquals(emptyList<StockTicker>(), result)
    }
}
