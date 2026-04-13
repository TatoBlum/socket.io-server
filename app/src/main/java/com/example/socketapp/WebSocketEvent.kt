package com.example.socketapp

import okio.ByteString

sealed class WebSocketEvent {
    data class Message(val text: String) : WebSocketEvent()
    data class Binary(val bytes: ByteString) : WebSocketEvent()
}
