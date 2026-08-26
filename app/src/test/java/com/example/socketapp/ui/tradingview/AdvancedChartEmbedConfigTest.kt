package com.example.socketapp.ui.tradingview

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvancedChartEmbedConfigTest {

    @Test
    fun `default config matches advanced chart builder`() {
        val json = JSONObject(AdvancedChartEmbedConfig().toJson())

        assertEquals("NASDAQ:AAPL", json.getString("symbol"))
        assertEquals("1", json.getString("interval"))
        assertEquals("America/Argentina/Buenos_Aires", json.getString("timezone"))
        assertEquals("es", json.getString("locale"))
        assertEquals("light", json.getString("theme"))
        assertTrue(json.getBoolean("allow_symbol_change"))
        assertTrue(json.getBoolean("autosize"))
        assertEquals("100%", json.getString("width"))
        assertEquals("100%", json.getString("height"))
    }
}
