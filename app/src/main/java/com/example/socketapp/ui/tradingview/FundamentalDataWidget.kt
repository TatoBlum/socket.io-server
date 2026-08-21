package com.example.socketapp.ui.tradingview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.json.JSONObject

private const val SCRIPT_FUNDAMENTAL_DATA =
    "https://s3.tradingview.com/external-embedding/embed-widget-financials.js"

data class FundamentalDataConfig(
    val symbol: String,
    val colorTheme: String = "light",
    val displayMode: String = "regular",
    val locale: String = "es",
) {
    fun toJson(): String = JSONObject().apply {
        put("symbol", symbol)
        put("colorTheme", colorTheme)
        put("displayMode", displayMode)
        put("locale", locale)
        put("width", "100%")
        put("height", "100%")
    }.toString()
}

@Composable
fun TradingViewFundamentalDataWebView(
    symbol: String,
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
    onBoundaryScroll: (Float) -> Unit = {},
    onScrollGestureActiveChange: (Boolean) -> Unit = {},
) {
    val config = FundamentalDataConfig(symbol = symbol)

    TradingViewWidgetWebView(
        scriptSrc = SCRIPT_FUNDAMENTAL_DATA,
        configJson = config.toJson(),
        reloadKey = reloadKey,
        onStateChange = onStateChange,
        modifier = modifier,
        handlesInternalVerticalScroll = true,
        onInternalScrollBoundary = onBoundaryScroll,
        onInternalScrollGestureActiveChange = onScrollGestureActiveChange,
    )
}
