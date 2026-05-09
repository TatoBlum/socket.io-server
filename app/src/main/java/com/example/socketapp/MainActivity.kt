package com.example.socketapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.socketapp.ui.MainScreen
import com.example.socketapp.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val checkNetworkConnection = CheckNetworkConnection(application)
        setContent {
            AppTheme {
                MainScreen(mainViewModel, checkNetworkConnection)
            }
        }
    }
}
