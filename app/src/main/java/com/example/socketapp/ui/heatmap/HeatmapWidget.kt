package com.example.socketapp.ui.heatmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

private const val SCRIPT_HEATMAP = "https://s3.tradingview.com/external-embedding/embed-widget-stock-heatmap.js"

data class HeatmapConfig(
    val dataSource: String,
    val exchanges: List<String> = emptyList(),
    val locale: String = "en",
    val grouping: String = "sector",
    val blockSize: String = "market_cap_basic",
    val blockColor: String = "change",
    val colorTheme: String = "dark",
    val hasTopBar: Boolean = false,
    val isDataSetEnabled: Boolean = false,
    val isZoomEnabled: Boolean = true,
    val hasSymbolTooltip: Boolean = true,
    val isMonoSize: Boolean = false,
) {
    fun toJson(): String = JSONObject().apply {
        put("dataSource", dataSource)
        put("exchanges", JSONArray(exchanges))
        put("locale", locale)
        put("grouping", grouping)
        put("blockSize", blockSize)
        put("blockColor", blockColor)
        put("colorTheme", colorTheme)
        put("hasTopBar", hasTopBar)
        put("isDataSetEnabled", isDataSetEnabled)
        put("isZoomEnabled", isZoomEnabled)
        put("hasSymbolTooltip", hasSymbolTooltip)
        put("isMonoSize", isMonoSize)
        put("symbolUrl", "")
        put("width", "100%")
        put("height", "100%")
    }.toString()
}

enum class Market(val displayName: String, val config: HeatmapConfig) {
    MERVAL(
        displayName = "Merval",
        config = HeatmapConfig(
            dataSource = "BCBAIMV",
            exchanges = listOf("BCBA"),
            locale = "es",
        ),
    ),
    SPY(
        displayName = "SPY",
        config = HeatmapConfig(
            dataSource = "SPX500",
            locale = "en",
        ),
    ),
}

@Composable
fun TradingViewHeatmapWebView(
    market: Market,
    reloadKey: Int,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val templateHtml = remember {
        context.assets.open(TEMPLATE_ASSET).bufferedReader().use { it.readText() }
    }
    val lastMarket = remember { mutableStateOf<Market?>(null) }
    val lastReloadKey = remember { mutableIntStateOf(-1) }
    val timeoutHolder = remember { TimeoutHolder() }

    AndroidView(
        factory = { ctx -> createTradingViewWebView(ctx, onLoadingChange, onError, timeoutHolder) },
        update = { webView ->
            if (lastMarket.value != market || lastReloadKey.intValue != reloadKey) {
                lastMarket.value = market
                lastReloadKey.intValue = reloadKey
                loadTradingViewWidget(webView, templateHtml, SCRIPT_HEATMAP, market.config.toJson())
            }
        },
        onRelease = { webView ->
            timeoutHolder.cancel()
            webView.destroy()
        },
        modifier = modifier,
    )
}
