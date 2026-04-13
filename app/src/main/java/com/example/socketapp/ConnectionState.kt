package com.example.socketapp

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Failed(val cause: Throwable) : ConnectionState()
}
