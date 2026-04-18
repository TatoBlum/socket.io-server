package com.example.socketapp.ui.tradingview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HotlistsConfigTest {

    // --- HotlistsConfig.toJson() ---

    @Test
    fun `toJson includes exchange field`() {
        // given
        val config = HotlistsConfig(exchange = "BCBA")
        // when
        val json = config.toJson()
        // then
        assertTrue("expected exchange in JSON", json.contains("\"exchange\":\"BCBA\""))
    }

    @Test
    fun `toJson default colorTheme is light`() {
        // given
        val config = HotlistsConfig(exchange = "NASDAQ")
        // when
        val json = config.toJson()
        // then
        assertTrue(json.contains("\"colorTheme\":\"light\""))
    }

    @Test
    fun `toJson width is hardcoded to 100 percent`() {
        // given
        val config = HotlistsConfig(exchange = "NYSE")
        // when
        val json = config.toJson()
        // then
        assertTrue(json.contains("\"width\":\"100%\""))
    }

    @Test
    fun `toJson height is hardcoded to 100 percent`() {
        // given
        val config = HotlistsConfig(exchange = "NYSE")
        // when
        val json = config.toJson()
        // then
        assertTrue(json.contains("\"height\":\"100%\""))
    }

    @Test
    fun `toJson default locale is es`() {
        // given
        val config = HotlistsConfig(exchange = "BCBA")
        // when
        val json = config.toJson()
        // then
        assertTrue(json.contains("\"locale\":\"es\""))
    }

    @Test
    fun `toJson respects overridden locale`() {
        // given
        val config = HotlistsConfig(exchange = "NASDAQ", locale = "en")
        // when
        val json = config.toJson()
        // then
        assertTrue(json.contains("\"locale\":\"en\""))
    }

    @Test
    fun `toJson default showChart is true`() {
        // given
        val config = HotlistsConfig(exchange = "NYSE")
        // when
        val json = config.toJson()
        // then
        assertTrue(json.contains("\"showChart\":true"))
    }

    // --- Exchange enum ---

    @Test
    fun `Exchange BCBA config exchange matches displayName`() {
        // given / when
        val entry = Exchange.BCBA
        // then
        assertEquals(entry.displayName, entry.config.exchange)
    }

    @Test
    fun `Exchange NASDAQ config exchange matches displayName`() {
        val entry = Exchange.NASDAQ
        assertEquals(entry.displayName, entry.config.exchange)
    }

    @Test
    fun `Exchange NYSE config exchange matches displayName`() {
        val entry = Exchange.NYSE
        assertEquals(entry.displayName, entry.config.exchange)
    }

    @Test
    fun `Exchange enum has exactly three entries`() {
        assertEquals(3, Exchange.entries.size)
    }

    @Test
    fun `Exchange entries are BCBA NASDAQ NYSE`() {
        val names = Exchange.entries.map { it.name }
        assertEquals(listOf("BCBA", "NASDAQ", "NYSE"), names)
    }

    @Test
    fun `Exchange NASDAQ locale is en`() {
        assertEquals("en", Exchange.NASDAQ.config.locale)
    }

    @Test
    fun `Exchange NYSE locale is en`() {
        assertEquals("en", Exchange.NYSE.config.locale)
    }

    @Test
    fun `Exchange BCBA locale is es`() {
        assertEquals("es", Exchange.BCBA.config.locale)
    }
}
