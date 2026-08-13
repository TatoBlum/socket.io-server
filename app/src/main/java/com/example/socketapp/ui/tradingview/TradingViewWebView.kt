package com.example.socketapp.ui.tradingview

import android.content.Context
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.socketapp.BuildConfig

internal const val BASE_URL = "https://tradingview-widget.local/"
internal const val CONFIG_PLACEHOLDER = "{{CONFIG}}"
internal const val SCRIPT_PLACEHOLDER = "{{SCRIPT_SRC}}"
internal const val TEMPLATE_ASSET = "tradingview/tradingview_widget_template.html"
private const val WIDGET_TIMEOUT_MS = 15_000L
private const val WIDGET_POLL_INTERVAL_MS = 250L
private const val WIDGET_IFRAME_CHECK =
    "document.querySelector('.tradingview-widget-container iframe') !== null"

sealed interface TradingViewWidgetState {
    data object Loading : TradingViewWidgetState
    data object Ready : TradingViewWidgetState
    data object Refreshing : TradingViewWidgetState
    data class Stale(val message: String) : TradingViewWidgetState
    data class Error(val message: String) : TradingViewWidgetState
}

internal fun tradingViewLoadingState(hasCachedContent: Boolean): TradingViewWidgetState =
    if (hasCachedContent) TradingViewWidgetState.Refreshing else TradingViewWidgetState.Loading

internal fun tradingViewFailureState(
    hasCachedContent: Boolean,
    message: String,
): TradingViewWidgetState = if (hasCachedContent) {
    TradingViewWidgetState.Stale(message)
} else {
    TradingViewWidgetState.Error(message)
}

internal class TimeoutHolder {
    val handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null
    private var pollRunnable: Runnable? = null

    fun schedule(delayMillis: Long, action: () -> Unit) {
        cancel()
        Runnable(action).also {
            runnable = it
            handler.postDelayed(it, delayMillis)
        }
    }

    fun schedulePoll(delayMillis: Long, action: () -> Unit) {
        pollRunnable?.let { handler.removeCallbacks(it) }
        Runnable(action).also {
            pollRunnable = it
            handler.postDelayed(it, delayMillis)
        }
    }

    fun cancel() {
        runnable?.let { handler.removeCallbacks(it) }
        pollRunnable?.let { handler.removeCallbacks(it) }
        runnable = null
        pollRunnable = null
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

internal fun createTradingViewWebView(
    ctx: Context,
    scriptSrc: String,
    onLoading: () -> Unit,
    onReady: () -> Unit,
    onFailure: (String) -> Unit,
    timeoutHolder: TimeoutHolder,
    backgroundColor: Int,
): WebView {
    var settled = false

    fun startLoading() {
        settled = false
        onLoading()
        timeoutHolder.schedule(WIDGET_TIMEOUT_MS) {
            if (!settled) {
                settled = true
                onFailure("TradingView no respondió a tiempo")
            }
        }
    }

    fun finishReady() {
        if (settled) return
        settled = true
        timeoutHolder.cancel()
        onReady()
    }

    fun finishFailure(message: String) {
        if (settled) return
        settled = true
        timeoutHolder.cancel()
        onFailure(message)
    }

    fun pollForWidget(view: WebView) {
        if (settled) return
        view.evaluateJavascript(WIDGET_IFRAME_CHECK) { result ->
            if (settled) return@evaluateJavascript
            if (result == "true") {
                finishReady()
            } else {
                timeoutHolder.schedulePoll(WIDGET_POLL_INTERVAL_MS) {
                    pollForWidget(view)
                }
            }
        }
    }

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
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = false

        setBackgroundColor(backgroundColor)

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (view != null) {
                    pollForWidget(view)
                }
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
                val requestUrl = request?.url
                if (
                    request?.isForMainFrame == true ||
                    requestUrl.isTradingViewUrl() && error?.errorCode.isFatalNetworkError()
                ) {
                    finishFailure("No se pudo conectar con TradingView")
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
                val statusCode = errorResponse?.statusCode ?: return
                if (
                    request?.url?.toString() == scriptSrc ||
                    statusCode >= 500 && request?.url.isTradingViewUrl()
                ) {
                    finishFailure("TradingView respondió con error ($statusCode)")
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                if (BuildConfig.DEBUG) Log.e("TVWebView", "onReceivedSslError: $error")
                handler?.cancel()
                finishFailure("No se pudo establecer una conexión segura con TradingView")
            }

            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?,
            ) {
                if (failingUrl == view?.url || Uri.parse(failingUrl).isTradingViewUrl()) {
                    finishFailure("No se pudo conectar con TradingView")
                }
            }

            @RequiresApi(Build.VERSION_CODES.N)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return request?.isForMainFrame == true
            }

            @Deprecated("Deprecated in Java")
            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return true
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean = false

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

        startLoading()
    }
}

