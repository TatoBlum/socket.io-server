package com.example.socketapp.ui.tradingview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TechnicalAnalysisConfigTest {

    @Test
    fun `attributes match the technical analysis builder settings`() {
        val attributes = TechnicalAnalysisConfig(symbol = "NASDAQ:AAPL").toHtmlAttributes()

        assertTrue(attributes.contains("symbol=\"NASDAQ:AAPL\""))
        assertTrue(attributes.contains("interval=\"1D\""))
        assertTrue(attributes.contains("ratings-mode=\"single\""))
        assertTrue(attributes.contains("ratings-display-mode=\"gauge\""))
    }

    @Test
    fun `symbol is escaped before insertion into HTML`() {
        val attributes = TechnicalAnalysisConfig(symbol = "<unsafe>").toHtmlAttributes()

        assertFalse(attributes.contains("<unsafe>"))
        assertTrue(attributes.contains("&lt;unsafe&gt;"))
    }
}
