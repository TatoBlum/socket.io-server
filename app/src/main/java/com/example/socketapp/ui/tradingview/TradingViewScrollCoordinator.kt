package com.example.socketapp.ui.tradingview

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlin.math.abs
import kotlin.math.max

private const val INTERNAL_SCROLL_POLL_INTERVAL_MS = 50L
private const val INTERNAL_SCROLL_MESSAGE_FRESHNESS_MS = 2_500L
private const val INTERNAL_SCROLL_BRIDGE_NAME = "tradingViewScrollBounds"

internal fun shouldPollInternalScrollBounds(
    nowMillis: Long,
    lastBridgeMessageMillis: Long?,
): Boolean = lastBridgeMessageMillis == null ||
    nowMillis - lastBridgeMessageMillis > INTERNAL_SCROLL_MESSAGE_FRESHNESS_MS

internal fun resolveVerticalScrollGesture(
    totalDeltaX: Float,
    totalDeltaY: Float,
    touchSlop: Float,
): Boolean? {
    if (max(abs(totalDeltaX), abs(totalDeltaY)) < touchSlop) return null
    return abs(totalDeltaY) > abs(totalDeltaX)
}

internal fun isCurrentScrollBoundsPoll(
    requestId: Long,
    activeRequestId: Long?,
    boundsRevisionAtRequest: Long,
    currentBoundsRevision: Long,
): Boolean = requestId == activeRequestId && boundsRevisionAtRequest == currentBoundsRevision

internal fun pointerRawCoordinate(
    primaryPointerRaw: Float,
    primaryPointerLocal: Float,
    pointerLocal: Float,
): Float = primaryPointerRaw + pointerLocal - primaryPointerLocal

internal fun shouldHandOffVerticalScroll(
    deltaY: Float,
    boundsKnown: Boolean,
    atTop: Boolean,
    atBottom: Boolean,
    nativeClampedInDirection: Boolean,
): Boolean {
    if (!boundsKnown) return nativeClampedInDirection
    return atTop && deltaY > 0f || atBottom && deltaY < 0f
}

internal class TradingViewNativeWebView(context: Context) : WebView(context) {
    private var currentVerticalTouchDirection = 0
    private var clampedVerticalTouchDirection = 0

    fun beginVerticalGesture() {
        currentVerticalTouchDirection = 0
        clampedVerticalTouchDirection = 0
    }

    fun recordVerticalTouchDelta(deltaY: Float) {
        currentVerticalTouchDirection = deltaY.direction()
    }

    fun isClampedInDirection(deltaY: Float): Boolean =
        clampedVerticalTouchDirection != 0 &&
            clampedVerticalTouchDirection == deltaY.direction()

    fun endVerticalGesture() {
        currentVerticalTouchDirection = 0
        clampedVerticalTouchDirection = 0
    }

    override fun onOverScrolled(
        scrollX: Int,
        scrollY: Int,
        clampedX: Boolean,
        clampedY: Boolean,
    ) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        clampedVerticalTouchDirection = if (clampedY) currentVerticalTouchDirection else 0
    }

    private fun Float.direction(): Int = when {
        this > 0f -> 1
        this < 0f -> -1
        else -> 0
    }
}

