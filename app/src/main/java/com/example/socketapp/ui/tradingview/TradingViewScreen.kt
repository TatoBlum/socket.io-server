package com.example.socketapp.ui.tradingview

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.CheckNetworkConnection
import com.example.socketapp.model.StockTicker
import com.example.socketapp.ui.ExpandingDotStepperDemo
import com.example.socketapp.ui.stocks.StockTickerItem
import com.example.socketapp.ui.theme.CardSurface
import com.example.socketapp.ui.theme.SegmentedTrack

private val HEATMAP_CARD_HEIGHT = 609.dp
private val HOTLISTS_CARD_HEIGHT = 570.dp

@Composable
fun TradingViewScreen(
    networkConnection: CheckNetworkConnection,
    favorites: List<StockTicker>,
    onOpenTitles: () -> Unit,
) {
    val markets = Market.entries
    var selectedMarket by rememberSaveable { mutableStateOf(Market.SP_MERVAL) }

    var heatmapState by remember { mutableStateOf<TradingViewWidgetState>(TradingViewWidgetState.Loading) }
    var heatmapReloadTrigger by remember { mutableIntStateOf(0) }

    var hotlistsState by remember { mutableStateOf<TradingViewWidgetState>(TradingViewWidgetState.Loading) }
    var hotlistsReloadTrigger by remember { mutableIntStateOf(0) }

    val isConnected by networkConnection.observeAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        if (favorites.isNotEmpty()) {
            WidgetCard(title = "Favoritos") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    favorites.forEachIndexed { index, ticker ->
                        StockTickerItem(ticker = ticker)
                        if (index < favorites.lastIndex) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetCard(
            title = "Mapa de calor",
        ) {
            TabSelector(
                items = markets,
                selected = selectedMarket,
                displayName = { it.displayName },
                onSelected = { selectedMarket = it },
            )

            WidgetBox(
                height = HEATMAP_CARD_HEIGHT,
                isConnected = isConnected,
                state = heatmapState,
                onRetry = { heatmapReloadTrigger++ },
            ) {
                TradingViewHeatmapWebView(
                    selected = selectedMarket,
                    reloadKey = heatmapReloadTrigger,
                    onStateChange = { heatmapState = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        WidgetCard(
            title = "Movimientos · ${selectedMarket.displayName}",
        ) {
            WidgetBox(
                height = HOTLISTS_CARD_HEIGHT,
                isConnected = isConnected,
                state = hotlistsState,
                onRetry = { hotlistsReloadTrigger++ },
            ) {
                TradingViewHotlistsWebView(
                    exchanges = Exchange.entries,
                    selected = selectedMarket.hotlistsExchange,
                    reloadKey = hotlistsReloadTrigger,
                    onStateChange = { hotlistsState = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        WidgetCard(title = "Stepper (demo)") {
            ExpandingDotStepperDemo(totalSteps = 5)
            Button(
                onClick = onOpenTitles,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            ) {
                Text("Ver títulos")
            }
        }
    }
}

@Composable
private fun WidgetCard(
    title: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurface),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
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
    state: TradingViewWidgetState,
    onRetry: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        content()

        val displayState = tradingViewDisplayState(state, isConnected)

        when (displayState) {
            TradingViewWidgetState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            TradingViewWidgetState.Refreshing -> WidgetStatusBanner(
                message = "Actualizando datos…",
                showProgress = true,
            )

            is TradingViewWidgetState.Stale -> WidgetStatusBanner(
                message = "${displayState.message}. Mostrando los últimos datos cargados.",
                onRetry = onRetry,
            )

            is TradingViewWidgetState.Error -> WidgetBlockingMessage(
                message = displayState.message,
                onRetry = onRetry,
            )

            TradingViewWidgetState.Ready -> Unit
        }
    }
}

@Composable
private fun BoxScope.WidgetStatusBanner(
    message: String,
    showProgress: Boolean = false,
    onRetry: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun BoxScope.WidgetBlockingMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "⚠️", fontSize = 48.sp)
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        if (onRetry != null) {
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun <T> TabSelector(
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
        label = "tabIndicator",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SegmentedTrack),
    ) {
        Box(
            modifier = Modifier
                .align(BiasAlignment(animatedBias, 0f))
                .fillMaxWidth(1f / items.size)
                .fillMaxHeight()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)),
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
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
