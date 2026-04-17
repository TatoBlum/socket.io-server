package com.example.socketapp.ui.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
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
private val CardBackgroundColor = Color(0xFF1E1E1E)
private val IndicatorColor = Color(0xFF4CAF50)
private val SegmentedTrackColor = Color(0xFFE5E5EA)
private val SegmentedSelectedColor = Color.White
private val SegmentedTextColor = Color(0xFF1C1C1E)

@Composable
fun HeatmapScreen(networkConnection: CheckNetworkConnection) {
    val markets = Market.entries
    var selectedMarket by rememberSaveable { mutableStateOf(Market.MERVAL) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    val isConnected by networkConnection.observeAsState(initial = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.LightGray),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)) {
                Text(
                    text = "Mapa de calor",
                    color = SegmentedTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SegmentedTrackColor)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    markets.forEach { market ->
                        val selected = selectedMarket == market
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(
                                    if (selected) {
                                        Modifier.shadow(
                                            elevation = 2.dp,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                    } else {
                                        Modifier
                                    },
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) SegmentedSelectedColor else Color.Transparent,
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { selectedMarket = market },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = market.displayName,
                                color = SegmentedTextColor,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (!isConnected) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CardBackgroundColor),
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
                                    .background(CardBackgroundColor.copy(alpha = 0.8f)),
                                color = IndicatorColor,
                            )
                        }

                        if (errorMessage != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(CardBackgroundColor.copy(alpha = 0.8f)),
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
    }
}
