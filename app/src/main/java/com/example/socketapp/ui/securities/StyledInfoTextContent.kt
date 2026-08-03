package com.example.socketapp.ui.securities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
    val isBullet: Boolean = false,
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
                    StyledInfoParagraphText(paragraph = paragraph)
                }
            }
        }
    }
}

@Composable
private fun StyledInfoParagraphText(paragraph: StyledInfoParagraph) {
    if (paragraph.isBullet) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
            )
            Text(
                text = paragraph.toAnnotatedString(),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
            )
        }
    } else {
        Text(
            text = paragraph.toAnnotatedString(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp,
        )
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

fun moreInformationSections(): List<StyledInfoSection> =
    listOf(
        StyledInfoSection(
            title = "¿Cuándo operar?",
            paragraphs = listOf(
                StyledInfoParagraph(
                    parts = listOf(
                        StyledInfoTextPart("De lunes a viernes de 10:30 a 16:55 h, excepto feriados."),
                    ),
                ),
            ),
        ),
        StyledInfoSection(
            title = "¿Qué necesito saber antes de empezar?",
            paragraphs = listOf(
                StyledInfoParagraph(
                    isBullet = true,
                    parts = listOf(
                        StyledInfoTextPart(
                            "Necesitás una Cuenta Inversora en Galicia y el perfil de inversión. " +
                                "Si no los tenés, te vamos a guiar para hacerlo cuando empieces a operar.",
                        ),
                    ),
                ),
                StyledInfoParagraph(
                    isBullet = true,
                    parts = listOf(
                        StyledInfoTextPart(
                            "Si compraste dólar oficial, vas a tener algunas restricciones para operar Títulos durante 90 días. " +
                                "También aplica si operás Títulos y después querés comprar dólar oficial.",
                        ),
                    ),
                ),
            ),
        ),
        StyledInfoSection(
            title = "¿Qué Títulos puedo operar?",
            paragraphs = listOf(
                StyledInfoParagraph(
                    parts = listOf(
                        StyledInfoTextPart("Acciones: ", FontWeight.Bold),
                        StyledInfoTextPart(
                            "Invertís en partes de una empresa. Su valor sube o baja según el desempeño del negocio y el mercado.",
                        ),
                    ),
                ),
                StyledInfoParagraph(
                    parts = listOf(
                        StyledInfoTextPart("Bonos: ", FontWeight.Bold),
                        StyledInfoTextPart(
                            "Funcionan como préstamos que le hacés al Estado, que luego te devuelve el capital más intereses.",
                        ),
                    ),
                ),
                StyledInfoParagraph(
                    parts = listOf(
                        StyledInfoTextPart("CEDEARs: ", FontWeight.Bold),
                        StyledInfoTextPart(
                            "Representan acciones de empresas extranjeras que cotizan en Argentina. " +
                                "Permiten invertir en el exterior y acompañar al dólar.",
                        ),
                    ),
                ),
                StyledInfoParagraph(
                    parts = listOf(
                        StyledInfoTextPart("ETF (Fondos cotizados): ", FontWeight.Bold),
                        StyledInfoTextPart(
                            "Agrupan muchas acciones o bonos en un solo instrumento. Sirven para diversificar, " +
                                "seguir el desempeño de un sector, mercado o índice.",
                        ),
                    ),
                ),
                StyledInfoParagraph(
                    parts = listOf(
                        StyledInfoTextPart("Letras: ", FontWeight.Bold),
                        StyledInfoTextPart(
                            "Son instrumentos de deuda pública emitidos por el Tesoro Nacional y por el Banco Central para financiarse.",
                        ),
                    ),
                ),
                StyledInfoParagraph(
                    parts = listOf(
                        StyledInfoTextPart("Obligaciones Negociables: ", FontWeight.Bold),
                        StyledInfoTextPart(
                            "Las empresas emiten deuda para financiar sus operaciones. " +
                                "Vos financiás sus proyectos para luego cobrar intereses fijos y el capital invertido.",
                        ),
                    ),
                ),
            ),
        ),
    )
