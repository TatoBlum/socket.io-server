package com.example.socketapp

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure sync tests of the Moshi deserialization of [BitcoinTicker].
 * No coroutines, no server, no networking.
 */
class BitcoinTickerParsingTest {

    private val adapter = Moshi.Builder().build().adapter(BitcoinTicker::class.java)

    @Test
    fun `valid payload with c field parses price`() {
        val ticker = adapter.fromJson("""{"c":"34220.10"}""")
        assertNotNull(ticker)
        assertEquals("34220.10", ticker?.price)
    }

    @Test
    fun `payload missing c field yields null price`() {
        val ticker = adapter.fromJson("""{}""")
        assertNotNull(ticker)
        assertNull(ticker?.price)
    }

    @Test
    fun `payload with extra fields parses c and ignores the rest`() {
        val ticker = adapter.fromJson("""{"e":"24hrTicker","s":"BTCEUR","c":"42000.00","v":"123.45"}""")
        assertNotNull(ticker)
        assertEquals("42000.00", ticker?.price)
    }

    @Test
    fun `malformed JSON surfaces as exception`() {
        var thrown: Throwable? = null
        try {
            adapter.fromJson("""{"c":broken}""")
        } catch (ex: Exception) {
            thrown = ex
        }
        assertNotNull("expected an exception for malformed JSON", thrown)
    }
}
