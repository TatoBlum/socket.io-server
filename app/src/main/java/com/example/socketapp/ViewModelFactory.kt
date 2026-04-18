package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.socketapp.socket.StockTickerDataSource
import com.example.socketapp.socket.WebSocketClient

class ViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                StockTickerDataSource(
                    client = WebSocketClient(),
                    keyId = BuildConfig.ALPACA_KEY_ID,
                    secret = BuildConfig.ALPACA_SECRET_KEY,
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}