private fun Uri?.isTradingViewUrl(): Boolean {
    val host = this?.host.orEmpty()
    return host == "tradingview.com" || host.endsWith(".tradingview.com") ||
        host == "tradingview-widget.com" || host.endsWith(".tradingview-widget.com")
}

private fun Int?.isFatalNetworkError(): Boolean = this in setOf(
    WebViewClient.ERROR_HOST_LOOKUP,
    WebViewClient.ERROR_CONNECT,
    WebViewClient.ERROR_TIMEOUT,
    WebViewClient.ERROR_IO,
    WebViewClient.ERROR_FAILED_SSL_HANDSHAKE,
)

@Composable
internal fun TradingViewWidgetWebView(
    scriptSrc: String,
    configJson: String,
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    var activeGeneration by remember(configJson) { mutableIntStateOf(-1) }
    var requestedGeneration by remember(configJson) { mutableIntStateOf(0) }
    var failedGeneration by remember(configJson) { mutableStateOf<Int?>(null) }
    var observedReloadKey by remember(configJson) { mutableIntStateOf(reloadKey) }

    LaunchedEffect(reloadKey) {
        if (reloadKey != observedReloadKey) {
            observedReloadKey = reloadKey
            requestedGeneration++
            failedGeneration = null
            currentOnStateChange(tradingViewLoadingState(activeGeneration >= 0))
        }
    }

    val generations = buildList {
        if (activeGeneration >= 0) add(activeGeneration)
        if (requestedGeneration != activeGeneration && failedGeneration != requestedGeneration) {
            add(requestedGeneration)
        }
    }

    Box(modifier = modifier) {
        generations.distinct().forEach { generation ->
            val isActive = generation == activeGeneration
            val isVisible = isActive || activeGeneration < 0
            key(configJson, generation) {
                TradingViewWebViewInstance(
                    scriptSrc = scriptSrc,
                    configJson = configJson,
                    onLoading = {
                        currentOnStateChange(tradingViewLoadingState(activeGeneration >= 0))
                    },
                    onReady = {
                        activeGeneration = generation
                        failedGeneration = null
                        currentOnStateChange(TradingViewWidgetState.Ready)
                    },
                    onFailure = { message ->
                        failedGeneration = generation
                        currentOnStateChange(
                            tradingViewFailureState(
                                hasCachedContent = activeGeneration >= 0,
                                message = message,
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isActive) 1f else 0f)
                        .alpha(if (isVisible) 1f else 0f),
                )
            }
        }
    }
}

@Composable
private fun TradingViewWebViewInstance(
    scriptSrc: String,
    configJson: String,
    onLoading: () -> Unit,
    onReady: () -> Unit,
    onFailure: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val templateResult = remember {
        runCatching {
            context.assets.open(TEMPLATE_ASSET).bufferedReader().use { it.readText() }
        }
    }
    val timeoutHolder = remember { TimeoutHolder() }
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val templateHtml = templateResult.getOrNull()

    if (templateHtml == null) {
        LaunchedEffect(Unit) {
            if (BuildConfig.DEBUG) {
                Log.e("TVWebView", "Missing asset: $TEMPLATE_ASSET", templateResult.exceptionOrNull())
            }
            onFailure("No se pudo inicializar TradingView")
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            createTradingViewWebView(
                ctx = ctx,
                scriptSrc = scriptSrc,
                onLoading = onLoading,
                onReady = onReady,
                onFailure = onFailure,
                timeoutHolder = timeoutHolder,
                backgroundColor = backgroundColor,
            ).apply {
                loadTradingViewWidget(this, templateHtml, scriptSrc, configJson)
            }
        },
        onRelease = { webView ->
            timeoutHolder.cancel()
            webView.destroy()
        },
        modifier = modifier,
    )
}

@Composable
internal fun <T> TradingViewTabbedWidgetWebView(
    items: List<T>,
    selected: T,
    scriptSrc: String,
    configJsonFor: (T) -> String,
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val states = remember { mutableStateMapOf<T, TradingViewWidgetState>() }
    val perItemReloadKey = remember { mutableStateMapOf<T, Int>() }
    var lastReloadKey by remember { mutableIntStateOf(reloadKey) }

    LaunchedEffect(reloadKey) {
        if (reloadKey != lastReloadKey) {
            lastReloadKey = reloadKey
            perItemReloadKey[selected] = (perItemReloadKey[selected] ?: 0) + 1
        }
    }

    val currentState = states[selected] ?: TradingViewWidgetState.Loading
    LaunchedEffect(selected, currentState) { onStateChange(currentState) }

    Box(modifier = modifier) {
        items.forEach { item ->
            val isSelected = item == selected
            key(item) {
                val configJson = remember { configJsonFor(item) }
                TradingViewWidgetWebView(
                    scriptSrc = scriptSrc,
                    configJson = configJson,
                    reloadKey = perItemReloadKey[item] ?: 0,
                    onStateChange = { states[item] = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isSelected) 1f else 0f)
                        .alpha(if (isSelected) 1f else 0f),
                )
            }
        }
    }
}
