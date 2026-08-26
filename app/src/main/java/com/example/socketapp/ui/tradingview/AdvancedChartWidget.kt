package com.example.socketapp.ui.tradingview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.socketapp.BuildConfig
import org.json.JSONObject
import java.io.FileNotFoundException

private const val ADVANCED_CHART_BASE_URL = "https://tradingview-charting-library.local/"
private const val ADVANCED_CHART_TEMPLATE_ASSET = "tradingview/charting_library_mock_template.html"
private const val ADVANCED_CHART_LIBRARY_ASSET = "tradingview/charting_library/charting_library.js"
private const val ADVANCED_CHART_CONFIG_PLACEHOLDER = "{{CONFIG}}"

data class AdvancedChartConfig(
    val symbol: String,
    val description: String,
    val exchange: String,
    val basePrice: Double,
    val seed: Int,
    val currencyCode: String,
    val locale: String = "es",
    val interval: String = "1D",
    val session: String = "1030-1655",
    val timezone: String = "America/Argentina/Buenos_Aires",
    val theme: String = "light",
    val pricescale: Int = 100,
    val pollingMs: Int = 5_000,
    val realtimeStep: Double = 8.0,
) {
    fun toJson(): String = JSONObject().apply {
        put("symbol", symbol)
        put("description", description)
        put("exchange", exchange)
        put("basePrice", basePrice)
        put("seed", seed)
        put("currencyCode", currencyCode)
        put("locale", locale)
        put("interval", interval)
        put("session", session)
        put("timezone", timezone)
        put("theme", theme)
        put("pricescale", pricescale)
        put("pollingMs", pollingMs)
        put("realtimeStep", realtimeStep)
    }.toString()
}

enum class MarketChart(val displayName: String, val config: AdvancedChartConfig) {
    MERVAL(
        displayName = "Merval",
        config = AdvancedChartConfig(
            symbol = "BCBA:IMV",
            description = "Indice Merval",
            exchange = "BCBA",
            basePrice = 1_550_000.0,
            seed = 7,
            currencyCode = "ARS",
            realtimeStep = 1_250.0,
        ),
    ),
    SPY(
        displayName = "SPY",
        config = AdvancedChartConfig(
            symbol = "AMEX:SPY",
            description = "SPDR S&P 500 ETF Trust",
            exchange = "AMEX",
            basePrice = 530.0,
            seed = 17,
            currencyCode = "USD",
            session = "0930-1600",
            timezone = "America/New_York",
            realtimeStep = 0.35,
        ),
    ),
}

enum class HotlistChart(val displayName: String, val config: AdvancedChartConfig) {
    BCBA(
        displayName = "BCBA",
        config = AdvancedChartConfig(
            symbol = "BCBA:GGAL",
            description = "Grupo Financiero Galicia",
            exchange = "BCBA",
            basePrice = 5_420.0,
            seed = 31,
            currencyCode = "ARS",
            realtimeStep = 14.0,
        ),
    ),
    NASDAQ(
        displayName = "NASDAQ",
        config = AdvancedChartConfig(
            symbol = "NASDAQ:NVDA",
            description = "NVIDIA Corporation",
            exchange = "NASDAQ",
            basePrice = 118.0,
            seed = 47,
            currencyCode = "USD",
            session = "0930-1600",
            timezone = "America/New_York",
            realtimeStep = 0.22,
        ),
    ),
    NYSE(
        displayName = "NYSE",
        config = AdvancedChartConfig(
            symbol = "NYSE:JPM",
            description = "JPMorgan Chase & Co.",
            exchange = "NYSE",
            basePrice = 210.0,
            seed = 59,
            currencyCode = "USD",
            session = "0930-1600",
            timezone = "America/New_York",
            realtimeStep = 0.28,
        ),
    ),
}

