package com.example.socketapp.socket

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Tests de parsing de mensajes Alpaca IEX trades via [StockTickerDataSource.parse].
 * Tests sincrónicos, sin coroutines ni red.
 */
@ExperimentalCoroutinesApi
class AlpacaTradesParsingTest {

    private val dataSource = StockTickerDataSource(
        client = WebSocketClient(),
        keyId = "K",
        secret = "S",
    )

    @Test
    fun `empty array returns empty`() {
        val result = dataSource.parse("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `three valid trades return three tickers with formatted prices`() {
        val json = """[
            {"T":"t","S":"AAPL","p":189.55},
            {"T":"t","S":"TSLA","p":234.5},
            {"T":"t","S":"MSFT","p":410.0}
        ]"""
        val result = dataSource.parse(json)
        assertEquals(3, result.size)
        assertEquals("AAPL", result[0].symbol)
        assertEquals("189.55", result[0].price)
        assertEquals("234.50", result[1].price)
        assertEquals("410.00", result[2].price)
    }

    @Test
    fun `heterogeneous array returns only trade tickers`() {
        val json = """[
            {"T":"success","msg":"connected"},
            {"T":"t","S":"TSLA","p":234.5},
            {"T":"error","code":400,"msg":"bad request"}
        ]"""
        val result = dataSource.parse(json)
        assertEquals(1, result.size)
        assertEquals("TSLA", result[0].symbol)
    }

    @Test
    fun `trade without S field returns empty`() {
        val result = dataSource.parse("""[{"T":"t","p":100.0}]""")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `trade without p field returns empty`() {
        val result = dataSource.parse("""[{"T":"t","S":"AAPL"}]""")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `trade with numeric p formats to two decimals`() {
        val result = dataSource.parse("""[{"T":"t","S":"AAPL","p":189.5}]""")
        assertEquals(1, result.size)
        assertEquals("189.50", result[0].price)
    }

    @Test
    fun `bare object (not array) returns empty without crashing`() {
        val result = dataSource.parse("""{"T":"success","msg":"connected"}""")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invalid json returns empty without crashing`() {
        val result = dataSource.parse("not json")
        assertTrue(result.isEmpty())
    }

    // ---------- key validation ----------

    @Test
    fun `start con keyId vacio lanza IllegalStateException`() = runTest {
        // given
        val ds = StockTickerDataSource(client = WebSocketClient(), keyId = "", secret = "secret")
        var caught: Throwable? = null
        // when
        ds.start().catch { caught = it }.toList()
        // then
        assertTrue("expected IllegalStateException, got $caught", caught is IllegalStateException)
    }

    @Test
    fun `start con secret vacio lanza IllegalStateException`() = runTest {
        // given
        val ds = StockTickerDataSource(client = WebSocketClient(), keyId = "key", secret = "")
        var caught: Throwable? = null
        // when
        ds.start().catch { caught = it }.toList()
        // then
        assertTrue("expected IllegalStateException, got $caught", caught is IllegalStateException)
    }

    @Test
    fun `start con ambas keys vacias lanza IllegalStateException`() = runTest {
        // given
        val ds = StockTickerDataSource(client = WebSocketClient(), keyId = "", secret = "")
        var caught: Throwable? = null
        // when
        ds.start().catch { caught = it }.toList()
        // then
        assertTrue("expected IllegalStateException, got $caught", caught is IllegalStateException)
    }
}
