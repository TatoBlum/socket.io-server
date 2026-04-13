package com.example.socketapp

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.socketapp.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var mainViewModel: MainViewModel
    private lateinit var checkNetworkConnection: CheckNetworkConnection


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainViewModel = ViewModelProvider(this, ViewModelFactory())[MainViewModel::class.java]
    }


    override fun onResume() {
        super.onResume()
        subscribeObserver()
        callNetworkConnection()
    }

    @SuppressLint("SetTextI18n")
    private fun subscribeObserver() {
        lifecycleScope.launchWhenStarted {
            mainViewModel.bitcoin.collectLatest { bitcoin->
                binding.btcPriceTv.text = "1 BTC: ${bitcoin.price} €"
            }
        }
    }

    private fun callNetworkConnection() {
        checkNetworkConnection = CheckNetworkConnection(application)
        checkNetworkConnection.observe(this) { isConnected ->
            if (isConnected) {
                println("IS CONNECTED YEAH!!!")
                mainViewModel.subscribeToSocketEvents()
            } else {
                println("IS DISCONNECTED OUCH!!!")
                mainViewModel.stopSocket()
            }
        }
    }
}
