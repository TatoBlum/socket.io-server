package com.example.socketapp.ui.tradingview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val colorTheme: String = "light",
    val hasTopBar: Boolean = false,
    val isDataSetEnabled: Boolean = false,
    val isZoomEnabled: Boolean = false,
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
    markets: List<Market>,
    selected: Market,
    reloadKey: Int,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    TradingViewTabbedWidgetWebView(
        items = markets,
        selected = selected,
        scriptSrc = SCRIPT_HEATMAP,
        configJsonFor = { it.config.toJson() },
        reloadKey = reloadKey,
        onLoadingChange = onLoadingChange,
        onError = onError,
        modifier = modifier,
    )
}
