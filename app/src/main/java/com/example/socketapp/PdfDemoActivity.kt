package com.example.socketapp

import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.socketapp.ui.securities.Base64PdfViewer
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                PdfDemoScreen(onBack = ::finish)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfDemoScreen(
    onBack: () -> Unit = {},
) {
    var pdfBase64 by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val downloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val currentPdfBase64 = pdfBase64
        if (uri == null || currentPdfBase64 == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(Base64.decode(currentPdfBase64, Base64.DEFAULT))
            } ?: error("No se pudo crear el archivo")
        }.onSuccess {
            Toast.makeText(context, "PDF descargado", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "No se pudo descargar el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) {
                URL(DEMO_PDF_URL).openStream().use { input ->
                    Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
                }
            }
        }.onSuccess { base64 ->
            pdfBase64 = base64
            loadError = null
        }.onFailure { error ->
            pdfBase64 = null
            loadError = error.message ?: "No se pudo descargar el PDF"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier
            .fillMaxSize(),
        shape = RectangleShape,
        dragHandle = null,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val currentPdfBase64 = pdfBase64
            when {
                currentPdfBase64 != null -> {
                    Base64PdfViewer(
                        base64 = currentPdfBase64,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                loadError != null -> {
                    Text(
                        text = loadError.orEmpty(),
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                else -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
            ) {
                IconButton(
                    onClick = { downloadLauncher.launch(DEMO_PDF_FILE_NAME) },
                    enabled = currentPdfBase64 != null,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Descargar PDF",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private const val DEMO_PDF_URL = "https://mozilla.github.io/pdf.js/web/compressed.tracemonkey-pldi-09.pdf"
private const val DEMO_PDF_FILE_NAME = "demo-pdf.pdf"

@Preview(showBackground = true)
@Composable
private fun PdfDemoScreenPreview() {
    MaterialTheme {
        PdfDemoScreen()
    }
}
