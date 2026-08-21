package com.example.socketapp.ui.tradingview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import com.example.socketapp.BuildConfig
import java.net.URI

internal const val BASE_ORIGIN = "https://tradingview-widget.local"
internal const val BASE_URL = "$BASE_ORIGIN/"
internal const val CONFIG_PLACEHOLDER = "{{CONFIG}}"
internal const val SCRIPT_PLACEHOLDER = "{{SCRIPT_SRC}}"
internal const val TEMPLATE_ASSET = "tradingview/tradingview_widget_template.html"
private const val WIDGET_TIMEOUT_MS = 15_000L
private const val WIDGET_POLL_INTERVAL_MS = 250L

internal fun loadTradingViewWidget(
    webView: WebView,
    templateHtml: String,
    scriptSrc: String,
    configJson: String,
    baseUrl: String = BASE_URL,
) {
    val html = buildTradingViewWidgetHtml(templateHtml, scriptSrc, configJson)
    webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
}

internal fun buildTradingViewWidgetHtml(
    templateHtml: String,
    scriptSrc: String,
    configJson: String,
): String {
    val configuredHtml = templateHtml
        .replace(CONFIG_PLACEHOLDER, configJson)
        .replace(SCRIPT_PLACEHOLDER, scriptSrc)
    return if (configuredHtml.contains("</head>")) {
        configuredHtml.replaceFirst(
            "</head>",
            "$TRADING_VIEW_IFRAME_SANDBOX_BOOTSTRAP\n</head>",
        )
    } else {
        "$TRADING_VIEW_IFRAME_SANDBOX_BOOTSTRAP\n$configuredHtml"
    }
}

internal fun trustedOriginFor(baseUrl: String): String {
    val uri = URI(baseUrl)
    val scheme = requireNotNull(uri.scheme) { "TradingView base URL requires a scheme" }
    val host = requireNotNull(uri.host) { "TradingView base URL requires a host" }
    val port = if (uri.port == -1) "" else ":${uri.port}"
    return "$scheme://$host$port"
}

@SuppressLint("SetJavaScriptEnabled") // TradingView requires JS; CSP and origin checks restrict it.
internal fun createTradingViewWebView(
    ctx: Context,
    monitoredUrl: String,
    widgetReadyCheck: String,
    handlesInternalVerticalScroll: Boolean,
    onLoading: () -> Unit,
    onReady: () -> Unit,
    onFailure: (String) -> Unit,
    onInternalScrollBoundary: (Float) -> Unit,
    onInternalScrollGestureActiveChange: (Boolean) -> Unit = {},
    timeoutHolder: TimeoutHolder,
    backgroundColor: Int,
    trustedOrigin: String = BASE_ORIGIN,
): WebView {
    var settled = false
    var scrollCoordinator: TradingViewScrollCoordinator? = null

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
        if (settled || timeoutHolder.isDisposed) return
        settled = true
        timeoutHolder.cancel()
        onReady()
    }

    fun finishFailure(message: String) {
        if (settled || timeoutHolder.isDisposed) return
        settled = true
        timeoutHolder.cancel()
        onFailure(message)
    }

    fun pollForWidget(view: WebView) {
        if (settled || timeoutHolder.isDisposed) return
        view.evaluateJavascript(widgetReadyCheck) { result ->
            if (settled || timeoutHolder.isDisposed) return@evaluateJavascript
            if (result == "true") {
                scrollCoordinator?.installObserver()
                finishReady()
            } else {
                timeoutHolder.schedulePoll(WIDGET_POLL_INTERVAL_MS) {
                    pollForWidget(view)
                }
            }
        }
    }

    WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    return TradingViewNativeWebView(ctx).apply {
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
        if (handlesInternalVerticalScroll) {
            scrollCoordinator = TradingViewScrollCoordinator(
                webView = this,
                trustedOrigin = trustedOrigin,
                onBoundaryScroll = onInternalScrollBoundary,
                onGestureActiveChange = onInternalScrollGestureActiveChange,
            )
            scrollCoordinator?.attach()
        }

        webViewClient = tradingViewWebViewClient(monitoredUrl, ::pollForWidget, ::finishFailure)
        webChromeClient = tradingViewChromeClient()

        startLoading()
    }
}
