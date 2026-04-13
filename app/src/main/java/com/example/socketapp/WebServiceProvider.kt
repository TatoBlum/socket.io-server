package com.example.socketapp

import android.util.Log
import com.example.socketapp.Constants.NORMAL_CLOSURE_STATUS
import com.example.socketapp.Constants.WEB_SOCKET_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.util.concurrent.TimeUnit

private const val TAG = "WebServicesProvider"

class WebServicesProvider {
    private var _webSocket: WebSocket? = null

    private val socketOkHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private var _webSocketListener: SocketListener? = null

    private var socketScope = newSocketScope()

    fun startSocket(): SharedFlow<DataState> {
        if (!socketScope.isActive) {
            socketScope = newSocketScope()
        }
        val listener = SocketListener(socketScope)
        startSocket(listener)
        return listener.socketBitcoinPriceEventChannel
    }

    private fun startSocket(webSocketListener: SocketListener) {
        _webSocketListener = webSocketListener
        _webSocket = socketOkHttpClient.newWebSocket(
            Request.Builder().url(WEB_SOCKET_URL).build(),
            webSocketListener
        )
    }

    fun stopSocket() {
        Log.d(TAG, "stopSocket")
        try {
            _webSocket?.close(NORMAL_CLOSURE_STATUS, null)
            _webSocket = null
            _webSocketListener = null
            socketScope.cancel()
        } catch (ex: Exception) {
            Log.w(TAG, "stopSocket failed", ex)
        }
    }

    private fun newSocketScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