@Composable
fun TradingViewAdvancedChartWebView(
    config: AdvancedChartConfig,
    reloadKey: Int,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnLoadingChange = rememberUpdatedState(onLoadingChange)
    val currentOnError = rememberUpdatedState(onError)
    val templateHtml = remember {
        context.assets.open(ADVANCED_CHART_TEMPLATE_ASSET).bufferedReader().use { it.readText() }
    }
    val hasChartingLibrary = remember {
        context.hasAsset(ADVANCED_CHART_LIBRARY_ASSET)
    }
    val lastConfigJson = remember { mutableStateOf<String?>(null) }
    val lastReloadKey = remember { mutableIntStateOf(-1) }
    val timeoutHolder = remember { TimeoutHolder() }
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()

    AndroidView(
        factory = { ctx ->
            createAdvancedChartWebView(
                context = ctx,
                onLoadingChange = { isLoading -> currentOnLoadingChange.value(isLoading) },
                onError = { message -> currentOnError.value(message) },
                timeoutHolder = timeoutHolder,
                backgroundColor = backgroundColor,
            )
        },
        update = { webView ->
            webView.setBackgroundColor(backgroundColor)
            val configJson = config.toJson()
            if (lastConfigJson.value != configJson || lastReloadKey.intValue != reloadKey) {
                lastConfigJson.value = configJson
                lastReloadKey.intValue = reloadKey
                val html = if (hasChartingLibrary) {
                    templateHtml.replace(ADVANCED_CHART_CONFIG_PLACEHOLDER, configJson)
                } else {
                    missingChartingLibraryHtml(config)
                }
                webView.loadDataWithBaseURL(ADVANCED_CHART_BASE_URL, html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { webView ->
            timeoutHolder.dispose()
            webView.destroy()
        },
        modifier = modifier,
    )
}

private fun Context.hasAsset(path: String): Boolean =
    try {
        assets.open(path).close()
        true
    } catch (_: FileNotFoundException) {
        false
    }

private fun missingChartingLibraryHtml(config: AdvancedChartConfig): String =
    """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <style>
        html, body {
          width: 100%;
          height: 100%;
          margin: 0;
          background: #f8fafc;
          color: #1f2937;
          font-family: sans-serif;
        }
        .container {
          box-sizing: border-box;
          height: 100%;
          padding: 24px;
          display: flex;
          flex-direction: column;
          gap: 12px;
          line-height: 1.35;
        }
        .header {
          display: flex;
          justify-content: space-between;
          gap: 12px;
          align-items: flex-start;
        }
        strong {
          display: block;
          font-size: 18px;
        }
        .subtitle {
          color: #64748b;
          font-size: 13px;
        }
        .price {
          text-align: right;
          white-space: nowrap;
        }
        .price strong {
          font-size: 20px;
        }
        code {
          color: #0f766e;
          word-break: break-word;
        }
        canvas {
          width: 100%;
          height: 100%;
          min-height: 260px;
          flex: 1;
          border: 1px solid #e2e8f0;
          border-radius: 12px;
          background: #ffffff;
        }
        .note {
          color: #64748b;
          font-size: 12px;
        }
      </style>
    </head>
    <body>
      <div class="container">
        <div class="header">
          <div>
            <strong>${config.symbol}</strong>
            <div class="subtitle">${config.description}</div>
          </div>
          <div class="price">
            <strong id="last-price"></strong>
            <div class="subtitle">${config.currencyCode}</div>
          </div>
        </div>
        <canvas id="mock-chart"></canvas>
        <div class="note">
          Vista mock local. Para usar TradingView Advanced Charts real, copiá:
          <code>app/src/main/assets/tradingview/charting_library/</code>
        </div>
      </div>
      <script>
        var config = ${config.toJson()};
        var bars = buildMockBars(config.basePrice, config.seed);
        var canvas = document.getElementById("mock-chart");
        var price = document.getElementById("last-price");

        function draw() {
          var scale = window.devicePixelRatio || 1;
          var rect = canvas.getBoundingClientRect();
          canvas.width = Math.max(1, Math.floor(rect.width * scale));
          canvas.height = Math.max(1, Math.floor(rect.height * scale));

          var ctx = canvas.getContext("2d");
          ctx.scale(scale, scale);
          ctx.clearRect(0, 0, rect.width, rect.height);

          var padding = { left: 14, top: 16, right: 56, bottom: 28 };
          var plotWidth = rect.width - padding.left - padding.right;
          var plotHeight = rect.height - padding.top - padding.bottom;
          var visibleBars = bars.slice(-70);
          var highs = visibleBars.map(function (bar) { return bar.high; });
          var lows = visibleBars.map(function (bar) { return bar.low; });
          var max = Math.max.apply(null, highs);
          var min = Math.min.apply(null, lows);
          var range = Math.max(1, max - min);
          var barWidth = plotWidth / visibleBars.length;

          ctx.strokeStyle = "#e2e8f0";
          ctx.lineWidth = 1;
          ctx.font = "11px sans-serif";
          ctx.fillStyle = "#64748b";

          for (var grid = 0; grid <= 4; grid++) {
            var y = padding.top + plotHeight * grid / 4;
            ctx.beginPath();
            ctx.moveTo(padding.left, y);
            ctx.lineTo(rect.width - padding.right, y);
            ctx.stroke();

            var label = max - range * grid / 4;
            ctx.fillText(formatPrice(label), rect.width - padding.right + 8, y + 4);
          }

          visibleBars.forEach(function (bar, index) {
            var x = padding.left + index * barWidth + barWidth / 2;
            var openY = yFor(bar.open);
            var closeY = yFor(bar.close);
            var highY = yFor(bar.high);
            var lowY = yFor(bar.low);
            var growing = bar.close >= bar.open;

            ctx.strokeStyle = growing ? "#16a34a" : "#dc2626";
            ctx.fillStyle = growing ? "#16a34a" : "#dc2626";

            ctx.beginPath();
            ctx.moveTo(x, highY);
            ctx.lineTo(x, lowY);
            ctx.stroke();

            var bodyTop = Math.min(openY, closeY);
            var bodyHeight = Math.max(2, Math.abs(closeY - openY));
            ctx.fillRect(
              x - Math.max(2, barWidth * 0.28),
              bodyTop,
              Math.max(4, barWidth * 0.56),
              bodyHeight
            );
          });

          var last = visibleBars[visibleBars.length - 1];
          price.textContent = formatPrice(last.close);

          function yFor(value) {
            return padding.top + (max - value) / range * plotHeight;
          }
        }

        function tick() {
          var last = bars[bars.length - 1];
          var nextClose = round(last.close + pseudoRandom(config.seed + bars.length + Date.now()) * config.realtimeStep);
          var next = {
            time: Date.now(),
            open: last.close,
            high: Math.max(last.close, nextClose) + Math.abs(config.realtimeStep),
            low: Math.min(last.close, nextClose) - Math.abs(config.realtimeStep),
            close: nextClose,
            volume: last.volume + 250
          };

          bars.push(next);
          draw();
        }

        function buildMockBars(basePrice, seed) {
          var result = [];
          var dayMs = 24 * 60 * 60 * 1000;
          var now = Date.now();
          var firstDay = now - dayMs * 180;
          var previousClose = basePrice;

          for (var i = 0; i < 180; i++) {
            var drift = pseudoRandom(seed + i) * basePrice * 0.018;
            var open = previousClose;
            var close = Math.max(1, open + drift);
            var high = Math.max(open, close) + Math.abs(drift) * 0.45;
            var low = Math.max(1, Math.min(open, close) - Math.abs(drift) * 0.45);

            result.push({
              time: firstDay + i * dayMs,
              open: round(open),
              high: round(high),
              low: round(low),
              close: round(close),
              volume: 100000 + i * 1500
            });

            previousClose = close;
          }

          return result;
        }

        function pseudoRandom(value) {
          var x = Math.sin(value) * 10000;
          return (x - Math.floor(x) - 0.5) * 2;
        }

        function round(value) {
          return Math.round(value * 100) / 100;
        }

        function formatPrice(value) {
          return value.toLocaleString("es-AR", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
          });
        }

        draw();
        window.addEventListener("resize", draw);
        setInterval(tick, config.pollingMs);
      </script>
    </body>
    </html>
    """.trimIndent()

@SuppressLint("SetJavaScriptEnabled") // The local chart library requires JavaScript.
private fun createAdvancedChartWebView(
    context: Context,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    timeoutHolder: TimeoutHolder,
    backgroundColor: Int,
): WebView {
    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    return WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        setBackgroundColor(backgroundColor)

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? = request?.url?.let { url ->
                interceptChartingLibraryAsset(context, url)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                onLoadingChange(true)
                onError(null)
                timeoutHolder.schedule(15_000) { onLoadingChange(false) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                timeoutHolder.cancel()
                onLoadingChange(false)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                timeoutHolder.cancel()
                if (request?.isForMainFrame == true) {
                    onError(error?.description?.toString() ?: "Error desconocido")
                }
            }

            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?,
            ) {
                timeoutHolder.cancel()
                if (failingUrl == view?.url) {
                    onError(description ?: "Error desconocido")
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                handler?.cancel()
                timeoutHolder.cancel()
                onError("SSL error: ${error?.primaryError}")
            }
        }
    }
}

private fun interceptChartingLibraryAsset(
    context: Context,
    url: Uri,
): WebResourceResponse? {
    if (url.host != Uri.parse(ADVANCED_CHART_BASE_URL).host) return null

    val path = url.path?.removePrefix("/") ?: return null
    if (!path.startsWith("charting_library/")) return null

    return try {
        WebResourceResponse(
            mimeTypeFor(path),
            "UTF-8",
            context.assets.open("tradingview/$path"),
        )
    } catch (_: FileNotFoundException) {
        null
    }
}

private fun mimeTypeFor(path: String): String =
    when {
        path.endsWith(".js") -> "application/javascript"
        path.endsWith(".css") -> "text/css"
        path.endsWith(".html") -> "text/html"
        path.endsWith(".json") -> "application/json"
        path.endsWith(".png") -> "image/png"
        path.endsWith(".svg") -> "image/svg+xml"
        path.endsWith(".woff2") -> "font/woff2"
        else -> "application/octet-stream"
    }
