package com.example.socketapp.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.R
import kotlinx.coroutines.launch

private data class TitlePagerStep(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val helperTextRes: List<Int>,
    val background: Color,
    val icon: ImageVector,
)

private val titlePagerSteps = listOf(
    TitlePagerStep(
        titleRes = R.string.titles_pager_step_1_title,
        subtitleRes = R.string.titles_pager_step_1_subtitle,
        helperTextRes = listOf(
            R.string.titles_pager_step_1_helper_1,
            R.string.titles_pager_step_1_helper_2,
            R.string.titles_pager_step_1_helper_3,
        ),
        background = Color(0xFFFFF0DF),
        icon = Icons.Outlined.Business,
    ),
    TitlePagerStep(
        titleRes = R.string.titles_pager_step_2_title,
        subtitleRes = R.string.titles_pager_step_2_subtitle,
        helperTextRes = listOf(
            R.string.titles_pager_step_2_helper_1,
            R.string.titles_pager_step_2_helper_2,
            R.string.titles_pager_step_2_helper_3,
        ),
        background = Color(0xFFEAF6FF),
        icon = Icons.Outlined.Insights,
    ),
    TitlePagerStep(
        titleRes = R.string.titles_pager_step_3_title,
        subtitleRes = R.string.titles_pager_step_3_subtitle,
        helperTextRes = listOf(
            R.string.titles_pager_step_3_helper_1,
            R.string.titles_pager_step_3_helper_2,
            R.string.titles_pager_step_3_helper_3,
        ),
        background = Color(0xFFEFF7EA),
        icon = Icons.Outlined.Payments,
    ),
    TitlePagerStep(
        titleRes = R.string.titles_pager_step_4_title,
        subtitleRes = R.string.titles_pager_step_4_subtitle,
        helperTextRes = listOf(
            R.string.titles_pager_step_4_helper_1,
            R.string.titles_pager_step_4_helper_2,
            R.string.titles_pager_step_4_helper_3,
        ),
        background = Color(0xFFF3EEFF),
        icon = Icons.Outlined.Insights,
    ),
    TitlePagerStep(
        titleRes = R.string.titles_pager_step_5_title,
        subtitleRes = R.string.titles_pager_step_5_subtitle,
        helperTextRes = listOf(
            R.string.titles_pager_step_5_helper_1,
            R.string.titles_pager_step_5_helper_2,
            R.string.titles_pager_step_5_helper_3,
        ),
        background = Color(0xFFFFF7D8),
        icon = Icons.Outlined.Payments,
    ),
    TitlePagerStep(
        titleRes = R.string.titles_pager_step_6_title,
        subtitleRes = R.string.titles_pager_step_6_subtitle,
        helperTextRes = listOf(
            R.string.titles_pager_step_6_helper_1,
            R.string.titles_pager_step_6_helper_2,
            R.string.titles_pager_step_6_helper_3,
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
            key = { page -> titlePagerSteps[page].titleRes },
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
                Text(stringResource(R.string.titles_pager_skip))
            }
            Button(
                enabled = !pagerState.isScrollInProgress,
                onClick = {
                    if (pagerState.currentPage < titlePagerSteps.lastIndex) {
                        val nextPage = pagerState.currentPage + 1
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(
                                page = nextPage,
                                animationSpec = tween(
                                    durationMillis = 180,
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
                Text(
                    stringResource(
                        if (pagerState.currentPage == titlePagerSteps.lastIndex) {
                            R.string.titles_pager_finish
                        } else {
                            R.string.titles_pager_next
                        },
                    ),
                )
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
            text = stringResource(R.string.titles_pager_category_actions),
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
            text = stringResource(step.titleRes),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(step.subtitleRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp),
        )

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dashedBorder()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            step.helperTextRes.forEach { helperTextRes ->
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
                        text = stringResource(helperTextRes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

private fun Modifier.dashedBorder(
    color: Color = Color.Black,
    strokeWidth: Dp = 1.dp,
    dashWidth: Dp = 4.dp,
    dashGap: Dp = 4.dp,
    cornerRadius: Dp = 12.dp,
): Modifier = drawWithCache {
    val strokeWidthPx = strokeWidth.toPx()
    val cornerRadiusPx = cornerRadius.toPx()
    val stroke = Stroke(
        width = strokeWidthPx,
        pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(dashWidth.toPx(), dashGap.toPx()),
        ),
    )
    val topLeft = Offset(
        x = strokeWidthPx / 2,
        y = strokeWidthPx / 2,
    )
    val borderSize = size.copy(
        width = size.width - strokeWidthPx,
        height = size.height - strokeWidthPx,
    )

    onDrawBehind {
        drawRoundRect(
            color = color,
            size = borderSize,
            topLeft = topLeft,
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            style = stroke,
        )
    }
}
