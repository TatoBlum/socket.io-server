package com.example.socketapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val TAG = "MainScreen"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainViewModel = ViewModelProvider(this, ViewModelFactory())[MainViewModel::class.java]
        val checkNetworkConnection = CheckNetworkConnection(application)

        setContent {
            MaterialTheme {
                MainScreen(mainViewModel, checkNetworkConnection)
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    networkConnection: CheckNetworkConnection,
) {
    val ticker by viewModel.bitcoin.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val isConnected by networkConnection.observeAsState(initial = false)

    LaunchedEffect(isConnected) {
        if (isConnected) {
            Log.i(TAG, "IS CONNECTED YEAH!!!")
            viewModel.subscribeToSocketEvents()
        } else {
            Log.w(TAG, "IS DISCONNECTED OUCH!!!")
            viewModel.stopSocket()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = renderLabel(ticker, connection), fontSize = 40.sp)
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
