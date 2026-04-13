package com.example.socketapp

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tests the [WebSocketClient] against a real [MockWebServer] running in-process.
 *
 * Uses [runBlocking] (not runBlockingTest) because MockWebServer operates on real
 * threads and the TCP handshake needs real wall time on localhost.
 */
@ExperimentalCoroutinesApi
class WebSocketClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: WebSocketClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = WebSocketClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun wsUrl(): String =
        server.url("/").toString().replace("http://", "ws://")

    /** Server that accepts the upgrade and immediately sends [messages]. */
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
    fun `happy path emits first message`() = runBlocking {
        enqueueServerSending("""{"c":"34220.10"}""")

        val received = withTimeout(5_000) {
            client.connect(wsUrl()).first()
        }

        // first() consuming implies onOpen → onMessage fired, i.e. socket was Connected.
        assertEquals("""{"c":"34220.10"}""", received)
    }

    @Test
    fun `emits multiple messages in order`() = runBlocking {
        enqueueServerSending("a", "b", "c")

        val received = withTimeout(5_000) {
            client.connect(wsUrl()).take(3).toList()
        }

        assertEquals(listOf("a", "b", "c"), received)
    }

    @Test
    fun `server-initiated close surfaces as IOException`() = runBlocking {
        val serverClosed = CountDownLatch(1)
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send("one")
                    webSocket.close(1001, "going away")
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    serverClosed.countDown()
                }
            })
        )

        var thrown: Throwable? = null
        try {
            withTimeout(5_000) {
                client.connect(wsUrl()).toList()
            }
        } catch (ex: IOException) {
            thrown = ex
        }

        assertTrue("expected IOException, got $thrown", thrown is IOException)
        serverClosed.await(2, TimeUnit.SECONDS)
        assertEquals(ConnectionState.Disconnected, client.connectionState.value)
    }

    @Test
    fun `handshake failure surfaces as IOException and transitions through Failed`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        // Collect connectionState transitions in parallel — awaitClose races with our
        // read and collapses state to Disconnected, so we can't just read .value at the end.
        val observed = mutableListOf<ConnectionState>()
        val stateJob = launch {
            client.connectionState.collect { observed += it }
        }
        yield() // let stateJob start collecting before we fire connect()

        var thrown: Throwable? = null
        try {
            withTimeout(5_000) {
                client.connect(wsUrl()).toList()
            }
        } catch (ex: Exception) {
            thrown = ex
        }

        stateJob.cancel()

        assertTrue(
            "expected IOException, got ${thrown?.javaClass?.simpleName}: $thrown",
            thrown is IOException
        )
        assertTrue(
            "expected Failed in state transitions, got $observed",
            observed.any { it is ConnectionState.Failed }
        )
    }

    @Test
    fun `client can consume first and cancel without hanging`() = runBlocking {
        // Server keeps sending so collection would continue indefinitely if not cancelled.
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    repeat(10) { webSocket.send("msg-$it") }
                }
            })
        )

        val first = withTimeout(5_000) {
            client.connect(wsUrl()).first()
        }

        // Reaching here within the timeout proves first() cancelled the collection and
        // awaitClose ran (otherwise test would hang / leak threads).
        assertEquals("msg-0", first)
    }
}
