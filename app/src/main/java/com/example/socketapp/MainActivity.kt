package com.example.socketapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = renderPriceLabel(ticker, connection), fontSize = 24.sp, color = Color.White)
            Spacer(Modifier.height(16.dp))
            val statusLabel = when (val state = connection) {
                ConnectionState.Disconnected -> "Desconectado"
                ConnectionState.Connecting -> "Conectando..."
                ConnectionState.Connected -> "Conectado"
                is ConnectionState.Failed -> "Error: ${state.cause.message ?: "desconocido"}"
            }
            Text("Estado de conexión: $statusLabel", color = Color.White)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (connection) {
                ConnectionState.Connected -> {
                    Button(onClick = { viewModel.stopSocket() }) {
                        Text("Cerrar socket")
                    }
                }
                ConnectionState.Disconnected, is ConnectionState.Failed -> {
                    Button(onClick = { viewModel.subscribeToSocketEvents() }) {
                        Text("Abrir socket")
                    }
                }
                ConnectionState.Connecting -> Unit
            }
        }
    }
}

private fun renderPriceLabel(ticker: BitcoinTicker?, state: ConnectionState): String {
    val price = ticker?.price
    return when {
        price != null -> "1 BTC: $price €"
        else -> "1 BTC: — €"
    }
}