internal class TradingViewScrollCoordinator(
    private val webView: TradingViewNativeWebView,
    private val trustedOrigin: String,
    private val onBoundaryScroll: (Float) -> Unit,
    private val onGestureActiveChange: (Boolean) -> Unit,
) {
    private val touchSlop = ViewConfiguration.get(webView.context).scaledTouchSlop.toFloat()

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var downTouchRawX = 0f
    private var downTouchRawY = 0f
    private var lastTouchRawY = 0f
    private var isVerticalGesture: Boolean? = null

    private var boundsKnown = false
    private var atTop = false
    private var atBottom = false
    private var lastBridgeMessageTime: Long? = null
    private var lastPollTime = 0L
    private var boundsRevision = 0L
    private var nextPollId = 0L
    private var activePollId: Long? = null
    private var isHandingOffToParent = false
    private var handoffDirection = 0

    @SuppressLint("ClickableViewAccessibility")
    fun attach() {
        registerBoundsBridge()
        webView.setOnTouchListener(::onTouch)
    }

    fun installObserver() {
        webView.evaluateJavascript(WIDGET_INTERNAL_SCROLL_OBSERVER_INSTALL, null)
    }

    private fun registerBoundsBridge() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return

        WebViewCompat.addWebMessageListener(
            webView,
            INTERNAL_SCROLL_BRIDGE_NAME,
            setOf(trustedOrigin),
        ) { _, message, sourceOrigin, isMainFrame, _ ->
            if (!isMainFrame || sourceOrigin.toString() != trustedOrigin) {
                return@addWebMessageListener
            }
            val bounds = message.data?.toIntOrNull() ?: return@addWebMessageListener
            if (bounds !in 0..3) return@addWebMessageListener

            lastBridgeMessageTime = SystemClock.uptimeMillis()
            applyBounds(bounds)
        }
    }

    private fun applyBounds(bounds: Int) {
        boundsRevision++
        boundsKnown = bounds in 0..3
        atTop = boundsKnown && bounds and 1 != 0
        atBottom = boundsKnown && bounds and 2 != 0
    }

    private fun updateBounds(force: Boolean = false) {
        val now = SystemClock.uptimeMillis()
        if (!shouldPollInternalScrollBounds(now, lastBridgeMessageTime)) return
        if (!force && now - lastPollTime < INTERNAL_SCROLL_POLL_INTERVAL_MS) return
        if (!force && activePollId != null) return
        lastPollTime = now

        val requestId = ++nextPollId
        activePollId = requestId
        val revisionAtRequest = boundsRevision
        webView.evaluateJavascript(WIDGET_INTERNAL_SCROLL_BOUNDS_CHECK) { result ->
            val currentRequestId = activePollId
            if (requestId == currentRequestId) activePollId = null
            if (!isCurrentScrollBoundsPoll(requestId, currentRequestId, revisionAtRequest, boundsRevision)) {
                return@evaluateJavascript
            }
            result.toIntOrNull()?.let(::applyBounds)
        }
    }

    private fun onTouch(view: View, event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> startGesture(view, event)
            MotionEvent.ACTION_MOVE -> moveGesture(event)
            MotionEvent.ACTION_POINTER_UP -> replaceLiftedPointer(view, event)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> releaseParentIntercept(view)
            else -> isHandingOffToParent
        }
    }

    private fun startGesture(view: View, event: MotionEvent): Boolean {
        activePointerId = event.getPointerId(0)
        downTouchRawX = event.rawX
        downTouchRawY = event.rawY
        lastTouchRawY = event.rawY
        isVerticalGesture = null
        webView.beginVerticalGesture()
        onGestureActiveChange(true)
        view.parent?.requestDisallowInterceptTouchEvent(true)
        updateBounds(force = true)
        isHandingOffToParent = false
        return false
    }

    private fun moveGesture(event: MotionEvent): Boolean {
        val pointerIndex = event.findPointerIndex(activePointerId)
        if (pointerIndex < 0) return isHandingOffToParent

        val touchRawX = event.pointerRawX(pointerIndex)
        val touchRawY = event.pointerRawY(pointerIndex)
        val deltaY = touchRawY - lastTouchRawY
        lastTouchRawY = touchRawY
        val deltaDirection = when {
            deltaY > 0f -> 1
            deltaY < 0f -> -1
            else -> 0
        }
        if (isHandingOffToParent && deltaDirection != 0 && deltaDirection != handoffDirection) {
            // A direction reversal starts a new inner-scroll phase. Keeping the
            // handoff latched would make the WebView appear stuck after an edge.
            isHandingOffToParent = false
            handoffDirection = 0
        }
        webView.recordVerticalTouchDelta(deltaY)

        if (isVerticalGesture == null) {
            isVerticalGesture = resolveVerticalScrollGesture(
                totalDeltaX = touchRawX - downTouchRawX,
                totalDeltaY = touchRawY - downTouchRawY,
                touchSlop = touchSlop,
            )
        }
        if (isVerticalGesture != true) return isHandingOffToParent

        updateBounds()
        if (
            shouldHandOffVerticalScroll(
                deltaY = deltaY,
                boundsKnown = boundsKnown,
                atTop = atTop,
                atBottom = atBottom,
                nativeClampedInDirection = webView.isClampedInDirection(deltaY),
            )
        ) {
            // The WebView must not process this same move after the parent has
            // received it; otherwise both scroll containers consume one delta.
            if (!isHandingOffToParent) {
                // Let the parent take ownership of the remaining drag. The
                // current delta is dispatched explicitly below because the
                // parent cannot retroactively intercept this MotionEvent.
                webView.parent?.requestDisallowInterceptTouchEvent(false)
                onGestureActiveChange(false)
            }
            isHandingOffToParent = true
            handoffDirection = deltaDirection
            onBoundaryScroll(deltaY)
        }
        return isHandingOffToParent
    }

    private fun replaceLiftedPointer(view: View, event: MotionEvent): Boolean {
        val liftedPointerIndex = event.actionIndex
        if (event.getPointerId(liftedPointerIndex) != activePointerId) return isHandingOffToParent

        val replacementIndex = (0 until event.pointerCount).firstOrNull { it != liftedPointerIndex }
        if (replacementIndex == null) {
            releaseParentIntercept(view)
            return false
        }

        activePointerId = event.getPointerId(replacementIndex)
        downTouchRawX = event.pointerRawX(replacementIndex)
        downTouchRawY = event.pointerRawY(replacementIndex)
        lastTouchRawY = downTouchRawY
        isVerticalGesture = null
        return isHandingOffToParent
    }

    private fun releaseParentIntercept(view: View): Boolean {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        isVerticalGesture = null
        webView.endVerticalGesture()
        onGestureActiveChange(false)
        view.parent?.requestDisallowInterceptTouchEvent(false)
        isHandingOffToParent = false
        handoffDirection = 0
        return false
    }

    private fun MotionEvent.pointerRawX(pointerIndex: Int): Float =
        pointerRawCoordinate(rawX, getX(0), getX(pointerIndex))

    private fun MotionEvent.pointerRawY(pointerIndex: Int): Float =
        pointerRawCoordinate(rawY, getY(0), getY(pointerIndex))
}
