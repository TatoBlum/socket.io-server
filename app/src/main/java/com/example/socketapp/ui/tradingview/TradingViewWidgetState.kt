package com.example.socketapp.ui.tradingview

import android.os.Handler
import android.os.Looper

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
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var pollRunnable: Runnable? = null
    var isDisposed: Boolean = false
        private set

    fun schedule(delayMillis: Long, action: () -> Unit) {
        if (isDisposed) return
        cancel()
        Runnable(action).also {
            runnable = it
            handler.postDelayed(it, delayMillis)
        }
    }

    fun schedulePoll(delayMillis: Long, action: () -> Unit) {
        if (isDisposed) return
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

    fun dispose() {
        cancel()
        isDisposed = true
    }
}
