package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(BitcoinTickerDataSource(WebSocketClient())) as T
        }
        throw IllegalArgumentException("Unknown class name")
    }
}