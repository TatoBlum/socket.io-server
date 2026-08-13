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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.CheckNetworkConnection
import com.example.socketapp.TradeType
import com.example.socketapp.model.StockTicker

private val HEATMAP_CARD_HEIGHT = 609.dp
private val HOTLISTS_CARD_HEIGHT = 570.dp

private data class MockHoldingTitle(
    val codeType: String,
    val codeValue: String,
    val ticker: String,
    val name: String,
    val holdingQuantity: Int,
)

private val mockHoldingTitles = listOf(
    MockHoldingTitle(
        codeType = "MOCK_SECURITY_ID",
        codeValue = "PAMP-0",
        ticker = "PAMP",
        name = "Pampa Energia",
        holdingQuantity = 125,
    ),
    MockHoldingTitle(
        codeType = "MOCK_SECURITY_ID",
        codeValue = "YPFD-0",
        ticker = "YPFD",
        name = "YPF",
        holdingQuantity = 80,
    ),
    MockHoldingTitle(
        codeType = "MOCK_SECURITY_ID",
        codeValue = "ALUA-0",
        ticker = "ALUA",
        name = "Aluar",
        holdingQuantity = 250,
    ),
)

@Composable
private fun FavoriteTickerRow(ticker: StockTicker) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = ticker.symbol,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = ticker.displayName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = ticker.price,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SimpleStepperDemo(
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(8.dp)
                    .fillMaxWidth(if (index == 0) 0.08f else 0.02f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (index == 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                    ),
            )
        }
    }
}

@Composable
fun TradingViewScreen(
    networkConnection: CheckNetworkConnection,
    favorites: List<StockTicker>,
    onTradeAction: (codeType: String, codeValue: String, tradeType: TradeType) -> Unit,
) {
    val markets = Market.entries
    var selectedMarket by rememberSaveable { mutableStateOf(Market.SP_MERVAL) }

    var heatmapState by remember { mutableStateOf<TradingViewWidgetState>(TradingViewWidgetState.Loading) }
    var heatmapReloadTrigger by remember { mutableIntStateOf(0) }

    var hotlistsState by remember { mutableStateOf<TradingViewWidgetState>(TradingViewWidgetState.Loading) }
    var hotlistsReloadTrigger by remember { mutableIntStateOf(0) }

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
                        FavoriteTickerRow(ticker = ticker)
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
            SimpleStepperDemo(totalSteps = 5)
        }

        Spacer(modifier = Modifier.height(16.dp))

        MockHoldingTitlesSection(onTradeAction = onTradeAction)
    }
}

@Composable
private fun MockHoldingTitlesSection(
    onTradeAction: (codeType: String, codeValue: String, tradeType: TradeType) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Mis titulos",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 2.dp),
        )

        mockHoldingTitles.forEach { title ->
            MockHoldingTitleCard(
                title = title,
                onBuy = { onTradeAction(title.codeType, title.codeValue, TradeType.Buy) },
                onSell = { onTradeAction(title.codeType, title.codeValue, TradeType.Sell) },
            )
        }
    }
}

@Composable
private fun MockHoldingTitleCard(
    title: MockHoldingTitle,
    onBuy: () -> Unit,
    onSell: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${title.ticker} (${title.name})",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Nominales: ${title.holdingQuantity}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onSell,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "Venta",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = onBuy,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = "Compra",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
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

        when (state) {
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
                message = "${state.message}. Mostrando los últimos datos cargados.",
                onRetry = onRetry,
            )

            is TradingViewWidgetState.Error -> WidgetBlockingMessage(
                message = state.message,
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
            .background(MaterialTheme.colorScheme.surfaceVariant),
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
