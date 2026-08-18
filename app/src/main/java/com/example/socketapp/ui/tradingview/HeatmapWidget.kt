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
    val blockColor: String = "change|60",
    val colorTheme: String = "light",
    val hasTopBar: Boolean = false,
    val isDataSetEnabled: Boolean = false,
    val isZoomEnabled: Boolean = false,
    val hasSymbolTooltip: Boolean = false,
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

internal val SP_MERVAL_HEATMAP_CONFIG = HeatmapConfig(
    dataSource = "BCBAIMV",
    locale = "es",
)

@Composable
fun TradingViewHeatmapWebView(
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
) {
    TradingViewWidgetWebView(
        scriptSrc = SCRIPT_HEATMAP,
        configJson = SP_MERVAL_HEATMAP_CONFIG.toJson(),
        reloadKey = reloadKey,
        onStateChange = onStateChange,
        modifier = modifier,
    )
}
