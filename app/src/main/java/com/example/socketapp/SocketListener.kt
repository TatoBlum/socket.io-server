package com.example.socketapp

import com.example.socketapp.Constants.NORMAL_CLOSURE_STATUS
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.cancel
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class SocketListener: WebSocketListener() {

    //val socketBitcoinPriceEventChannel: Channel<DataState> = Channel()
    private val _socketBitcoinPriceEventChannel = MutableSharedFlow<DataState>()
    val socketBitcoinPriceEventChannel = _socketBitcoinPriceEventChannel.asSharedFlow()

    override fun onOpen(webSocket: WebSocket, response: Response) {

        webSocket.send(
            "{\n" +
                "    \"type\": \"subscribe\",\n" +
                "    \"channels\": [{ \"name\": \"ticker\", \"product_ids\": [\"BTC-EUR\"] }]\n" +
                "}"
        )

        GlobalScope.launch {
            _socketBitcoinPriceEventChannel.emit(DataState(webSocket = webSocket))
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println("Text $text")
        GlobalScope.launch {
            _socketBitcoinPriceEventChannel.emit(DataState(textToBitcoinTicket(text)))
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println("ONCLOSING: $code $reason");

        webSocket.send(
            "{\n" +
                "    \"type\": \"unsubscribe\",\n" +
                "    \"channels\": [\"ticker\"]\n" +
                "}"
        )

        GlobalScope.launch {
            _socketBitcoinPriceEventChannel.emit(DataState(exception = SocketAbortedException()))
        }

        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        // _socketBitcoinPriceEventChannel.close()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("ONFAILURE: $t ${response?.body}");
        println("ONFAILURE cause: $t ${t.cause}");
        println("stackTrace: $t ${t.stackTrace}");

        GlobalScope.launch {
            _socketBitcoinPriceEventChannel.emit(DataState(exception = t))
        }
    }

    private fun textToBitcoinTicket(text: String): BitcoinTicker? {
        val moshi = Moshi.Builder().build()
        val adapter: JsonAdapter<BitcoinTicker> = moshi.adapter(BitcoinTicker::class.java)
        val tempBitcoin = adapter.fromJson(text)
        return tempBitcoin
    }
}

class SocketAbortedException : Exception()

data class DataState(
    val text: BitcoinTicker? = null,
    val byteString: ByteString? = null,
    val exception: Throwable? = null,
    val webSocket: WebSocket? = null,
)