package com.example.socketapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val DotSize = 8.dp
private val SmallDotSize = 6.dp
private val TinyDotSize = 4.dp
private val ActiveWidth = 24.dp
private val Spacing = 6.dp
private const val AnimationDurationMs = 240

@Composable
fun ExpandingDotStepper(
    numberOfSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val safeCurrent = currentStep.coerceIn(0, (numberOfSteps - 1).coerceAtLeast(0))

    Row(
        modifier = modifier.height(DotSize),
        horizontalArrangement = Arrangement.spacedBy(Spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (step in 0 until numberOfSteps) {
            val isActive = step == safeCurrent
            val distance = abs(step - safeCurrent)
            val inactiveSize = when (distance) {
                1 -> DotSize
                2 -> SmallDotSize
                else -> TinyDotSize
            }

            val animatedWidth by animateDpAsState(
                targetValue = if (isActive) ActiveWidth else inactiveSize,
                animationSpec = tween(AnimationDurationMs, easing = EaseInOutCubic),
                label = "dotWidth",
            )
            val animatedHeight by animateDpAsState(
                targetValue = if (isActive) DotSize else inactiveSize,
                animationSpec = tween(AnimationDurationMs, easing = EaseInOutCubic),
                label = "dotHeight",
            )
            val animatedColor by animateColorAsState(
                targetValue = if (isActive) activeColor else inactiveColor,
                animationSpec = tween(AnimationDurationMs, easing = EaseInOutCubic),
                label = "dotColor",
            )

            Box(
                modifier = Modifier
                    .height(animatedHeight)
                    .width(animatedWidth)
                    .clip(RoundedCornerShape(50))
                    .background(animatedColor),
            )
        }
    }
}


@Composable
fun ExpandingDotStepperDemo(
    totalSteps: Int = 5,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Paso ${currentStep + 1} de $totalSteps",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExpandingDotStepper(
            numberOfSteps = totalSteps,
            currentStep = currentStep,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { if (currentStep > 0) currentStep-- },
                enabled = currentStep > 0,
            ) { Text("Anterior") }

            Button(
                onClick = { if (currentStep < totalSteps - 1) currentStep++ },
                enabled = currentStep < totalSteps - 1,
            ) { Text("Siguiente") }
        }
    }
}

@Preview(showBackground = true, name = "Interactive")
@Composable
private fun ExpandingDotStepperPreviewInteractive() {
    ExpandingDotStepperDemo(totalSteps = 5)
}
