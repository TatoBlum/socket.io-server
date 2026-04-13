package com.example.socketapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

class MainViewModel(
    private val tickerDataSource: BitcoinTickerDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): ViewModel() {

    private val _bitcoin = MutableStateFlow<BitcoinTicker?>(null)
    val bitcoin = _bitcoin.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = tickerDataSource.connectionState

    private var socketJob: Job? = null

    fun subscribeToSocketEvents() {
        if (socketJob?.isActive == true) return
        socketJob = viewModelScope.launch(ioDispatcher) {
            tickerDataSource.start()
                .catch { ex ->
                    Log.e(TAG, "socket stream ended with error", ex)
                    _bitcoin.value = null
                }
                .collect { ticker ->
                    _bitcoin.value = ticker
                    Log.d(TAG, "received price=${ticker.price}")
                }
        }
    }

    fun stopSocket() {
        socketJob?.cancel()
        socketJob = null
        _bitcoin.value = null
    }

    override fun onCleared() {
        socketJob?.cancel()
        socketJob = null
        super.onCleared()
    }
}
