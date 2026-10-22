package com.example.socketapp.ui.tradingview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.json.JSONArray
import org.json.JSONObject

private const val SCRIPT_MARKET_OVERVIEW =
    "https://widgets.tradingview-widget.com/w/es/tv-market-overview.js"
private const val MARKET_OVERVIEW_TEMPLATE_ASSET =
    "tradingview/market_overview_widget_template.html"
internal const val MARKET_OVERVIEW_READY_CHECK = """
    (function() {
      if (!customElements.get('tv-market-overview')) return false;
      var hasRenderedChart = false;
      var hasPopulatedTicker = false;
      var shadowRoots = window.__tradingViewShadowRoots || [];
      shadowRoots.forEach(function(root) {
        Array.from(root.querySelectorAll('svg')).forEach(function(svg) {
          var bounds = svg.getBoundingClientRect();
          if (bounds.width > 0 && bounds.height > 0 &&
              svg.querySelector('path[d], polyline[points]') !== null) {
            hasRenderedChart = true;
          }
        });
        if (root.host && root.host.tagName === 'TV-TICKER-ITEM') {
          var tickerText = (root.textContent || '').replace(/\s+/g, ' ').trim();
          if (tickerText.length > 0 && tickerText.indexOf('-----') === -1) {
            hasPopulatedTicker = true;
          }
        }
      });
      return hasRenderedChart || hasPopulatedTicker;
    })()
"""

data class MarketOverviewSection(
    val sectionName: String,
    val symbols: List<String>,
)

data class MarketOverviewConfig(
    val symbolSectors: List<MarketOverviewSection>,
    val exchange: String = "BCBA",
    val timeFrame: String = "1M",
    val mode: String = "market-movers",
    val hideMarketStatus: Boolean = true,
) {
    fun toJson(): String = JSONObject().apply {
        put(TRADING_VIEW_CUSTOMER_KEY, TRADING_VIEW_CUSTOMER)
        put("symbolSectors", symbolSectorsJson())
        put("exchange", exchange)
        put("timeFrame", timeFrame)
        put("mode", mode)
        put("hideMarketStatus", hideMarketStatus)
    }.toString()

    fun toHtmlAttributes(): String = buildString {
        append(TRADING_VIEW_CUSTOMER_KEY)
        append("=\"")
        append(TRADING_VIEW_CUSTOMER.escapeHtmlAttribute())
        append("\" symbol-sectors='")
        append(symbolSectorsJson().toString().escapeHtmlAttribute())
        append("' exchange=\"")
        append(exchange.escapeHtmlAttribute())
        append("\" time-frame=\"")
        append(timeFrame.escapeHtmlAttribute())
        append("\" mode=\"")
        append(mode.escapeHtmlAttribute())
        append('"')
        if (hideMarketStatus) append(" hide-market-status")
    }

    private fun symbolSectorsJson(): JSONArray = JSONArray().apply {
        symbolSectors.forEach { section ->
            put(
                JSONObject().apply {
                    put("sectionName", section.sectionName)
                    put("symbols", JSONArray(section.symbols))
                },
            )
        }
    }
}

private fun String.escapeHtmlAttribute(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

internal val SP_MERVAL_MARKET_OVERVIEW_CONFIG = MarketOverviewConfig(
    symbolSectors = listOf(
        MarketOverviewSection(
            sectionName = "S&P Merval",
            symbols = listOf("BCBA:IMV"),
        ),
    ),
)

@Composable
fun TradingViewMarketOverviewWebView(
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
) {
    TradingViewWidgetWebView(
        scriptSrc = SCRIPT_MARKET_OVERVIEW,
        configJson = SP_MERVAL_MARKET_OVERVIEW_CONFIG.toHtmlAttributes(),
        reloadKey = reloadKey,
        onStateChange = onStateChange,
        modifier = modifier,
        templateAsset = MARKET_OVERVIEW_TEMPLATE_ASSET,
        widgetReadyCheck = MARKET_OVERVIEW_READY_CHECK,
    )
}
