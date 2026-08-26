package com.example.socketapp.ui.tradingview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.json.JSONObject

private const val SCRIPT_ADVANCED_CHART =
    "https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js"

/** Configuration for TradingView's public Advanced Chart embed widget. */
data class AdvancedChartEmbedConfig(
    val symbol: String = "NASDAQ:AAPL",
    val interval: String = "1",
    val timezone: String = "America/Argentina/Buenos_Aires",
    val theme: String = "light",
    val locale: String = "es",
    val allowSymbolChange: Boolean = true,
    val hideSideToolbar: Boolean = true,
    val hideTopToolbar: Boolean = false,
    val hideLegend: Boolean = false,
    val hideVolume: Boolean = false,
    val saveImage: Boolean = true,
) {
    fun toJson(): String = JSONObject().apply {
        put("autosize", true)
        put("symbol", symbol)
        put("interval", interval)
        put("timezone", timezone)
        put("theme", theme)
        put("locale", locale)
        put("allow_symbol_change", allowSymbolChange)
        put("calendar", false)
        put("details", false)
        put("hide_side_toolbar", hideSideToolbar)
        put("hide_top_toolbar", hideTopToolbar)
        put("hide_legend", hideLegend)
        put("hide_volume", hideVolume)
        put("hotlist", false)
        put("save_image", saveImage)
        put("style", "1")
        put("backgroundColor", "#ffffff")
        put("gridColor", "rgba(46, 46, 46, 0.2)")
        put("width", "100%")
        put("height", "100%")
    }.toString()
}

@Composable
fun TradingViewAdvancedChartEmbedWebView(
    config: AdvancedChartEmbedConfig = AdvancedChartEmbedConfig(),
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
) {
    TradingViewWidgetWebView(
        scriptSrc = SCRIPT_ADVANCED_CHART,
        configJson = config.toJson(),
        reloadKey = reloadKey,
        onStateChange = onStateChange,
        modifier = modifier,
    )
}
