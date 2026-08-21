package com.example.socketapp.ui.tradingview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private const val SCRIPT_COMPANY_PROFILE =
    "https://widgets.tradingview-widget.com/w/es/tv-company-profile.js"
private const val COMPANY_PROFILE_TEMPLATE_ASSET =
    "tradingview/company_profile_widget_template.html"
internal const val COMPANY_PROFILE_READY_CHECK = """
    (function() {
      if (!customElements.get('tv-company-profile')) return false;
      var hasDescription = false;
      var populatedValueCount = 0;
      (window.__tradingViewShadowRoots || []).forEach(function(root) {
        var description = root.querySelector('.description');
        if (description !== null && (description.textContent || '').trim().length > 0) {
          hasDescription = true;
        }
        Array.from(root.querySelectorAll('.row')).forEach(function(row) {
          var value = row.querySelector('.value');
          if (value !== null && (value.textContent || '').trim().length > 0) {
            populatedValueCount += 1;
          }
        });
      });
      return hasDescription && populatedValueCount >= 2;
    })()
"""

data class CompanyProfileLocalization(
    val sector: String,
    val industry: String,
    val description: String,
)

internal val APPLE_COMPANY_PROFILE_SPANISH = CompanyProfileLocalization(
    sector = "Tecnología electrónica",
    industry = "Equipos de telecomunicaciones",
    description = """
        Apple, Inc. se dedica al diseño, la fabricación y la venta de teléfonos inteligentes,
        computadoras personales, tabletas, dispositivos portátiles y accesorios, además de otros
        servicios relacionados. Opera a través de los siguientes segmentos geográficos: América,
        Europa, Gran China, Japón y el resto de Asia-Pacífico. El segmento América incluye América
        del Norte y América del Sur. El segmento Europa comprende los países europeos, además de
        India, Medio Oriente y África. El segmento Gran China comprende China, Hong Kong y Taiwán.
        El segmento Resto de Asia-Pacífico incluye Australia y los países asiáticos. Sus productos
        y servicios incluyen iPhone, Mac, iPad, AirPods, Apple TV, Apple Watch, productos Beats,
        AppleCare, iCloud, tiendas de contenido digital, servicios de streaming y licencias. La
        empresa fue fundada por Steven Paul Jobs, Ronald Gerald Wayne y Stephen G. Wozniak en abril
        de 1976 y tiene su sede en Cupertino, California.
    """.trimIndent().replace("\n", " "),
)

data class CompanyProfileConfig(
    val symbol: String,
    val localization: CompanyProfileLocalization? = null,
) {
    fun toHtmlAttributes(): String = buildString {
        append("symbol=\"")
        append(symbol.escapeCompanyProfileAttribute())
        append('"')
        localization?.let { localized ->
            append(" data-localized-sector=\"")
            append(localized.sector.escapeCompanyProfileAttribute())
            append("\" data-localized-industry=\"")
            append(localized.industry.escapeCompanyProfileAttribute())
            append("\" data-localized-description=\"")
            append(localized.description.escapeCompanyProfileAttribute())
            append('"')
        }
    }
}

private fun String.escapeCompanyProfileAttribute(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

@Composable
fun TradingViewCompanyProfileWebView(
    symbol: String,
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
    onBoundaryScroll: (Float) -> Unit = {},
    localization: CompanyProfileLocalization? = null,
    onScrollGestureActiveChange: (Boolean) -> Unit = {},
) {
    val config = CompanyProfileConfig(
        symbol = symbol,
        localization = localization,
    )

    TradingViewWidgetWebView(
        scriptSrc = SCRIPT_COMPANY_PROFILE,
        configJson = config.toHtmlAttributes(),
        reloadKey = reloadKey,
        onStateChange = onStateChange,
        modifier = modifier,
        templateAsset = COMPANY_PROFILE_TEMPLATE_ASSET,
        widgetReadyCheck = COMPANY_PROFILE_READY_CHECK,
        handlesInternalVerticalScroll = true,
        onInternalScrollBoundary = onBoundaryScroll,
        onInternalScrollGestureActiveChange = onScrollGestureActiveChange,
    )
}
