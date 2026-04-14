package com.example.socketapp

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests de parsing de [CombinedStreamMessage] + [TickerData] con Moshi.
 * Tests sincrónicos, sin coroutines ni red.
 */
class CombinedStreamParsingTest {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(CombinedStreamMessage::class.java)

    @Test
    fun `payload combinado valido parsea stream data symbol y price`() {
        // given
        val json = """
            {
              "stream": "btcusdt@ticker",
              "data": {
                "s": "BTCUSDT",
                "c": "42000.50",
                "P": "1.23"
              }
            }
        """.trimIndent()

        // when
        val msg = adapter.fromJson(json)

        // then
        assertNotNull(msg)
        assertEquals("btcusdt@ticker", msg?.stream)
        assertEquals("BTCUSDT", msg?.data?.symbol)
        assertEquals("42000.50", msg?.data?.price)
        assertEquals("1.23", msg?.data?.percentChange)
    }

    @Test
    fun `payload sin data field — data es null`() {
        // given
        val json = """{"stream": "btcusdt@ticker"}"""

        // when
        val msg = adapter.fromJson(json)

        // then
        assertNotNull(msg)
        assertNull(msg?.data)
    }

    @Test
    fun `payload sin stream field — stream es null`() {
        // given
        val json = """{"data": {"s": "BTCUSDT", "c": "42000.00", "P": "0.50"}}"""

        // when
        val msg = adapter.fromJson(json)

        // then
        assertNotNull(msg)
        assertNull(msg?.stream)
        assertEquals("BTCUSDT", msg?.data?.symbol)
    }

    @Test
    fun `payload con campos extra — se ignoran y parsea correctamente`() {
        // given
        val json = """
            {
              "stream": "ethusdt@ticker",
              "data": {
                "s": "ETHUSDT",
                "c": "3000.00",
                "P": "2.10",
                "e": "24hrTicker",
                "v": "999999.99",
                "unknownField": true
              },
              "extraTopLevel": 42
            }
        """.trimIndent()

        // when
        val msg = adapter.fromJson(json)

        // then
        assertNotNull(msg)
        assertEquals("ethusdt@ticker", msg?.stream)
        assertEquals("ETHUSDT", msg?.data?.symbol)
        assertEquals("3000.00", msg?.data?.price)
    }

    @Test
    fun `payload data con symbol null — symbol es null`() {
        // given
        val json = """{"stream": "btcusdt@ticker", "data": {"c": "42000.00", "P": "1.00"}}"""

        // when
        val msg = adapter.fromJson(json)

        // then
        assertNotNull(msg)
        assertNull(msg?.data?.symbol)
        assertEquals("42000.00", msg?.data?.price)
    }

    @Test
    fun `payload data con price null — price es null`() {
        // given
        val json = """{"stream": "btcusdt@ticker", "data": {"s": "BTCUSDT", "P": "1.00"}}"""

        // when
        val msg = adapter.fromJson(json)

        // then
        assertNotNull(msg)
        assertEquals("BTCUSDT", msg?.data?.symbol)
        assertNull(msg?.data?.price)
    }
}
