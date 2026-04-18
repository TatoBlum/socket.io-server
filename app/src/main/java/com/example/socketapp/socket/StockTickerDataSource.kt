package com.example.socketapp.socket

import android.util.Log
import com.example.socketapp.Constants
import com.example.socketapp.model.ConnectionState
import com.example.socketapp.model.PriceDirection
import com.example.socketapp.model.StockTicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale
import kotlin.random.Random

private const val TAG = "StockTickerDataSource"
private const val MAX_RETRIES = 5L
private const val BASE_BACKOFF_MS = 1_000L
private const val MAX_BACKOFF_MS = 30_000L
private const val MAX_JITTER_MS = 500L

open class StockTickerDataSource(
    private val client: WebSocketClient,
    private val keyId: String = "",
    private val secret: String = "",
    private val symbols: List<String> = Constants.SYMBOLS,
    private val url: String = Constants.ALPACA_WS_URL,
) {
    open val connectionState: StateFlow<ConnectionState> = client.connectionState

    @OptIn(ExperimentalCoroutinesApi::class)
    open fun start(): Flow<StockTicker> {
        if (keyId.isBlank() || secret.isBlank()) {
            return flow { throw IllegalStateException("Alpaca API key missing — see local.properties.example") }
        }

        return client.connect(url, onOpen = { ws ->
            Log.i(TAG, "Alpaca connected — trades flow only during market hours (9:30-16:00 ET)")
            ws.send(authJson())
            Log.d(TAG, "auth sent for keyId=${keyId.take(4)}***")
            ws.send(subscribeJson())
            Log.d(TAG, "subscribe sent for ${symbols.size} symbols")
        })
            .flatMapConcat { text -> parse(text).asFlow() }
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
    }

    private fun nextBackoffMs(attempt: Long): Long {
        val exponential = (BASE_BACKOFF_MS shl attempt.toInt()).coerceAtMost(MAX_BACKOFF_MS)
        val jitter = Random.nextLong(0L, MAX_JITTER_MS)
        return exponential + jitter
    }

    private fun authJson(): String =
        JSONObject()
            .put("action", "auth")
            .put("key", keyId)
            .put("secret", secret)
            .toString()

    private fun subscribeJson(): String =
        JSONObject()
            .put("action", "subscribe")
            .put("trades", JSONArray(symbols))
            .toString()

    internal fun parse(text: String): List<StockTicker> {
        return try {
            when (val root = JSONTokener(text).nextValue()) {
                is JSONArray -> (0 until root.length())
                    .mapNotNull { i -> root.optJSONObject(i)?.let { toTicker(it) } }
                else -> emptyList()
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun toTicker(obj: JSONObject): StockTicker? {
        if (obj.optString("T") != "t") return null
        val symbol = obj.optString("S").takeIf { it.isNotEmpty() } ?: return null
        val price = obj.optDouble("p", Double.NaN).takeIf { !it.isNaN() } ?: return null
        return StockTicker(
            symbol = symbol,
            displayName = Constants.displayName(symbol),
            price = String.format(Locale.US, "%.2f", price),
            previousPrice = "0.00",
            priceDirection = PriceDirection.NEUTRAL,
            percentChange = "0.00",
        )
    }
}
