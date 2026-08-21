package com.example.socketapp.ui.tradingview

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.example.socketapp.BuildConfig

private data class TradingViewWidgetIdentity(
    val scriptSrc: String,
    val configJson: String,
    val templateAsset: String,
    val widgetReadyCheck: String,
    val handlesInternalVerticalScroll: Boolean,
    val baseUrl: String,
)

@Composable
internal fun TradingViewWidgetWebView(
    scriptSrc: String,
    configJson: String,
    reloadKey: Int,
    onStateChange: (TradingViewWidgetState) -> Unit,
    modifier: Modifier = Modifier,
    templateAsset: String = TEMPLATE_ASSET,
    widgetReadyCheck: String = WIDGET_IFRAME_CHECK,
    handlesInternalVerticalScroll: Boolean = false,
    baseUrl: String = BASE_URL,
    onInternalScrollBoundary: (Float) -> Unit = {},
    onInternalScrollGestureActiveChange: (Boolean) -> Unit = {},
) {
    val currentOnStateChange by rememberUpdatedState(onStateChange)
    val currentOnInternalScrollBoundary by rememberUpdatedState(onInternalScrollBoundary)
    val currentOnInternalScrollGestureActiveChange by rememberUpdatedState(
        onInternalScrollGestureActiveChange,
    )
    val identity = remember(
        scriptSrc,
        configJson,
        templateAsset,
        widgetReadyCheck,
        handlesInternalVerticalScroll,
        baseUrl,
    ) {
        TradingViewWidgetIdentity(
            scriptSrc = scriptSrc,
            configJson = configJson,
            templateAsset = templateAsset,
            widgetReadyCheck = widgetReadyCheck,
            handlesInternalVerticalScroll = handlesInternalVerticalScroll,
            baseUrl = baseUrl,
        )
    }
    var activeGeneration by remember(identity) { mutableIntStateOf(-1) }
    var requestedGeneration by remember(identity) { mutableIntStateOf(0) }
    var failedGeneration by remember(identity) { mutableStateOf<Int?>(null) }
    var observedReloadKey by remember(identity) { mutableIntStateOf(reloadKey) }

    LaunchedEffect(identity, reloadKey) {
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
            key(identity, generation) {
                TradingViewWebViewInstance(
                    scriptSrc = scriptSrc,
                    configJson = configJson,
                    templateAsset = templateAsset,
                    widgetReadyCheck = widgetReadyCheck,
                    handlesInternalVerticalScroll = handlesInternalVerticalScroll,
                    baseUrl = baseUrl,
                    onInternalScrollBoundary = { deltaY ->
                        currentOnInternalScrollBoundary(deltaY)
                    },
                    onInternalScrollGestureActiveChange = { isActive ->
                        currentOnInternalScrollGestureActiveChange(isActive)
                    },
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
    templateAsset: String,
    widgetReadyCheck: String,
    handlesInternalVerticalScroll: Boolean,
    baseUrl: String,
    onInternalScrollBoundary: (Float) -> Unit,
    onInternalScrollGestureActiveChange: (Boolean) -> Unit,
    onLoading: () -> Unit,
    onReady: () -> Unit,
    onFailure: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentOnInternalScrollGestureActiveChange by rememberUpdatedState(
        onInternalScrollGestureActiveChange,
    )
    val templateResult = remember(templateAsset) {
        runCatching {
            context.assets.open(templateAsset).bufferedReader().use { it.readText() }
        }
    }
    val timeoutHolder = remember { TimeoutHolder() }
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val templateHtml = templateResult.getOrNull()
    if (templateHtml == null) {
        LaunchedEffect(Unit) {
            if (BuildConfig.DEBUG) {
                Log.e("TVWebView", "Missing asset: $templateAsset", templateResult.exceptionOrNull())
            }
            onFailure("No se pudo inicializar TradingView")
        }
        return
    }

    AndroidView(
        factory = { ctx ->
            createTradingViewWebView(
                ctx = ctx,
                monitoredUrl = scriptSrc,
                widgetReadyCheck = widgetReadyCheck,
                handlesInternalVerticalScroll = handlesInternalVerticalScroll,
                onLoading = onLoading,
                onReady = onReady,
                onFailure = onFailure,
                onInternalScrollBoundary = onInternalScrollBoundary,
                onInternalScrollGestureActiveChange = currentOnInternalScrollGestureActiveChange,
                timeoutHolder = timeoutHolder,
                backgroundColor = backgroundColor,
                trustedOrigin = trustedOriginFor(baseUrl),
            ).apply {
                loadTradingViewWidget(this, templateHtml, scriptSrc, configJson, baseUrl)
            }
        },
        update = { webView ->
            webView.setBackgroundColor(backgroundColor)
        },
        onRelease = { webView ->
            if (handlesInternalVerticalScroll) {
                currentOnInternalScrollGestureActiveChange(false)
            }
            timeoutHolder.dispose()
            webView.destroy()
        },
        modifier = modifier,
    )
}
