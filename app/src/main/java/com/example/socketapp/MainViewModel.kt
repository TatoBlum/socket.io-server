package com.example.socketapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

class MainViewModel(
    private val interactor: MainInteractor
): ViewModel() {

    private val _bitcoin = MutableStateFlow<BitcoinTicker?>(null)
    val bitcoin = _bitcoin.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = interactor.connectionState

    private var socketJob: Job? = null

    fun subscribeToSocketEvents() {
        if (socketJob?.isActive == true) return
        socketJob = viewModelScope.launch(IO) {
            interactor.startSocket()
                .catch { ex ->
                    Log.e(TAG, "socket stream ended with error", ex)
                    _bitcoin.value = null
                }
                .collect { ticker ->
                    Log.d(TAG, "ticker received: ${ticker.price}")
                    _bitcoin.value = ticker
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

class MainInteractor(private val repository: MainRepository) {
    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    fun startSocket(): Flow<BitcoinTicker> = repository.startSocket()
}

class MainRepository(private val dataSource: BitcoinTickerDataSource) {
    val connectionState: StateFlow<ConnectionState> = dataSource.connectionState

    fun startSocket(): Flow<BitcoinTicker> = dataSource.start()
}
