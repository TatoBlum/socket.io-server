package com.example.socketapp.ui.tradingview

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
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
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.example.socketapp.BuildConfig

internal fun tradingViewWebViewClient(
    monitoredUrl: String,
    onPageLoaded: (WebView) -> Unit,
    onFailure: (String) -> Unit,
): WebViewClient = object : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (BuildConfig.DEBUG) Log.d("TVWebView", "Page started loading: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (BuildConfig.DEBUG) Log.d("TVWebView", "Page finished loading: $url")
        view?.let(onPageLoaded)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        if (BuildConfig.DEBUG) {
            Log.e(
                "TVWebView",
                "onReceivedError (M+): mainFrame=${request?.isForMainFrame} url=${request?.url} " +
                    "code=${error?.errorCode} desc=${error?.description}",
            )
        }
        if (
            request?.isForMainFrame == true ||
            request?.url.isTradingViewUrl() && error?.errorCode.isFatalNetworkError()
        ) {
            onFailure("No se pudo conectar con TradingView")
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (BuildConfig.DEBUG) {
            Log.e(
                "TVWebView",
                "onReceivedHttpError: url=${request?.url} status=${errorResponse?.statusCode} " +
                    "reason=${errorResponse?.reasonPhrase}",
            )
        }
        val statusCode = errorResponse?.statusCode ?: return
        if (
            request?.url?.toString() == monitoredUrl ||
            statusCode >= 500 && request?.url.isTradingViewUrl()
        ) {
            onFailure("TradingView respondió con error ($statusCode)")
        }
    }

    override fun onReceivedSslError(
        view: WebView?,
        handler: SslErrorHandler?,
        error: SslError?,
    ) {
        if (BuildConfig.DEBUG) {
            Log.e(
                "TVWebView",
                "SSL validation failed: url=${error?.url} primaryError=${error?.primaryError}",
            )
        }
        handler?.cancel()
        onFailure("No se pudo establecer una conexión segura con TradingView")
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    override fun onReceivedError(
        view: WebView?,
        errorCode: Int,
        description: String?,
        failingUrl: String?,
    ) {
        if (failingUrl == view?.url || failingUrl?.let(Uri::parse).isTradingViewUrl()) {
            onFailure("No se pudo conectar con TradingView")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
        request?.isForMainFrame == true

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION", "OverridingDeprecatedMember")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = true
}

internal fun tradingViewChromeClient(): WebChromeClient = object : WebChromeClient() {
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
                "[${message?.messageLevel()}] ${message?.message()} " +
                    "@ ${message?.sourceId()}:${message?.lineNumber()}",
            )
        }
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?,
    ): Boolean {
        result?.cancel()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?,
    ): Boolean {
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
