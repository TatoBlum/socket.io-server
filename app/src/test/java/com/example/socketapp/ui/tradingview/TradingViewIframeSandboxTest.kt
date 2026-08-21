package com.example.socketapp.ui.tradingview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TradingViewIframeSandboxTest {

    @Test
    fun `sandbox uses only the approved capabilities`() {
        assertEquals(
            "allow-scripts allow-same-origin allow-forms",
            TRADING_VIEW_IFRAME_SANDBOX,
        )
    }

    @Test
    fun `sandbox bootstrap runs before the provider script`() {
        val html = buildTradingViewWidgetHtml(
            templateHtml = """
                <html>
                <head></head>
                <body><script src="{{SCRIPT_SRC}}">{{CONFIG}}</script></body>
                </html>
            """.trimIndent(),
            scriptSrc = "https://s3.tradingview.com/widget.js",
            configJson = "{ test: true }",
        )

        assertTrue(html.contains(TRADING_VIEW_IFRAME_SANDBOX_BOOTSTRAP))
        assertTrue(
            html.indexOf("enforceTradingViewIframeSandbox") <
                html.indexOf("https://s3.tradingview.com/widget.js"),
        )
        assertTrue(html.contains("{ test: true }"))
    }

    @Test
    fun `trusted bridge origin excludes paths and preserves explicit ports`() {
        assertEquals(BASE_ORIGIN, trustedOriginFor(BASE_URL))
        assertEquals(
            "https://widgets.example:8443",
            trustedOriginFor("https://widgets.example:8443/embed/index.html"),
        )
    }
}
