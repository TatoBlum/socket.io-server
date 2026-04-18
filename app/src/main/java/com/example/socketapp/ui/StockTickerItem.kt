package com.example.socketapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.example.socketapp.model.PriceDirection
import com.example.socketapp.model.StockTicker
import com.example.socketapp.ui.theme.PriceDown
import com.example.socketapp.ui.theme.PriceDownFlash
import com.example.socketapp.ui.theme.PricePctTextStyle
import com.example.socketapp.ui.theme.PriceTextStyle
import com.example.socketapp.ui.theme.PriceUp
import com.example.socketapp.ui.theme.PriceUpFlash
import com.example.socketapp.ui.theme.SoftAvatarPalette
import kotlinx.coroutines.delay

private fun avatarColor(symbol: String): Color =
    SoftAvatarPalette[(symbol.hashCode() and 0x7FFFFFFF) % SoftAvatarPalette.size]

@Composable
fun StockTickerItem(ticker: StockTicker) {
    var isFlashing by remember { mutableStateOf(false) }

    LaunchedEffect(ticker.price) {
        isFlashing = true
        delay(500)
        isFlashing = false
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isFlashing && ticker.priceDirection == PriceDirection.UP -> PriceUpFlash
            isFlashing && ticker.priceDirection == PriceDirection.DOWN -> PriceDownFlash
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(avatarColor(ticker.symbol)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ticker.symbol.take(2),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = ticker.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$ ${ticker.price}",
                color = MaterialTheme.colorScheme.onSurface,
                style = PriceTextStyle,
            )
            if (ticker.percentChange != "0.00") {
                val pctValue = ticker.percentChange.toDoubleOrNull() ?: 0.0
                val pctColor = when {
                    pctValue > 0 -> PriceUp
                    pctValue < 0 -> PriceDown
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val pctSign = if (pctValue > 0) "+" else ""
                Text(
                    text = "$pctSign${ticker.percentChange}%",
                    color = pctColor,
                    style = PricePctTextStyle,
                )
            }
        }
    }
}
