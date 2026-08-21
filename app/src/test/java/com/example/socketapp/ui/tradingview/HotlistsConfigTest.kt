package com.example.socketapp.ui.tradingview

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotlistsConfigTest {

    @Test
    fun `BCBA hotlists show daily movers in Spanish without chart`() {
        val json = JSONObject(BCBA_HOTLISTS_CONFIG.toJson())

        assertEquals("BCBA", json.getString("exchange"))
        assertEquals("1D", json.getString("dateRange"))
        assertEquals("es", json.getString("locale"))
        assertFalse(json.getBoolean("showChart"))
        assertTrue(json.getBoolean("showSymbolLogo"))
        assertEquals("100%", json.getString("width"))
        assertEquals("100%", json.getString("height"))
    }
}
