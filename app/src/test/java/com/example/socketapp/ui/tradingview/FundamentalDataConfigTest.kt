package com.example.socketapp.ui.tradingview

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class FundamentalDataConfigTest {

    @Test
    fun `config matches the fundamental data builder settings`() {
        val json = JSONObject(FundamentalDataConfig(symbol = "NASDAQ:AAPL").toJson())

        assertEquals("NASDAQ:AAPL", json.getString("symbol"))
        assertEquals("light", json.getString("colorTheme"))
        assertEquals("regular", json.getString("displayMode"))
        assertEquals("es", json.getString("locale"))
        assertEquals("100%", json.getString("width"))
        assertEquals("100%", json.getString("height"))
    }

}
