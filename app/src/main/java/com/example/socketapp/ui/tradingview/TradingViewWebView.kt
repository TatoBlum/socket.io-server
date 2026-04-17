package com.example.socketapp.ui.tradingview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
import androidx.core.net.toUri

internal const val BASE_URL = "https://tradingview-widget.local/"
internal const val CONFIG_PLACEHOLDER = "{{CONFIG}}"
internal const val SCRIPT_PLACEHOLDER = "{{SCRIPT_SRC}}"
internal const val TEMPLATE_ASSET = "tradingview/tradingview_widget_template.html"

private val ALLOWED_HOSTS = listOf("tradingview.com", "tradingview-widget.com")

internal fun isAllowedHost(host: String): Boolean =
    ALLOWED_HOSTS.any { host == it || host.endsWith(".$it") }

internal class TimeoutHolder {
    val handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null
    fun cancel() {
        runnable?.let { handler.removeCallbacks(it) }
        runnable = null
    }
}

internal fun loadTradingViewWidget(
    webView: WebView,
    templateHtml: String,
    scriptSrc: String,
    configJson: String,
) {
    val html = templateHtml
        .replace(CONFIG_PLACEHOLDER, configJson)
        .replace(SCRIPT_PLACEHOLDER, scriptSrc)
    webView.loadDataWithBaseURL(BASE_URL, html, "text/html", "UTF-8", null)
}

@SuppressLint("ClickableViewAccessibility")
internal fun createTradingViewWebView(
    ctx: Context,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    timeoutHolder: TimeoutHolder,
): WebView {
    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    return WebView(ctx).apply {
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

        // Visual-only: swallow touches para evitar navegación dentro del widget.
        setOnTouchListener { _, _ -> true }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
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

            @RequiresApi(Build.VERSION_CODES.M)
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                if (BuildConfig.DEBUG) {
                    Log.e(
                        "TVWebView",
                        "onReceivedError (M+): mainFrame=${request?.isForMainFrame} url=${request?.url} code=${error?.errorCode} desc=${error?.description}",
                    )
                }
                timeoutHolder.cancel()
                if (request?.isForMainFrame == true) {
                    onError(error?.description?.toString() ?: "Error desconocido")
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?,
            ) {
                if (BuildConfig.DEBUG) {
                    Log.e(
                        "TVWebView",
                        "onReceivedHttpError: url=${request?.url} status=${errorResponse?.statusCode} reason=${errorResponse?.reasonPhrase}",
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                if (BuildConfig.DEBUG) Log.e("TVWebView", "onReceivedSslError: $error")
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

            @RequiresApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host ?: return true
                if (isAllowedHost(host)) return false
                return true
            }

            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val uri = (url ?: return false).toUri()
                val host = uri.host ?: return true
                if (isAllowedHost(host)) return false
                return true
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "TVWebView",
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

@Composable
internal fun TradingViewWidgetWebView(
    scriptSrc: String,
    configJson: String,
    reloadKey: Int,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val templateHtml = remember {
        context.assets.open(TEMPLATE_ASSET).bufferedReader().use { it.readText() }
    }
    val lastConfigJson = remember { mutableStateOf<String?>(null) }
    val lastReloadKey = remember { mutableIntStateOf(-1) }
    val timeoutHolder = remember { TimeoutHolder() }

    AndroidView(
        factory = { ctx -> createTradingViewWebView(ctx, onLoadingChange, onError, timeoutHolder) },
        update = { webView ->
            if (lastConfigJson.value != configJson || lastReloadKey.intValue != reloadKey) {
                lastConfigJson.value = configJson
                lastReloadKey.intValue = reloadKey
                loadTradingViewWidget(webView, templateHtml, scriptSrc, configJson)
            }
        },
        onRelease = { webView ->
            timeoutHolder.cancel()
            webView.destroy()
        },
        modifier = modifier,
    )
}
