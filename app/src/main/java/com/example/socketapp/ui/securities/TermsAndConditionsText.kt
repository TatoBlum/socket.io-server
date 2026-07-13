package com.example.socketapp.ui.securities

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.socketapp.ui.theme.AppError

private const val TERMS_TAG = "terms_and_conditions"

@Composable
fun TermsAndConditionsText(
    onTermsClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingText: String = "Al comprar estás aceptando los ",
    termsText: String = "Términos y Condiciones",
    trailingText: String = ".",
    highlightedTrailingText: String? = null,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    linkStyle: SpanStyle = SpanStyle(
        color = AppError,
        fontWeight = FontWeight.Bold,
    ),
    highlightedTrailingStyle: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold),
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val text = buildAnnotatedString {
        append(leadingText)
        pushStringAnnotation(
            tag = TERMS_TAG,
            annotation = termsText,
        )
        withStyle(linkStyle) {
            append(termsText)
        }
        pop()
        append(trailingText)

        if (highlightedTrailingText != null) {
            withStyle(highlightedTrailingStyle) {
                append(highlightedTrailingText)
            }
        }
    }

    BasicText(
        text = text,
        modifier = modifier.pointerInput(text) {
            detectTapGestures { position ->
                val offset = textLayoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
                text.getStringAnnotations(
                    tag = TERMS_TAG,
                    start = offset,
                    end = offset,
                ).firstOrNull()?.let {
                    onTermsClick()
                }
            }
        },
        style = style.copy(lineHeight = 28.sp),
        onTextLayout = { result ->
            textLayoutResult = result
        },
    )
}
