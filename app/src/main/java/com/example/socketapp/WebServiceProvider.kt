package com.example.socketapp

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

private const val TAG = "WebSocketClient"
private const val NORMAL_CLOSURE_STATUS = 1000

/**
 * Transport-only WebSocket client. Agnostic to payload and business.
 * The returned flow is cold: each collect opens a fresh socket; cancelling
 * the collecting coroutine closes it via [awaitClose].
 */
class WebSocketClient {
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun connect(url: String): Flow<WebSocketEvent> = callbackFlow {
        Log.d(TAG, "connect → $url")
        _connectionState.value = ConnectionState.Connecting

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "onOpen: ${response.code}")
                _connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val r = trySend(WebSocketEvent.Message(text))
                Log.d(TAG, "onMessage(text, ${text.length} chars) → trySend success=${r.isSuccess}")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val r = trySend(WebSocketEvent.Binary(bytes))
                Log.d(TAG, "onMessage(bytes) → trySend success=${r.isSuccess}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "onClosing: $code $reason")
                webSocket.close(NORMAL_CLOSURE_STATUS, null)
                _connectionState.value = ConnectionState.Disconnected
                close()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "onFailure (response=${response?.code})", t)
                // Si el handshake HTTP falló, OkHttp nos pasa el Response y es
                // responsabilidad nuestra cerrarlo; si no, dispara el warning al GC.
                response?.close()
                _connectionState.value = ConnectionState.Failed(t)
                close(t)
            }
        }

        val webSocket = okHttpClient.newWebSocket(
            Request.Builder().url(url).build(),
            listener
        )

        awaitClose {
            // cancel() libera el Call subyacente de forma inmediata, haya conectado o no.
            // close() sólo funciona si el handshake ya estaba establecido, y puede dejar
            // recursos colgando ("A resource failed to call close") si el socket murió
            // durante el handshake.
            webSocket.cancel()
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}
