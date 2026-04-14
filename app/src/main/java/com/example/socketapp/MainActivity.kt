package com.example.socketapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socketapp.ui.ConnectionStatusBar
import com.example.socketapp.ui.CryptoTickerItem

private const val TAG = "MainScreen"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainViewModel = ViewModelProvider(this, ViewModelFactory())[MainViewModel::class.java]
        val checkNetworkConnection = CheckNetworkConnection(application)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212),
                ) {
                    MainScreen(mainViewModel, checkNetworkConnection)
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    networkConnection: CheckNetworkConnection,
) {
    val tickerMap by viewModel.tickers.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    val isConnected by networkConnection.observeAsState(initial = false)

    LaunchedEffect(isConnected) {
        if (isConnected) {
            Log.i(TAG, "Network connected — subscribing")
            viewModel.subscribeToSocketEvents()
        } else {
            Log.w(TAG, "Network disconnected — stopping")
            viewModel.stopSocket()
        }
    }

    // Convert map to sorted list (fixed order based on Constants.SYMBOLS)
    val tickerList = remember(tickerMap) {
        tickerMap.values
            .sortedBy { ticker ->
                Constants.SYMBOLS.indexOf(ticker.symbol.lowercase())
                    .let { if (it == -1) Int.MAX_VALUE else it }
            }
            .toList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Connection status bar at top
        ConnectionStatusBar(
            connectionState = connection,
            onConnect = { viewModel.subscribeToSocketEvents() },
            onDisconnect = { viewModel.stopSocket() },
        )

        HorizontalDivider(color = Color(0xFF333333))

        if (tickerList.isEmpty() && connection == ConnectionState.Connected) {
            // Loading state
            Text(
                text = "Cargando precios...",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.CenterHorizontally),
            )
        }

        // Ticker list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        ) {
            items(
                items = tickerList,
                key = { it.symbol },
            ) { ticker ->
                CryptoTickerItem(ticker = ticker)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
