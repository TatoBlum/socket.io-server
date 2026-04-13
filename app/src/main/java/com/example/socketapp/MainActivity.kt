package com.example.socketapp

import android.annotation.SuppressLint
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.socketapp.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest

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
        lifecycleScope.launchWhenStarted {
            mainViewModel.bitcoin.collectLatest { bitcoin ->
                binding.btcPriceTv.text = bitcoin?.price
                    ?.let { "1 BTC: $it €" }
                    ?: "1 BTC: — €"
            }
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
