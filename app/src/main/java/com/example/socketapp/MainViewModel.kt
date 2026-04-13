package com.example.socketapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

class MainViewModel(
    private val interactor: MainInteractor
): ViewModel() {

    private val _bitcoin = MutableStateFlow<BitcoinTicker?>(null)
    val bitcoin = _bitcoin.asStateFlow()

    private var isSubscribed = false
    private var socketJob: Job? = null

    fun subscribeToSocketEvents() {
        if (isSubscribed) return
        isSubscribed = true
        socketJob = viewModelScope.launch(IO) {
            try {
                interactor.startSocket().collect { state ->
                    state.exception?.let { ex ->
                        Log.e(TAG, "socket state error", ex)
                        return@collect
                    }
                    state.text?.let { bitcoin ->
                        _bitcoin.value = bitcoin
                    }
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                Log.e(TAG, "socket error", ex)
            }
        }
    }

    fun stopSocket() {
        isSubscribed = false
        socketJob?.cancel()
        socketJob = null
        interactor.stopSocket()
    }

    override fun onCleared() {
        isSubscribed = false
        socketJob?.cancel()
        socketJob = null
        interactor.stopSocket()
        super.onCleared()
    }
}

class MainInteractor(private val repository: MainRepository) {
    // @ExperimentalCoroutinesApi
    fun startSocket(): SharedFlow<DataState> = repository.startSocket()

    // @ExperimentalCoroutinesApi
    fun stopSocket() {
        repository.closeSocket()
    }
}

class MainRepository(private val webServicesProvider: WebServicesProvider) {

    // @ExperimentalCoroutinesApi
    fun startSocket(): SharedFlow<DataState> =
        webServicesProvider.startSocket()

    // @ExperimentalCoroutinesApi
    fun closeSocket() {
        webServicesProvider.stopSocket()
    }
}
