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
    val grouping: String = "no_group",
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

enum class Market(
    val displayName: String,
    val config: HeatmapConfig,
    val hotlistsExchange: Exchange,
) {
    SP_MERVAL(
        displayName = "S&P Merval",
        config = HeatmapConfig(
            dataSource = "BCBAIMV",
            exchanges = listOf("BCBA"),
            locale = "es",
        ),
        hotlistsExchange = Exchange.BCBA,
    ),
    SP_500(
        displayName = "S&P 500",
        config = HeatmapConfig(
            dataSource = "SPX500",
            locale = "es",
        ),
        hotlistsExchange = Exchange.US,
    ),
}

@Composable
fun TradingViewHeatmapWebView(
    selected: Market,
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
) {
    TradingViewTabbedWidgetWebView(
        items = Market.entries,
        selected = selected,
        scriptSrc = SCRIPT_HEATMAP,
        configJsonFor = { market -> market.config.toJson() },
        reloadKey = reloadKey,
        onStateChange = onStateChange,
        modifier = modifier,
    )
}
