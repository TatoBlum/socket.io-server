package com.example.socketapp.ui.tradingview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanyProfileConfigTest {

    @Test
    fun `attributes contain the selected company symbol`() {
        val attributes = CompanyProfileConfig(symbol = "NASDAQ:AAPL").toHtmlAttributes()

        assertTrue(attributes.contains("symbol=\"NASDAQ:AAPL\""))
    }

    @Test
    fun `symbol is escaped before insertion into HTML`() {
        val attributes = CompanyProfileConfig(symbol = "<unsafe>").toHtmlAttributes()

        assertFalse(attributes.contains("<unsafe>"))
        assertTrue(attributes.contains("&lt;unsafe&gt;"))
    }

    @Test
    fun `localized company fields are included and escaped`() {
        val attributes = CompanyProfileConfig(
            symbol = "NASDAQ:AAPL",
            localization = CompanyProfileLocalization(
                sector = "Tecnología & software",
                industry = "Equipos \"móviles\"",
                description = "Descripción <localizada>",
            ),
        ).toHtmlAttributes()

        assertTrue(attributes.contains("data-localized-sector=\"Tecnología &amp; software\""))
        assertTrue(attributes.contains("data-localized-industry=\"Equipos &quot;móviles&quot;\""))
        assertTrue(attributes.contains("data-localized-description=\"Descripción &lt;localizada&gt;\""))
    }
}
