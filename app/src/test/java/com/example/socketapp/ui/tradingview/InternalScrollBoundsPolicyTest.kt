package com.example.socketapp.ui.tradingview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InternalScrollBoundsPolicyTest {

    @Test
    fun `polls until the bridge sends its first bounds`() {
        assertTrue(
            shouldPollInternalScrollBounds(
                nowMillis = 10_000L,
                lastBridgeMessageMillis = null,
            ),
        )
    }

    @Test
    fun `fresh bridge bounds suppress regular polling`() {
        assertFalse(
            shouldPollInternalScrollBounds(
                nowMillis = 10_500L,
                lastBridgeMessageMillis = 10_000L,
            ),
        )
    }

    @Test
    fun `stale bridge bounds reactivate polling`() {
        assertTrue(
            shouldPollInternalScrollBounds(
                nowMillis = 12_501L,
                lastBridgeMessageMillis = 10_000L,
            ),
        )
    }

    @Test
    fun `gesture start keeps fresh observer bounds`() {
        assertFalse(
            shouldPollInternalScrollBounds(
                nowMillis = 10_100L,
                lastBridgeMessageMillis = 10_000L,
            ),
        )
    }

    @Test
    fun `cross origin iframe without visible bounds keeps the gesture`() {
        assertTrue(
            WIDGET_INTERNAL_SCROLL_BOUNDS_CHECK.contains(
                "return document.querySelector('iframe') ? -1 : 3",
            ),
        )
    }

    @Test
    fun `gesture direction stays unresolved inside touch slop`() {
        assertNull(resolveVerticalScrollGesture(3f, 4f, touchSlop = 8f))
    }

    @Test
    fun `vertical movement wins after touch slop`() {
        assertTrue(resolveVerticalScrollGesture(4f, 12f, touchSlop = 8f) == true)
    }

    @Test
    fun `horizontal movement is not handed to outer vertical scroll`() {
        assertFalse(resolveVerticalScrollGesture(12f, 4f, touchSlop = 8f) == true)
    }

    @Test
    fun `poll result is accepted only while request and revision are current`() {
        assertTrue(
            isCurrentScrollBoundsPoll(
                requestId = 4L,
                activeRequestId = 4L,
                boundsRevisionAtRequest = 7L,
                currentBoundsRevision = 7L,
            ),
        )
        assertFalse(
            isCurrentScrollBoundsPoll(
                requestId = 4L,
                activeRequestId = 5L,
                boundsRevisionAtRequest = 7L,
                currentBoundsRevision = 7L,
            ),
        )
        assertFalse(
            isCurrentScrollBoundsPoll(
                requestId = 4L,
                activeRequestId = 4L,
                boundsRevisionAtRequest = 7L,
                currentBoundsRevision = 8L,
            ),
        )
    }

    @Test
    fun `raw coordinate for primary pointer is unaffected by local view position`() {
        val beforeParentScroll = pointerRawCoordinate(
            primaryPointerRaw = 640f,
            primaryPointerLocal = 240f,
            pointerLocal = 240f,
        )
        val afterParentScroll = pointerRawCoordinate(
            primaryPointerRaw = 640f,
            primaryPointerLocal = 140f,
            pointerLocal = 140f,
        )

        assertEquals(
            beforeParentScroll,
            afterParentScroll,
            0f,
        )
    }

    @Test
    fun `raw coordinate preserves secondary pointer offset`() {
        assertEquals(
            700f,
            pointerRawCoordinate(
                primaryPointerRaw = 640f,
                primaryPointerLocal = 240f,
                pointerLocal = 300f,
            ),
            0f,
        )
    }

    @Test
    fun `known script bounds hand off only past the reported edge`() {
        assertTrue(
            shouldHandOffVerticalScroll(
                deltaY = 10f,
                boundsKnown = true,
                atTop = true,
                atBottom = false,
                nativeClampedInDirection = false,
            ),
        )
        assertFalse(
            shouldHandOffVerticalScroll(
                deltaY = -10f,
                boundsKnown = true,
                atTop = true,
                atBottom = false,
                nativeClampedInDirection = true,
            ),
        )
    }

    @Test
    fun `native clamp hands off cross origin iframe gestures`() {
        assertTrue(
            shouldHandOffVerticalScroll(
                deltaY = 10f,
                boundsKnown = false,
                atTop = false,
                atBottom = false,
                nativeClampedInDirection = true,
            ),
        )
        assertFalse(
            shouldHandOffVerticalScroll(
                deltaY = -10f,
                boundsKnown = false,
                atTop = false,
                atBottom = false,
                nativeClampedInDirection = false,
            ),
        )
    }

}
