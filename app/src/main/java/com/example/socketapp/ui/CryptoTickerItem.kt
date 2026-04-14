package com.example.socketapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.CryptoTicker
import com.example.socketapp.PriceDirection
import kotlinx.coroutines.delay

private val GreenFlash = Color(0xFF4CAF50).copy(alpha = 0.3f)
private val RedFlash = Color(0xFFF44336).copy(alpha = 0.3f)
private val PositiveGreen = Color(0xFF4CAF50)
private val NegativeRed = Color(0xFFF44336)

@Composable
fun CryptoTickerItem(ticker: CryptoTicker) {
    var isFlashing by remember { mutableStateOf(false) }

    LaunchedEffect(ticker.price) {
        isFlashing = true
        delay(500)
        isFlashing = false
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFlashing && ticker.priceDirection == PriceDirection.UP -> GreenFlash
            isFlashing && ticker.priceDirection == PriceDirection.DOWN -> RedFlash
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 500),
        label = "tickerBgColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: symbol name
        Text(
            text = ticker.displayName,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )

        // Right: price + percent change
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$ ${ticker.price}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            val pctValue = ticker.percentChange.toDoubleOrNull() ?: 0.0
            val pctColor = when {
                pctValue > 0 -> PositiveGreen
                pctValue < 0 -> NegativeRed
                else -> Color.Gray
            }
            val pctSign = if (pctValue > 0) "+" else ""
            Text(
                text = "$pctSign${ticker.percentChange}%",
                color = pctColor,
                fontSize = 12.sp,
            )
        }
    }
}
