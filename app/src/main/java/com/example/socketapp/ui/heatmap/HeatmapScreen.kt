package com.example.socketapp.ui.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.CheckNetworkConnection

private val BackgroundColor = Color(0xFF121212)
private val IndicatorColor = Color(0xFF4CAF50)

@Composable
fun HeatmapScreen(networkConnection: CheckNetworkConnection) {
    val markets = Market.entries
    var selectedMarket by rememberSaveable { mutableStateOf(Market.MERVAL) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    val isConnected by networkConnection.observeAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        TabRow(
            selectedTabIndex = markets.indexOf(selectedMarket),
            containerColor = BackgroundColor,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[markets.indexOf(selectedMarket)]),
                    color = IndicatorColor,
                )
            },
        ) {
            markets.forEach { market ->
                Tab(
                    selected = selectedMarket == market,
                    onClick = { selectedMarket = market },
                    text = {
                        Text(
                            text = market.displayName,
                            color = Color.White,
                        )
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (!isConnected) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundColor),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "📵",
                        fontSize = 48.sp,
                    )
                    Text(
                        text = "Sin conexión",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else {
                TradingViewHeatmapWebView(
                    market = selectedMarket,
                    reloadKey = reloadTrigger,
                    onLoadingChange = { isLoading = it },
                    onError = { errorMessage = it },
                    modifier = Modifier.fillMaxSize(),
                )

                if (isLoading && errorMessage == null) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(BackgroundColor.copy(alpha = 0.8f)),
                        color = IndicatorColor,
                    )
                }

                if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BackgroundColor.copy(alpha = 0.8f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Error al cargar: $errorMessage",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        )
                        Button(
                            onClick = {
                                errorMessage = null
                                reloadTrigger++
                            },
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}
