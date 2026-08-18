package com.example.socketapp.ui.tradingview

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketOverviewConfigTest {

    @Test
    fun `Merval market overview matches TradingView builder`() {
        val json = JSONObject(SP_MERVAL_MARKET_OVERVIEW_CONFIG.toJson())
        val sections = json.getJSONArray("symbolSectors")
        val section = sections.getJSONObject(0)

        assertEquals(1, sections.length())
        assertEquals("S&P Merval", section.getString("sectionName"))
        assertEquals(1, section.getJSONArray("symbols").length())
        assertEquals("BCBA:IMV", section.getJSONArray("symbols").getString(0))
        assertEquals("1D", json.getString("timeFrame"))
        assertTrue(json.getBoolean("hideMarketStatus"))
    }

    @Test
    fun `HTML attributes configure Merval before component registration`() {
        val attributes = SP_MERVAL_MARKET_OVERVIEW_CONFIG.toHtmlAttributes()

        assertTrue(attributes.contains("symbol-sectors="))
        assertTrue(attributes.contains("BCBA:IMV"))
        assertTrue(attributes.contains("time-frame=\"1D\""))
        assertTrue(attributes.contains("hide-market-status"))
        assertFalse(attributes.contains("<"))
    }
}
