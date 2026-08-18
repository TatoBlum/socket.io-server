package com.example.socketapp.ui.tradingview

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HeatmapConfigTest {

    @Test
    fun `Merval config matches TradingView widget builder`() {
        val json = JSONObject(SP_MERVAL_HEATMAP_CONFIG.toJson())

        assertEquals("BCBAIMV", json.getString("dataSource"))
        assertEquals(0, json.getJSONArray("exchanges").length())
        assertEquals("no_group", json.getString("grouping"))
        assertEquals("market_cap_basic", json.getString("blockSize"))
        assertEquals("change|60", json.getString("blockColor"))
        assertEquals("es", json.getString("locale"))
        assertEquals("", json.getString("symbolUrl"))
        assertEquals("light", json.getString("colorTheme"))
        assertFalse(json.getBoolean("hasTopBar"))
        assertFalse(json.getBoolean("isDataSetEnabled"))
        assertFalse(json.getBoolean("isZoomEnabled"))
        assertFalse(json.getBoolean("hasSymbolTooltip"))
        assertFalse(json.getBoolean("isMonoSize"))
        assertEquals("100%", json.getString("width"))
        assertEquals("100%", json.getString("height"))
    }
}
