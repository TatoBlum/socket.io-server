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

private const val TAG = "CryptoTickerDataSource"
private const val MAX_RETRIES = 5L
private const val BASE_BACKOFF_MS = 1_000L
private const val MAX_BACKOFF_MS = 30_000L
private const val MAX_JITTER_MS = 500L

open class CryptoTickerDataSource(
    private val client: WebSocketClient,
    private val url: String = Constants.combinedStreamUrl(),
) {
    open val connectionState: StateFlow<ConnectionState> = client.connectionState

    open fun start(): Flow<CryptoTicker> = client.connect(url)
        .mapNotNull { text -> parse(text) }
        .retryWhen { cause, attempt ->
            when {
                cause is CancellationException -> false
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

    private fun nextBackoffMs(attempt: Long): Long {
        val exponential = (BASE_BACKOFF_MS shl attempt.toInt()).coerceAtMost(MAX_BACKOFF_MS)
        val jitter = Random.nextLong(0L, MAX_JITTER_MS)
        return exponential + jitter
    }

    private fun parse(text: String): CryptoTicker? {
        val message = try {
            adapter.fromJson(text)
        } catch (ex: Exception) {
            Log.w(TAG, "parse failed (len=${text.length})", ex)
            return null
        }
        val data = message?.data ?: return null
        val symbol = data.symbol ?: return null
        val price = data.price ?: return null

        return CryptoTicker(
            symbol = symbol,
            displayName = Constants.displayName(symbol),
            price = price,
            percentChange = data.percentChange ?: "0.00",
        )
    }

    companion object {
        private val moshi = Moshi.Builder().build()
        private val adapter: JsonAdapter<CombinedStreamMessage> =
            moshi.adapter(CombinedStreamMessage::class.java)
    }
}
