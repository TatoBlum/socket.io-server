package com.example.socketapp.ui.securities

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Base64
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class Base64PdfViewerGestureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinchOutIncreasesZoom() {
        val zoom = AtomicReference(1f)

        composeRule.setContent {
            Base64PdfViewer(
                base64 = buildTestPdfBase64(),
                modifier = Modifier,
                onZoomChange = zoom::set,
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(Base64PdfViewerContentTag)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag(Base64PdfViewerContentTag)
            .performTouchInput {
                pinch(
                    start0 = center - Offset(40f, 0f),
                    end0 = center - Offset(180f, 0f),
                    start1 = center + Offset(40f, 0f),
                    end1 = center + Offset(180f, 0f),
                )
            }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            zoom.get() > 1f
        }

        assertTrue("Expected zoom to increase after pinch out, but was ${zoom.get()}", zoom.get() > 1f)
    }

    private fun buildTestPdfBase64(): String {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 420, 1).create()
        val page = document.startPage(pageInfo)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            isFakeBoldText = true
        }

        page.canvas.drawText("PDF zoom test", 32f, 80f, paint)
        document.finishPage(page)

        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()

        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
