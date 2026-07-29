package com.example.socketapp.ui.securities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Base64PdfViewer(
    base64: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 24.dp),
    pageSpacing: Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    minZoom: Float = 1f,
    maxZoom: Float = 5f,
    onZoomChange: (Float) -> Unit = {},
    onError: (Throwable) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var bitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var zoom by remember { mutableStateOf(minZoom) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextZoom = (zoom * zoomChange).coerceIn(minZoom, maxZoom)
        zoom = nextZoom
        onZoomChange(nextZoom)
        offset = if (nextZoom == minZoom) {
            Offset.Zero
        } else {
            offset + panChange
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        val pageWidthPx = with(density) { maxWidth.roundToPx() }

        LaunchedEffect(base64, pageWidthPx) {
            if (pageWidthPx <= 0 || base64.isBlank()) return@LaunchedEffect

            isLoading = true
            zoom = minZoom
            offset = Offset.Zero
            runCatching {
                withContext(Dispatchers.IO) {
                    renderPdfPages(
                        context = context,
                        base64 = base64,
                        pageWidthPx = pageWidthPx,
                    )
                }
            }.onSuccess { pages ->
                bitmaps.forEach { bitmap -> bitmap.recycle() }
                bitmaps = pages
                isLoading = false
            }.onFailure { error ->
                bitmaps.forEach { bitmap -> bitmap.recycle() }
                bitmaps = emptyList()
                isLoading = false
                onError(error)
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                bitmaps.forEach { bitmap -> bitmap.recycle() }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(Base64PdfViewerContentTag)
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .transformable(transformableState),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(pageSpacing),
                ) {
                    items(bitmaps) { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
            }
        }
    }
}

const val Base64PdfViewerContentTag = "base64-pdf-viewer-content"

private fun renderPdfPages(
    context: Context,
    base64: String,
    pageWidthPx: Int,
): List<Bitmap> {
    val file = writeBase64PdfToCache(context, base64)

    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fileDescriptor ->
            PdfRenderer(fileDescriptor).use { renderer ->
                (0 until renderer.pageCount).map { pageIndex ->
                    renderer.openPage(pageIndex).use { page ->
                        val scale = pageWidthPx.toFloat() / page.width
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                        )

                        bitmap
                    }
                }
            }
        }
    } finally {
        file.delete()
    }
}

private fun writeBase64PdfToCache(
    context: Context,
    base64: String,
): File {
    val pdfBytes = Base64.decode(base64, Base64.DEFAULT)
    return File.createTempFile("inline-pdf-", ".pdf", context.cacheDir)
        .also { file ->
            file.outputStream().use { outputStream ->
                outputStream.write(pdfBytes)
            }
        }
}
