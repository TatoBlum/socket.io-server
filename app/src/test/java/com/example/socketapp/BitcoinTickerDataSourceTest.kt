package com.example.socketapp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Tests the [BitcoinTickerDataSource] against a real [MockWebServer].
 *
 * These tests use [runBlocking] (not runBlockingTest) because MockWebServer is real.
 * The retry backoff (1s, 2s, 4s...) WILL elapse in real time. To keep the suite fast,
 * most tests succeed within the first attempt (no retry path exercised) or assert
 * failure after a bounded number of retries.
 */
@ExperimentalCoroutinesApi
class BitcoinTickerDataSourceTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun wsUrl(): String =
        server.url("/").toString().replace("http://", "ws://")

    private fun dataSource() = BitcoinTickerDataSource(WebSocketClient(), wsUrl())

    private fun enqueueServerSending(vararg messages: String) {
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    messages.forEach { webSocket.send(it) }
                }
            })
        )
    }

    @Test
    fun `happy path parses a valid payload into a BitcoinTicker`() = runBlocking {
        enqueueServerSending("""{"c":"34220.10"}""")

        val ticker = withTimeout(5_000) { dataSource().start().first() }

        assertEquals("34220.10", ticker.price)
    }

    @Test
    fun `invalid JSON is filtered and only valid tickers are emitted`() = runBlocking {
        enqueueServerSending(
            """not-json""",
            """{"c":null}""",   // price null → dropped
            """{"c":"42000.00"}"""
        )

        val ticker = withTimeout(5_000) { dataSource().start().first() }

        assertEquals("42000.00", ticker.price)
    }

    @Test
    fun `retry reconnects after server closes and emits from the second connection`() = runBlocking {
        // First attempt: open, send nothing, close immediately → retryWhen triggers
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.close(1001, "bye")
                }
            })
        )
        // Second attempt: send a valid ticker. The retry backoff (~1s) runs in real time.
        enqueueServerSending("""{"c":"55555.55"}""")

        val ticker = withTimeout(10_000) { dataSource().start().first() }

        assertEquals("55555.55", ticker.price)
    }

    @Test
    fun `external cancellation stops the flow without retry storm`() = runBlocking {
        // Server keeps the socket open without sending anything → collector suspends.
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                // no-op: accept handshake and stay silent
            })
        )

        val job = launch {
            dataSource().start().collect { _: BitcoinTicker -> /* never emits */ }
        }
        // Give the connection time to establish. 1s is conservative for CI under load;
        // the TCP handshake on localhost normally completes in <50ms but CI boxes can
        // stall. If this becomes flaky, move to a fake WebSocketClient with virtual time.
        delay(1_000)
        job.cancel()
        // If retry triggered on CancellationException, this would loop forever.
        // A successful exit within the timeout proves cancellation stops the flow.
        withTimeout(2_000) { job.join() }
        assertTrue("job should be cancelled", job.isCancelled)
    }

    /**
     * Exercises MAX_RETRIES exhaustion using a fake client so backoff delays run under
     * virtual time. With real MockWebServer this would take ~31s of wall-clock backoff.
     */
    @Test
    fun `gives up after MAX_RETRIES exhausted`() = runBlockingTest {
        val attempts = AtomicInteger(0)
        // Count must live INSIDE the flow block — retryWhen re-collects the same Flow,
        // so the counter needs to observe collects, not connect() calls.
        val fake = FakeWebSocketClient { _ ->
            flow {
                attempts.incrementAndGet()
                throw java.io.IOException("simulated connection failure")
            }
        }
        val dataSource = BitcoinTickerDataSource(fake, "ws://fake")

        var thrown: Throwable? = null
        try {
            dataSource.start().collect { }
        } catch (ex: Exception) {
            thrown = ex
        }

        // 1 initial attempt + 5 retries (MAX_RETRIES) = 6 collect invocations
        assertEquals(6, attempts.get())
        assertTrue(
            "expected IOException after exhausting retries, got $thrown",
            thrown is java.io.IOException
        )
    }
}

/**
 * Test-only stub that replaces network I/O with a caller-supplied flow factory.
 * Each call to [connect] invokes [behavior] — use an [AtomicInteger] in tests to
 * count attempts.
 */
private class FakeWebSocketClient(
    private val behavior: (url: String) -> Flow<String>
) : WebSocketClient() {
    override fun connect(url: String): Flow<String> = behavior(url)
}
