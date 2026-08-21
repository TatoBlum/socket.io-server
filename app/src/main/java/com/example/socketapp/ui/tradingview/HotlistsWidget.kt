package com.example.socketapp.ui.tradingview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.json.JSONObject

private const val SCRIPT_HOTLISTS =
    "https://s3.tradingview.com/external-embedding/embed-widget-hotlists.js"

data class HotlistsConfig(
    val exchange: String,
    val colorTheme: String = "light",
    val dateRange: String = "1D",
    val showChart: Boolean = false,
    val locale: String = "es",
    val isTransparent: Boolean = false,
    val showSymbolLogo: Boolean = true,
    val showFloatingTooltip: Boolean = false,
) {
    fun toJson(): String = JSONObject().apply {
        put("exchange", exchange)
        put("colorTheme", colorTheme)
        put("dateRange", dateRange)
        put("showChart", showChart)
        put("locale", locale)
        put("largeChartUrl", "")
        put("isTransparent", isTransparent)
        put("showSymbolLogo", showSymbolLogo)
        put("showFloatingTooltip", showFloatingTooltip)
        put("width", "100%")
        put("height", "100%")
    }.toString()
}

internal val BCBA_HOTLISTS_CONFIG = HotlistsConfig(exchange = "BCBA")

@Composable
fun TradingViewHotlistsWebView(
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
) {
    TradingViewWidgetWebView(
        scriptSrc = SCRIPT_HOTLISTS,
        configJson = BCBA_HOTLISTS_CONFIG.toJson(),
        reloadKey = reloadKey,
        onStateChange = onStateChange,
        modifier = modifier,
    )
}
