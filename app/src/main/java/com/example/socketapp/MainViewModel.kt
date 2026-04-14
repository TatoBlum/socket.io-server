package com.example.socketapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"
private const val PUBLISH_INTERVAL_MS = 250L

class MainViewModel(
    private val tickerDataSource: CryptoTickerDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _tickers = MutableStateFlow<Map<String, CryptoTicker>>(emptyMap())
    val tickers: StateFlow<Map<String, CryptoTicker>> = _tickers.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = tickerDataSource.connectionState

    private var socketJob: Job? = null
    private val internalMap = mutableMapOf<String, CryptoTicker>()

    fun subscribeToSocketEvents() {
        if (socketJob?.isActive == true) return

        socketJob = viewModelScope.launch(ioDispatcher) {
            // Coroutine 1: collect from datasource and accumulate
            launch {
                tickerDataSource.start()
                    .catch { ex ->
                        Log.e(TAG, "socket stream ended with error", ex)
                        synchronized(internalMap) { internalMap.clear() }
                        _tickers.value = emptyMap()
                    }
                    .collect { ticker ->
                        synchronized(internalMap) {
                            val existing = internalMap[ticker.symbol]
                            val newPrice = ticker.price.toBigDecimalOrNull()
                            val oldPrice = existing?.price?.toBigDecimalOrNull()
                            val direction = when {
                                newPrice == null || oldPrice == null -> PriceDirection.NEUTRAL
                                newPrice > oldPrice -> PriceDirection.UP
                                newPrice < oldPrice -> PriceDirection.DOWN
                                else -> PriceDirection.NEUTRAL
                            }
                            internalMap[ticker.symbol] = ticker.copy(
                                previousPrice = existing?.price,
                                priceDirection = direction,
                            )
                        }
                    }
            }

            // Coroutine 2: publish snapshot every PUBLISH_INTERVAL_MS
            launch {
                while (isActive) {
                    delay(PUBLISH_INTERVAL_MS)
                    val snapshot = synchronized(internalMap) {
                        if (internalMap.isNotEmpty()) internalMap.toMap() else null
                    }
                    if (snapshot != null) {
                        _tickers.value = snapshot
                    }
                }
            }
        }
    }

    fun stopSocket() {
        socketJob?.cancel()
        socketJob = null
        synchronized(internalMap) { internalMap.clear() }
        _tickers.value = emptyMap()
    }

    override fun onCleared() {
        socketJob?.cancel()
        socketJob = null
        super.onCleared()
    }
}
