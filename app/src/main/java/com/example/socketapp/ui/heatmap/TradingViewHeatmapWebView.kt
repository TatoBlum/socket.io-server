package com.example.socketapp.ui.heatmap

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.example.socketapp.BuildConfig

private const val BASE_URL = "https://tradingview-heatmap.local/"

private val ALLOWED_HOSTS = listOf("tradingview.com", "tradingview-widget.com")

private fun isAllowedHost(host: String): Boolean =
    ALLOWED_HOSTS.any { host == it || host.endsWith(".$it") }

private class TimeoutHolder {
    val handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null
    fun cancel() {
        runnable?.let { handler.removeCallbacks(it) }
        runnable = null
    }
}

data class HeatmapConfig(
    val dataSource: String,
    val exchanges: List<String> = emptyList(),
    val locale: String = "en",
    val grouping: String = "sector",
    val blockSize: String = "market_cap_basic",
    val blockColor: String = "change",
    val colorTheme: String = "dark",
    val hasTopBar: Boolean = false,
    val isDataSetEnabled: Boolean = false,
    val isZoomEnabled: Boolean = true,
    val hasSymbolTooltip: Boolean = true,
    val isMonoSize: Boolean = false,
) {
    fun toJson(): String = org.json.JSONObject().apply {
        put("dataSource", dataSource)
        put("exchanges", org.json.JSONArray(exchanges))
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

enum class Market(val displayName: String, val config: HeatmapConfig) {
    MERVAL(
        displayName = "Merval",
        config = HeatmapConfig(
            dataSource = "BCBAIMV",
            exchanges = listOf("BCBA"),
            locale = "es",
        ),
    ),
    SPY(
        displayName = "SPY",
        config = HeatmapConfig(
            dataSource = "SPX500",
            locale = "en",
        ),
    ),
}

private const val CONFIG_PLACEHOLDER = "{{CONFIG}}"
private const val TEMPLATE_ASSET = "heatmap/heatmap_template.html"

@Composable
fun TradingViewHeatmapWebView(
    market: Market,
    reloadKey: Int,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val templateHtml = remember {
        context.assets.open(TEMPLATE_ASSET).bufferedReader().use { it.readText() }
    }
    val lastMarket = remember { mutableStateOf<Market?>(null) }
    val lastReloadKey = remember { mutableIntStateOf(-1) }
    val timeoutHolder = remember { TimeoutHolder() }

    AndroidView(
        factory = { ctx -> createHeatmapWebView(ctx, onLoadingChange, onError, timeoutHolder) },
        update = { webView ->
            if (lastMarket.value != market || lastReloadKey.intValue != reloadKey) {
                lastMarket.value = market
                lastReloadKey.intValue = reloadKey
                val html = templateHtml.replace(CONFIG_PLACEHOLDER, market.config.toJson())
                webView.loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { webView ->
            timeoutHolder.cancel()
            webView.destroy()
        },
        modifier = modifier,
    )
}

@SuppressLint("ClickableViewAccessibility")
private fun createHeatmapWebView(
    ctx: Context,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    timeoutHolder: TimeoutHolder,
): WebView {
    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    return WebView(ctx).apply {
        // Required by TradingView widget
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

        setBackgroundColor("#121212".toColorInt())

        // Visual-only heatmap: swallow all touches so widget-internal tile clicks
        // don't trigger navigation. Trade-off: disables TalkBack on this view.
        setOnTouchListener { _, _ -> true }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                onLoadingChange(true)
                onError(null)
                timeoutHolder.cancel()
                val r = Runnable { onLoadingChange(false) }
                timeoutHolder.runnable = r
                timeoutHolder.handler.postDelayed(r, 15_000)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                timeoutHolder.cancel()
                onLoadingChange(false)
            }

            @RequiresApi(android.os.Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                if (BuildConfig.DEBUG) {
                    Log.e(
                        "TVHeatmapWebView",
                        "onReceivedError (M+): mainFrame=${request?.isForMainFrame} url=${request?.url} code=${error?.errorCode} desc=${error?.description}",
                    )
                }
                timeoutHolder.cancel()
                if (request?.isForMainFrame == true) {
                    onError("${error?.description}")
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?,
            ) {
                if (BuildConfig.DEBUG) {
                    Log.e(
                        "TVHeatmapWebView",
                        "onReceivedHttpError: url=${request?.url} status=${errorResponse?.statusCode} reason=${errorResponse?.reasonPhrase}",
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: android.webkit.SslErrorHandler?,
                error: android.net.http.SslError?,
            ) {
                if (BuildConfig.DEBUG) Log.e("TVHeatmapWebView", "onReceivedSslError: $error")
                handler?.cancel()
                timeoutHolder.cancel()
                onError("SSL error: ${error?.primaryError}")
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

            @RequiresApi(android.os.Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host ?: return true
                if (isAllowedHost(host)) return false
                return true
            }

            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val uri = Uri.parse(url ?: return false)
                val host = uri.host ?: return true
                if (isAllowedHost(host)) return false
                return true
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "TVHeatmapWebView",
                        "[${message?.messageLevel()}] ${message?.message()} @ ${message?.sourceId()}:${message?.lineNumber()}",
                    )
                }
                return true
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                result?.cancel()
                return true
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                result?.cancel()
                return true
            }

            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?,
            ): Boolean {
                result?.cancel()
                return true
            }
        }
    }
}
