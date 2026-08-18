package com.example.socketapp.ui.tradingview

import org.junit.Assert.assertEquals
import org.junit.Test

class TradingViewWidgetStateTest {

    @Test
    fun `loading without cached content blocks the widget`() {
        assertEquals(
            TradingViewWidgetState.Loading,
            tradingViewLoadingState(hasCachedContent = false),
        )
    }

    @Test
    fun `loading with cached content refreshes in background`() {
        assertEquals(
            TradingViewWidgetState.Refreshing,
            tradingViewLoadingState(hasCachedContent = true),
        )
    }

    @Test
    fun `failure without cached content is blocking error`() {
        assertEquals(
            TradingViewWidgetState.Error("Proveedor no disponible"),
            tradingViewFailureState(
                hasCachedContent = false,
                message = "Proveedor no disponible",
            ),
        )
    }

    @Test
    fun `failure with cached content preserves stale widget`() {
        assertEquals(
            TradingViewWidgetState.Stale("Proveedor no disponible"),
            tradingViewFailureState(
                hasCachedContent = true,
                message = "Proveedor no disponible",
            ),
        )
    }

}
