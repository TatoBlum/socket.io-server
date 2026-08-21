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
      var visibleTextElementCount = 0;
      var hasRenderedChart = false;
      (window.__tradingViewShadowRoots || []).forEach(function(root) {
        Array.from(root.querySelectorAll('svg')).forEach(function(svg) {
          var bounds = svg.getBoundingClientRect();
          if (bounds.width > 0 && bounds.height > 0 &&
              svg.querySelector('path[d], polyline[points]') !== null) {
            hasRenderedChart = true;
          }
        });
        visibleTextElementCount += Array.from(root.querySelectorAll('*')).filter(function(element) {
          if (element.tagName === 'STYLE' || element.tagName === 'SCRIPT' ||
              element.childElementCount > 0) {
            return false;
          }
          var text = (element.textContent || '').trim();
          if (text.length === 0) return false;
          var bounds = element.getBoundingClientRect();
          return bounds.width > 0 && bounds.height > 0;
        }).length;
      });
      return hasRenderedChart || visibleTextElementCount >= 2;
    })()
"""

data class MarketOverviewSection(
    val sectionName: String,
    val symbols: List<String>,
)

data class MarketOverviewConfig(
    val symbolSectors: List<MarketOverviewSection>,
    val timeFrame: String = "1D",
    val hideMarketStatus: Boolean = true,
) {
    fun toJson(): String = JSONObject().apply {
        put("symbolSectors", symbolSectorsJson())
        put("timeFrame", timeFrame)
        put("hideMarketStatus", hideMarketStatus)
    }.toString()

    fun toHtmlAttributes(): String = buildString {
        append("symbol-sectors='")
        append(symbolSectorsJson().toString().escapeHtmlAttribute())
        append("' time-frame=\"")
        append(timeFrame.escapeHtmlAttribute())
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
