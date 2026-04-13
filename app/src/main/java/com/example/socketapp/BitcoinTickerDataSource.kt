package com.example.socketapp

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.retryWhen
import kotlin.random.Random

private const val TAG = "BitcoinTickerDataSource"
private const val MAX_RETRIES = 5L
private const val BASE_BACKOFF_MS = 1_000L
private const val MAX_BACKOFF_MS = 30_000L
private const val MAX_JITTER_MS = 500L

/**
 * Domain-level source for Bitcoin ticker data. Owns the endpoint URL, the
 * payload→model mapping and the reconnect policy. Transport is delegated to
 * [WebSocketClient].
 */
open class BitcoinTickerDataSource(
    private val client: WebSocketClient,
    private val url: String = Constants.WEB_SOCKET_URL,
) {

    open val connectionState: StateFlow<ConnectionState> = client.connectionState

    open fun start(): Flow<BitcoinTicker> = client.connect(url)
        .mapNotNull { text -> parse(text) }
        .retryWhen { cause, attempt ->
            when {
                cause is CancellationException -> false      // stop pedido por el collector
                attempt >= MAX_RETRIES -> {
                    Log.e(TAG, "giving up after $MAX_RETRIES retries", cause)
                    false
                }
                else -> {
                    val waitMs = nextBackoffMs(attempt)
                    Log.w(TAG, "retry ${attempt + 1}/$MAX_RETRIES in ${waitMs}ms", cause)
                    delay(waitMs)
                    true
                }
            }
        }

    /**
     * Backoff exponencial con jitter: 1s, 2s, 4s, 8s, 16s (tope 30s),
     * +0-500ms random para evitar retry storms sincronizados.
     */
    private fun nextBackoffMs(attempt: Long): Long {
        val exponential = (BASE_BACKOFF_MS shl attempt.toInt()).coerceAtMost(MAX_BACKOFF_MS)
        val jitter = Random.nextLong(0L, MAX_JITTER_MS)
        return exponential + jitter
    }

    private fun parse(text: String): BitcoinTicker? {
        val ticker = try {
            adapter.fromJson(text)
        } catch (ex: Exception) {
            Log.w(TAG, "parse failed (len=${text.length})", ex)
            return null
        }
        if (ticker?.price == null) {
            Log.w(TAG, "parse: price null (len=${text.length})")
            return null
        }
        return ticker
    }

    companion object {
        private val moshi = Moshi.Builder().build()
        private val adapter: JsonAdapter<BitcoinTicker> =
            moshi.adapter(BitcoinTicker::class.java)
    }
}
