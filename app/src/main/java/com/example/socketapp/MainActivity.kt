package com.example.socketapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.socketapp.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var checkNetworkConnection: CheckNetworkConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainViewModel = ViewModelProvider(this, ViewModelFactory())[MainViewModel::class.java]

        subscribeObserver()
        callNetworkConnection()
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    mainViewModel.bitcoin,
                    mainViewModel.connectionState
                ) { ticker, state -> renderLabel(ticker, state) }
                    .distinctUntilChanged()
                    .collect { label -> binding.btcPriceTv.text = label }
            }
        }
    }

    private fun renderLabel(ticker: BitcoinTicker?, state: ConnectionState): String {
        val price = ticker?.price
        return when {
            price != null -> "1 BTC: $price €"
            state is ConnectionState.Connecting -> "Conectando..."
            state is ConnectionState.Failed -> "Error de conexión"
            state is ConnectionState.Disconnected -> "Sin conexión"
            else -> "1 BTC: — €"
        }
    }

    private fun callNetworkConnection() {
        checkNetworkConnection = CheckNetworkConnection(application)
        checkNetworkConnection.observe(this) { isConnected ->
            if (isConnected) {
                Log.i(TAG, "IS CONNECTED YEAH!!!")
                mainViewModel.subscribeToSocketEvents()
            } else {
                Log.w(TAG, "IS DISCONNECTED OUCH!!!")
                mainViewModel.stopSocket()
            }
        }
    }
}
