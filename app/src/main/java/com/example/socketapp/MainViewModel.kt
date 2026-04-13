package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel constructor(
    private val interactor: MainInteractor
): ViewModel() {

    private val _bitcoin = MutableStateFlow(BitcoinTicker("0"))
    val bitcoin = _bitcoin.asStateFlow()

    //@ExperimentalCoroutinesApi
    fun subscribeToSocketEvents() {
        viewModelScope.launch(IO) {
            try {
                interactor.startSocket().collect{
                    println("COLLECTION ::: ${it.text}")

                    it.text?.let {bitcoin->
                        _bitcoin.value = bitcoin
                    }
                }
                /*
                interactor.startSocket().onEach {
                    if (it.exception == null) {
                        println("COLLECTION ::: ${it.text}")
                        _bitcoin.value = it.text!!
                    } else {
                        onSocketError(it.exception!!)
                    }
                }
                */
            } catch (ex: java.lang.Exception) {
               // onSocketError(ex)
                println("vm exception ${ex.cause}")
                println("vm exception ${ex.message}")
                println("vm exception ${ex.localizedMessage}")
            }
        }
    }

    private fun onSocketError(ex: Throwable) {
        println("Error occurred : ${ex.message}")
    }

    fun stopSocket() {
        interactor.stopSocket()
    }

    override fun onCleared() {
        interactor.stopSocket()
        super.onCleared()
    }
}

class MainInteractor constructor(private val repository: MainRepository) {

    // @ExperimentalCoroutinesApi
    fun startSocket(): SharedFlow<DataState> = repository.startSocket()

    // @ExperimentalCoroutinesApi
    fun stopSocket() {
        repository.closeSocket()
    }
}

class MainRepository constructor(private val webServicesProvider: WebServicesProvider) {

    // @ExperimentalCoroutinesApi
    fun startSocket(): SharedFlow<DataState> =
        webServicesProvider.startSocket()

    // @ExperimentalCoroutinesApi
    fun closeSocket() {
        webServicesProvider.stopSocket()
    }
}