package com.example.socketapp.ui.tradingview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.json.JSONObject

private const val SCRIPT_HOTLISTS = "https://s3.tradingview.com/external-embedding/embed-widget-hotlists.js"

data class HotlistsConfig(
    val exchange: String,
    val colorTheme: String = "light",
    val dateRange: String = "12M",
    val showChart: Boolean = true,
    val locale: String = "es",
    val largeChartUrl: String = "",
    val isTransparent: Boolean = false,
    val showSymbolLogo: Boolean = false,
    val showFloatingTooltip: Boolean = false,
    val plotLineColorGrowing: String = "rgba(41, 98, 255, 1)",
    val plotLineColorFalling: String = "rgba(41, 98, 255, 1)",
    val gridLineColor: String = "rgba(240, 243, 250, 0)",
    val scaleFontColor: String = "#5C6B7A",
    val belowLineFillColorGrowing: String = "rgba(41, 98, 255, 0.12)",
    val belowLineFillColorFalling: String = "rgba(41, 98, 255, 0.12)",
    val belowLineFillColorGrowingBottom: String = "rgba(41, 98, 255, 0)",
    val belowLineFillColorFallingBottom: String = "rgba(41, 98, 255, 0)",
    val symbolActiveColor: String = "rgba(41, 98, 255, 0.12)",
) {
    fun toJson(): String = JSONObject().apply {
        put("exchange", exchange)
        put("colorTheme", colorTheme)
        put("dateRange", dateRange)
        put("showChart", showChart)
        put("locale", locale)
        put("largeChartUrl", largeChartUrl)
        put("isTransparent", isTransparent)
        put("showSymbolLogo", showSymbolLogo)
        put("showFloatingTooltip", showFloatingTooltip)
        put("plotLineColorGrowing", plotLineColorGrowing)
        put("plotLineColorFalling", plotLineColorFalling)
        put("gridLineColor", gridLineColor)
        put("scaleFontColor", scaleFontColor)
        put("belowLineFillColorGrowing", belowLineFillColorGrowing)
        put("belowLineFillColorFalling", belowLineFillColorFalling)
        put("belowLineFillColorGrowingBottom", belowLineFillColorGrowingBottom)
        put("belowLineFillColorFallingBottom", belowLineFillColorFallingBottom)
        put("symbolActiveColor", symbolActiveColor)
        put("width", "100%")
        put("height", "100%")
    }.toString()
}

enum class Exchange(val displayName: String, val config: HotlistsConfig) {
    BCBA("BCBA", HotlistsConfig(exchange = "BCBA")),
    NASDAQ("NASDAQ", HotlistsConfig(exchange = "NASDAQ", locale = "en")),
    NYSE("NYSE", HotlistsConfig(exchange = "NYSE", locale = "en")),
}

@Composable
fun TradingViewHotlistsWebView(
    exchanges: List<Exchange>,
    selected: Exchange,
    reloadKey: Int,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    TradingViewTabbedWidgetWebView(
        items = exchanges,
        selected = selected,
        scriptSrc = SCRIPT_HOTLISTS,
        configJsonFor = { it.config.toJson() },
        reloadKey = reloadKey,
        onLoadingChange = onLoadingChange,
        onError = onError,
        modifier = modifier,
    )
}
