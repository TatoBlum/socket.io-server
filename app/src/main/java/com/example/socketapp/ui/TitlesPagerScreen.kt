package com.example.socketapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class TitlePagerStep(
    val title: String,
    val subtitle: String,
    val helperTexts: List<String>,
    val background: Color,
    val icon: ImageVector,
)

private val titlePagerSteps = listOf(
    TitlePagerStep(
        title = "Forma parte de las empresas que conoces",
        subtitle = "Podes invertir en companias que te interesen y acompanar sus posibles ganancias.",
        helperTexts = listOf(
            "Accedes a empresas que cotizan en el mercado local.",
            "Aprovechas el potencial de crecimiento a largo plazo.",
            "En algunas podes cobrar dividendos.",
        ),
        background = Color(0xFFFFF0DF),
        icon = Icons.Outlined.Business,
    ),
    TitlePagerStep(
        title = "Maneja tus inversiones cuando quieras",
        subtitle = "Cuando compras titulos, tu dinero acompana el movimiento de cada empresa o sector.",
        helperTexts = listOf(
            "Sabes de antemano el precio de referencia.",
            "Recibis informacion para seguir cada posicion.",
            "Podes vender cuando quieras volver a pesos.",
        ),
        background = Color(0xFFEAF6FF),
        icon = Icons.Outlined.Insights,
    ),
    TitlePagerStep(
        title = "Diversifica desde un solo lugar",
        subtitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor.",
        helperTexts = listOf(
            "Lorem ipsum dolor sit amet.",
            "Consectetur adipiscing elit.",
            "Sed do eiusmod tempor incididunt.",
        ),
        background = Color(0xFFEFF7EA),
        icon = Icons.Outlined.Payments,
    ),
    TitlePagerStep(
        title = "Segui la evolucion de cada titulo",
        subtitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor.",
        helperTexts = listOf(
            "Revisas precios, variaciones y datos clave.",
            "Comparas alternativas antes de operar.",
            "Tenes informacion actualizada para decidir.",
        ),
        background = Color(0xFFF3EEFF),
        icon = Icons.Outlined.Insights,
    ),
    TitlePagerStep(
        title = "Inverti con el monto que prefieras",
        subtitle = "Podes empezar de forma simple y ajustar tu posicion segun tus objetivos.",
        helperTexts = listOf(
            "Elegis cuanto queres invertir.",
            "Confirmas la operacion antes de enviarla.",
            "Consultas el detalle desde tu cartera.",
        ),
        background = Color(0xFFFFF7D8),
        icon = Icons.Outlined.Payments,
    ),
    TitlePagerStep(
        title = "Organiza tus proximos movimientos",
        subtitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt.",
        helperTexts = listOf(
            "Guardas favoritos para seguirlos de cerca.",
            "Revisas oportunidades del mercado.",
            "Volves a operar cuando lo necesites.",
        ),
        background = Color(0xFFE9F4EF),
        icon = Icons.Outlined.Business,
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TitlesPagerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { titlePagerSteps.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            TitlePagerPage(step = titlePagerSteps[page])
        }

        ExpandingDotStepper(
            numberOfSteps = titlePagerSteps.size,
            currentStep = pagerState.currentPage,
            inactiveColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Text("Omitir")
            }
            Button(
                onClick = {
                    if (pagerState.currentPage < titlePagerSteps.lastIndex) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + 1,
                                animationSpec = tween(
                                    durationMillis = 450,
                                    easing = FastOutSlowInEasing,
                                ),
                            )
                        }
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(if (pagerState.currentPage == titlePagerSteps.lastIndex) "Finalizar" else "Siguiente")
            }
        }
    }
}

@Composable
private fun TitlePagerPage(
    step: TitlePagerStep,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(step.background),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Acciones",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = step.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = step.subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp),
        )

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            step.helperTexts.forEach { helperText ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Text(
                        text = helperText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}
