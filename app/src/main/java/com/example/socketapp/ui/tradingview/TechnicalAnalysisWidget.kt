package com.example.socketapp.ui.tradingview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val SCRIPT_TECHNICAL_ANALYSIS =
    "https://widgets.tradingview-widget.com/w/es/tv-technical-analysis.js"
private const val TECHNICAL_ANALYSIS_TEMPLATE_ASSET =
    "tradingview/technical_analysis_widget_template.html"
internal const val TECHNICAL_ANALYSIS_READY_CHECK = """
    (function() {
      if (!customElements.get('tv-technical-analysis')) return false;
      var visibleTextElementCount = 0;
      (window.__tradingViewShadowRoots || []).forEach(function(root) {
        var visibleTextElements = Array.from(root.querySelectorAll('*')).filter(function(element) {
          if (element.tagName === 'STYLE' || element.tagName === 'SCRIPT' ||
              element.childElementCount > 0) {
            return false;
          }
          var text = (element.textContent || '').trim();
          if (text.length === 0) return false;
          var bounds = element.getBoundingClientRect();
          return bounds.width > 0 && bounds.height > 0;
        });
        visibleTextElementCount += visibleTextElements.length;
      });
      return visibleTextElementCount >= 3;
    })()
"""

data class TechnicalAnalysisConfig(
    val symbol: String,
    val interval: String = "1D",
    val ratingsMode: String = "single",
    val ratingsDisplayMode: String = "gauge",
) {
    fun toHtmlAttributes(): String = buildString {
        append("symbol=\"")
        append(symbol.escapeTechnicalAnalysisAttribute())
        append("\" interval=\"")
        append(interval.escapeTechnicalAnalysisAttribute())
        append("\" ratings-mode=\"")
        append(ratingsMode.escapeTechnicalAnalysisAttribute())
        append("\" ratings-display-mode=\"")
        append(ratingsDisplayMode.escapeTechnicalAnalysisAttribute())
        append('"')
    }
}

private fun String.escapeTechnicalAnalysisAttribute(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

@Composable
fun TradingViewTechnicalAnalysisWebView(
    symbol: String,
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = TechnicalAnalysisConfig(symbol = symbol)

    TradingViewWidgetWebView(
        scriptSrc = SCRIPT_TECHNICAL_ANALYSIS,
        configJson = config.toHtmlAttributes(),
        reloadKey = reloadKey,
        onStateChange = onStateChange,
        modifier = modifier,
        templateAsset = TECHNICAL_ANALYSIS_TEMPLATE_ASSET,
        widgetReadyCheck = TECHNICAL_ANALYSIS_READY_CHECK,
    )
}
