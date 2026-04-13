package com.example.socketapp

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

private const val TAG = "SocketListener"

class SocketListener(private val scope: CoroutineScope) : WebSocketListener() {

    private val _socketBitcoinPriceEventChannel = MutableSharedFlow<DataState>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val socketBitcoinPriceEventChannel = _socketBitcoinPriceEventChannel.asSharedFlow()

    companion object {
        private val moshi = Moshi.Builder().build()
        private val adapter: JsonAdapter<BitcoinTicker> = moshi.adapter(BitcoinTicker::class.java)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        scope.launch {
            _socketBitcoinPriceEventChannel.emit(DataState(webSocket = webSocket))
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val ticker = textToBitcoinTicket(text) ?: return
        if (ticker.price == null) return
        scope.launch {
            _socketBitcoinPriceEventChannel.emit(DataState(text = ticker))
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.d(TAG, "ONCLOSING: $code $reason")
        webSocket.close(Constants.NORMAL_CLOSURE_STATUS, null)
        scope.launch {
            _socketBitcoinPriceEventChannel.emit(DataState(exception = SocketAbortedException()))
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e(TAG, "onFailure", t)
        scope.launch {
            _socketBitcoinPriceEventChannel.emit(DataState(exception = t))
        }
    }

    private fun textToBitcoinTicket(text: String): BitcoinTicker? =
        try {
            adapter.fromJson(text)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse ticker payload", e)
            null
        }
}

class SocketAbortedException : Exception()

data class DataState(
    val text: BitcoinTicker? = null,
    val byteString: ByteString? = null,
    val exception: Throwable? = null,
    val webSocket: WebSocket? = null,
)
