package com.example.socketapp.ui.securities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

/** One price level in the order book. [percentage] is expressed from 0 to 100. */
data class OrderBookLevel(
    val price: String,
    val quantity: String,
    val percentage: Float = 0f,
    val priceChanged: Boolean = false,
)

enum class OrderBookOperation {
    BUY,
    SELL,
}

/**
 * Compact bid/ask depth box. The lists are displayed from top to bottom and
 * can be replaced directly when a market-data update arrives.
 */
@Composable
fun OrderBookDepthBox(
    bids: List<OrderBookLevel>,
    asks: List<OrderBookLevel>,
    modifier: Modifier = Modifier,
    maxRows: Int = maxOf(bids.size, asks.size),
    currency: String = "",
    operation: OrderBookOperation? = null,
) {
    val rows = maxRows.coerceAtLeast(0)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderCell("Cantidad", Modifier.weight(1f), TextAlign.Start)
                HeaderCell("Comprás a", Modifier.weight(1.35f), TextAlign.Center)
                HeaderCell("Vendés a", Modifier.weight(1.35f), TextAlign.Center)
                HeaderCell("Cantidad", Modifier.weight(1f), TextAlign.End)
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            repeat(rows) { index ->
                val bid = bids.getOrNull(index)
                val ask = asks.getOrNull(index)
                DepthRow(bid = bid, ask = ask,
                    previousBidPrice = bids.getOrNull(index - 1)?.price,
                    previousAskPrice = asks.getOrNull(index - 1)?.price,
                    currency = currency,
                    boldBidQuantity = operation == OrderBookOperation.SELL,
                    boldAskQuantity = operation == OrderBookOperation.BUY,
                )
                if (index < rows - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, alignment: TextAlign) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = alignment,
    )
}

@Composable
private fun DepthRow(
    bid: OrderBookLevel?,
    ask: OrderBookLevel?,
    previousBidPrice: String?,
    previousAskPrice: String?,
    currency: String,
    boldBidQuantity: Boolean,
    boldAskQuantity: Boolean,
) {
    val buyColor = Color(0xFF2E7D32)
    val sellColor = Color(0xFFB3261E)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelCell(
            level = bid,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            alignment = TextAlign.Start,
            quantityFontWeight = if (boldBidQuantity) FontWeight.Bold else FontWeight.Normal,
        )
        LevelCell(
            level = bid,
            modifier = Modifier.weight(1.35f),
            color = buyColor,
            alignment = TextAlign.Center,
            showPrice = true,
            currency = currency,
            previousPrice = previousBidPrice,
        )
        LevelCell(
            level = ask,
            modifier = Modifier.weight(1.35f),
            color = sellColor,
            alignment = TextAlign.Center,
            showPrice = true,
            currency = currency,
            previousPrice = previousAskPrice,
        )
        LevelCell(
            level = ask,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            alignment = TextAlign.End,
            quantityFontWeight = if (boldAskQuantity) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun LevelCell(
    level: OrderBookLevel?,
    modifier: Modifier,
    color: Color,
    alignment: TextAlign,
    showPrice: Boolean = false,
    currency: String = "",
    quantityFontWeight: FontWeight = FontWeight.Normal,
    previousPrice: String? = null,
) {
    if (level == null) {
        Spacer(modifier)
        return
    }
    val intensity = (level.percentage.coerceIn(0f, 100f) / 100f) * 0.08f
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .background(Color.Transparent),
        contentAlignment = when {
            alignment == TextAlign.Start -> Alignment.CenterStart
            alignment == TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.Center
        },
    ) {
        Text(
            text = if (showPrice) {
                priceWithChangedSuffix(
                    currency = currency,
                    price = level.price,
                    previousPrice = previousPrice,
                    highlightColor = color.copy(alpha = intensity),
                )
            } else {
                AnnotatedString(level.quantity)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            color = color,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = if (showPrice) 20.sp else 18.sp,
            ),
            fontWeight = if (showPrice) FontWeight.SemiBold else quantityFontWeight,
            textAlign = alignment,
            maxLines = 1,
        )
    }
}

private fun priceWithChangedSuffix(
    currency: String,
    price: String,
    previousPrice: String?,
    highlightColor: Color,
): AnnotatedString {
    val value = "$currency$price"
    val previous = previousPrice?.let { "$currency$it" }
    val commonLength = if (previous == null) value.length else value.commonPrefixWith(previous).length
    return AnnotatedString.Builder().apply {
        append(value)
        if (previous != null && commonLength < value.length) {
            addStyle(
                style = SpanStyle(background = highlightColor),
                start = commonLength,
                end = value.length,
            )
        }
    }.toAnnotatedString()
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun OrderBookDepthBoxPreview() {
    MaterialTheme {
        OrderBookDepthBox(
            bids = listOf(
                OrderBookLevel("19.620,00", "4.270", 66f),
                OrderBookLevel("19.700,00", "10.056", 78f),
                OrderBookLevel("19.750,00", "40.021", 100f, priceChanged = true),
                OrderBookLevel("19.770,00", "5.325", 48f),
            ),
            asks = listOf(
                OrderBookLevel("19.580,00", "7.643", 58f),
                OrderBookLevel("19.560,00", "209", 24f),
                OrderBookLevel("19.550,00", "9.375", 72f, priceChanged = true),
                OrderBookLevel("19.540,00", "9.375", 70f),
            ),
            modifier = Modifier.padding(16.dp),
            currency = "$",
        )
    }
}
