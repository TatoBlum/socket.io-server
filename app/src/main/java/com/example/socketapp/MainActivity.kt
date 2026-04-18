package com.example.socketapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.socketapp.ui.MainScreen
import com.example.socketapp.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val mainViewModel = ViewModelProvider(this, ViewModelFactory())[MainViewModel::class.java]
        val checkNetworkConnection = CheckNetworkConnection(application)
        setContent {
            AppTheme {
                MainScreen(mainViewModel, checkNetworkConnection)
            }
        }
    }
}
