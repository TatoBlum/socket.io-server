package com.example.socketapp.ui.tradingview

import org.junit.Assert.assertTrue
import org.junit.Test

class TradingViewReadinessTest {

    @Test
    fun `technical analysis waits for visible shadow dom content`() {
        assertTrue(TECHNICAL_ANALYSIS_READY_CHECK.contains("__tradingViewShadowRoots"))
        assertTrue(TECHNICAL_ANALYSIS_READY_CHECK.contains("getBoundingClientRect"))
        assertTrue(TECHNICAL_ANALYSIS_READY_CHECK.contains("text.length === 0"))
        assertTrue(TECHNICAL_ANALYSIS_READY_CHECK.contains("visibleTextElementCount +="))
        assertTrue(TECHNICAL_ANALYSIS_READY_CHECK.contains("visibleTextElementCount >= 3"))
    }

    @Test
    fun `company profile aggregates description and populated values across shadow roots`() {
        assertTrue(COMPANY_PROFILE_READY_CHECK.contains(".description"))
        assertTrue(COMPANY_PROFILE_READY_CHECK.contains("querySelectorAll('.row')"))
        assertTrue(COMPANY_PROFILE_READY_CHECK.contains("hasDescription"))
        assertTrue(COMPANY_PROFILE_READY_CHECK.contains("populatedValueCount >= 2"))
    }

    @Test
    fun `iframe widgets wait for the frame load event`() {
        assertTrue(WIDGET_IFRAME_CHECK.contains("__tradingViewWidgetFrameLoaded === true"))
    }

    @Test
    fun `market overview waits for rendered chart or visible content`() {
        assertTrue(MARKET_OVERVIEW_READY_CHECK.contains("__tradingViewShadowRoots"))
        assertTrue(MARKET_OVERVIEW_READY_CHECK.contains("hasRenderedChart"))
        assertTrue(MARKET_OVERVIEW_READY_CHECK.contains("visibleTextElementCount >= 2"))
    }
}
