package com.example.socketapp.ui.heatmap

import android.content.Intent
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt

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

enum class Market(val assetFileName: String, val displayName: String) {
    MERVAL("heatmap/merval_heatmap.html", "Merval"),
    SPY("heatmap/spy_heatmap.html", "SPY"),
}

@Composable
fun TradingViewHeatmapWebView(
    market: Market,
    reloadKey: Int,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastMarket = remember { mutableStateOf<Market?>(null) }
    val lastReloadKey = remember { mutableIntStateOf(-1) }
    val timeoutHolder = remember { TimeoutHolder() }

    AndroidView(
        factory = { ctx ->
            WebView.setWebContentsDebuggingEnabled(true)
            WebView(ctx).apply {
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
                        Log.e(
                            "TVHeatmapWebView",
                            "onReceivedError (M+): mainFrame=${request?.isForMainFrame} url=${request?.url} code=${error?.errorCode} desc=${error?.description}",
                        )
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
                        Log.e(
                            "TVHeatmapWebView",
                            "onReceivedHttpError: url=${request?.url} status=${errorResponse?.statusCode} reason=${errorResponse?.reasonPhrase}",
                        )
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: android.webkit.SslErrorHandler?,
                        error: android.net.http.SslError?,
                    ) {
                        Log.e("TVHeatmapWebView", "onReceivedSslError: $error")
                        super.onReceivedSslError(view, handler, error)
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
                        val scheme = uri.scheme ?: return true
                        if (scheme != "https" && scheme != "http") return true
                        Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }.also { view?.context?.startActivity(it) }
                        return true
                    }

                    @Deprecated("Deprecated in Java")
                    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        val uri = Uri.parse(url ?: return false)
                        val host = uri.host ?: return true
                        if (isAllowedHost(host)) return false
                        val scheme = uri.scheme ?: return true
                        if (scheme != "https" && scheme != "http") return true
                        Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }.also { view?.context?.startActivity(it) }
                        return true
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                        Log.d(
                            "TVHeatmapWebView",
                            "[${message?.messageLevel()}] ${message?.message()} @ ${message?.sourceId()}:${message?.lineNumber()}",
                        )
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
        },
        update = { webView ->
            if (lastMarket.value != market || lastReloadKey.intValue != reloadKey) {
                lastMarket.value = market
                lastReloadKey.intValue = reloadKey
                val html = webView.context.assets.open(market.assetFileName)
                    .bufferedReader().use { it.readText() }
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
