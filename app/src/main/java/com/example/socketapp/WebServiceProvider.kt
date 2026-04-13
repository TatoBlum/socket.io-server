package com.example.socketapp

import com.example.socketapp.Constants.NORMAL_CLOSURE_STATUS
import com.example.socketapp.Constants.WEB_SOCKET_URL
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.util.concurrent.TimeUnit

class WebServicesProvider {
    private var _webSocket: WebSocket? = null

    private val socketOkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(39, TimeUnit.SECONDS)
        .hostnameVerifier { _, _ -> true }
        .build()

    // @ExperimentalCoroutinesApi
    private var _webSocketListener: SocketListener? = null

    // @ExperimentalCoroutinesApi
    /*
    fun startSocket(): Channel<SocketState> =
        with(com.example.socketapp.WebSocketListener()) {
            startSocket(this)
            this@with.socketBitcoinPriceEventChannel
        }
    */

    fun startSocket(): SharedFlow<DataState> {
        val listener = SocketListener()
        startSocket(listener)
        return listener.socketBitcoinPriceEventChannel
    }

    // @ExperimentalCoroutinesApi
    private fun startSocket(webSocketListener: SocketListener) {
        println("_webSocketListener:: $_webSocketListener")
        println("_websocket ::: $_webSocket")
        _webSocketListener = webSocketListener
        _webSocket = socketOkHttpClient.newWebSocket(
            Request.Builder().url(WEB_SOCKET_URL).build(),
            webSocketListener
        )

        // socketOkHttpClient.dispatcher.executorService.shutdown()
        socketOkHttpClient.connectionPool.evictAll()
        println("_webSocketListener2:: $_webSocketListener")
        println("_websocket2::: $_webSocket")
    }

    // @ExperimentalCoroutinesApi
    fun stopSocket() {
        println("stopSocket!!! ");
        try {
            _webSocket?.close(NORMAL_CLOSURE_STATUS, null)
            _webSocket = null

            // _webSocketListener?.socketBitcoinPriceEventChannel?.close()
            _webSocketListener = null
        } catch (ex: Exception) {
        }
    }
}