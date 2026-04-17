package com.example.socketapp.ui.heatmap

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.CheckNetworkConnection

@Composable
fun HeatmapScreen(networkConnection: CheckNetworkConnection) {
    val markets = Market.entries
    val exchanges = Exchange.entries
    var selectedMarket by rememberSaveable { mutableStateOf(Market.MERVAL) }
    var selectedExchange by rememberSaveable { mutableStateOf(Exchange.BCBA) }

    var isHeatmapLoading by remember { mutableStateOf(false) }
    var heatmapError by remember { mutableStateOf<String?>(null) }
    var heatmapReloadTrigger by remember { mutableIntStateOf(0) }

    var isHotlistsLoading by remember { mutableStateOf(false) }
    var hotlistsError by remember { mutableStateOf<String?>(null) }
    var hotlistsReloadTrigger by remember { mutableIntStateOf(0) }

    val isConnected by networkConnection.observeAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        WidgetCard(
            title = "Mapa de calor",
            widgetHeight = 609.dp,
        ) {
            SegmentedControl(
                items = markets,
                selected = selectedMarket,
                displayName = { it.displayName },
                onSelected = { selectedMarket = it },
            )

            WidgetBox(
                height = 609.dp,
                isConnected = isConnected,
                isLoading = isHeatmapLoading,
                errorMessage = heatmapError,
                onRetry = {
                    heatmapError = null
                    heatmapReloadTrigger++
                },
            ) {
                TradingViewHeatmapWebView(
                    market = selectedMarket,
                    reloadKey = heatmapReloadTrigger,
                    onLoadingChange = { isHeatmapLoading = it },
                    onError = { heatmapError = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        WidgetCard(
            title = null,
            widgetHeight = 570.dp,
        ) {
            SegmentedControl(
                items = exchanges,
                selected = selectedExchange,
                displayName = { it.displayName },
                onSelected = { selectedExchange = it },
            )

            WidgetBox(
                height = 570.dp,
                isConnected = isConnected,
                isLoading = isHotlistsLoading,
                errorMessage = hotlistsError,
                onRetry = {
                    hotlistsError = null
                    hotlistsReloadTrigger++
                },
            ) {
                TradingViewHotlistsWebView(
                    exchange = selectedExchange,
                    reloadKey = hotlistsReloadTrigger,
                    onLoadingChange = { isHotlistsLoading = it },
                    onError = { hotlistsError = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun WidgetCard(
    title: String?,
    widgetHeight: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    color = SegmentedTextColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
private fun WidgetBox(
    height: Dp,
    isConnected: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
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
                Text(text = "📵", fontSize = 48.sp)
                Text(
                    text = "Sin conexión",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        } else {
            content()

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
                    Button(onClick = onRetry) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SegmentedControl(
    items: List<T>,
    selected: T,
    displayName: (T) -> String,
    onSelected: (T) -> Unit,
) {
    val selectedIdx = items.indexOf(selected).coerceAtLeast(0)
    val targetBias = if (items.size <= 1) 0f else (2f * selectedIdx / (items.size - 1)) - 1f
    val animatedBias by animateFloatAsState(
        targetValue = targetBias,
        animationSpec = tween(durationMillis = 220),
        label = "segmentIndicator",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SegmentedTrackColor),
    ) {
        Box(
            modifier = Modifier
                .align(BiasAlignment(animatedBias, 0f))
                .fillMaxWidth(1f / items.size)
                .fillMaxHeight()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp))
                .background(SegmentedSelectedColor, RoundedCornerShape(10.dp)),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEach { item ->
                val isSelected = selected == item
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelected(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = displayName(item),
                        color = SegmentedTextColor,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
