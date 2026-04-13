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

/**
 * Domain-level source for Bitcoin ticker data. Owns the endpoint URL, the
 * payload→model mapping and the reconnect policy. Transport is delegated to
 * [WebSocketClient].
 */
class BitcoinTickerDataSource(private val client: WebSocketClient) {

    val connectionState: StateFlow<ConnectionState> = client.connectionState

    fun start(): Flow<BitcoinTicker> = client.connect(Constants.WEB_SOCKET_URL)
        .mapNotNull { event ->
            when (event) {
                is WebSocketEvent.Message -> parse(event.text)
                is WebSocketEvent.Binary -> null
            }
        }
        .retryWhen { cause, attempt ->
            if (cause is CancellationException) return@retryWhen false
            if (attempt >= MAX_RETRIES) {
                Log.e(TAG, "giving up after $MAX_RETRIES retries", cause)
                return@retryWhen false
            }
            val backoff = (BASE_BACKOFF_MS shl attempt.toInt()).coerceAtMost(MAX_BACKOFF_MS)
            val jitter = Random.nextLong(0L, 500L)
            Log.w(TAG, "retry ${attempt + 1}/$MAX_RETRIES in ${backoff + jitter}ms", cause)
            delay(backoff + jitter)
            true
        }

    private fun parse(text: String): BitcoinTicker? {
        val ticker = try {
            adapter.fromJson(text)
        } catch (ex: Exception) {
            Log.w(TAG, "parse failed: ${text.take(120)}", ex)
            return null
        }
        if (ticker?.price == null) {
            Log.w(TAG, "parse: price null. Raw: ${text.take(200)}")
            return null
        }
        Log.d(TAG, "parse → price=${ticker.price}")
        return ticker
    }

    companion object {
        private val moshi = Moshi.Builder().build()
        private val adapter: JsonAdapter<BitcoinTicker> =
            moshi.adapter(BitcoinTicker::class.java)
    }
}
