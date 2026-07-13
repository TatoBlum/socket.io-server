package com.example.socketapp.ui.securities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Immutable
data class StyledInfoSection(
    val title: String,
    val paragraphs: List<StyledInfoParagraph>,
)

@Immutable
data class StyledInfoParagraph(
    val parts: List<StyledInfoTextPart>,
)

@Immutable
data class StyledInfoTextPart(
    val text: String,
    val fontWeight: FontWeight = FontWeight.Normal,
)

@Composable
fun StyledInfoTextContent(
    sections: List<StyledInfoSection>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = section.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                )

                section.paragraphs.forEach { paragraph ->
                    Text(
                        text = paragraph.toAnnotatedString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                    )
                }
            }
        }
    }
}

private fun StyledInfoParagraph.toAnnotatedString() =
    buildAnnotatedString {
        parts.forEach { part ->
            withStyle(SpanStyle(fontWeight = part.fontWeight)) {
                append(part.text)
            }
        }
    }

