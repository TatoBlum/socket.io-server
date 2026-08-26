package com.example.socketapp.ui.tradingview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Immutable
data class HeatmapInstrument(
    val ticker: String,
    val sector: String,
    val marketCap: Double,
    val variationPercent: Double,
)

enum class NativeHeatmapMarket(
    val displayName: String,
    val instruments: List<HeatmapInstrument>,
) {
    BYMA(
        displayName = "BYMA",
        instruments = listOf(
            HeatmapInstrument("GGAL", "Finanzas", 12.8, -3.34),
            HeatmapInstrument("TECO2", "Comunicacion", 8.2, -1.29),
            HeatmapInstrument("PAMP", "Energia", 7.9, -0.20),
            HeatmapInstrument("YPFD", "Energia", 7.4, -0.75),
            HeatmapInstrument("TXAR", "Materiales", 5.1, -2.29),
            HeatmapInstrument("BBAR", "Finanzas", 4.9, -0.75),
            HeatmapInstrument("ALUA", "Materiales", 4.2, -0.75),
            HeatmapInstrument("EDN", "Energia", 3.9, -1.68),
            HeatmapInstrument("MIRG", "Consumo", 3.4, 4.93),
            HeatmapInstrument("LOMA", "Materiales", 3.1, -0.80),
            HeatmapInstrument("CRES", "Real Estate", 2.8, 1.27),
            HeatmapInstrument("COME", "Consumo", 2.6, 0.38),
            HeatmapInstrument("BYMA", "Finanzas", 2.3, -2.43),
            HeatmapInstrument("CEPU", "Energia", 2.1, 0.69),
            HeatmapInstrument("SUPV", "Finanzas", 1.9, -0.42),
            HeatmapInstrument("TGSU2", "Energia", 1.7, 0.74),
            HeatmapInstrument("MOLI", "Consumo", 1.5, -1.80),
            HeatmapInstrument("HAVA", "Consumo", 1.2, 2.18),
            HeatmapInstrument("AUSO", "Servicios", 1.0, -0.32),
            HeatmapInstrument("TRAN", "Servicios", 0.9, 1.64),
            HeatmapInstrument("DGCU2", "Servicios", 0.8, -0.55),
            HeatmapInstrument("BOLT", "Consumo", 0.7, 0.91),
        ),
    ),
    SP_MERVAL(
        displayName = "S&P Merval",
        instruments = listOf(
            HeatmapInstrument("GGAL", "Finanzas", 12.8, -3.34),
            HeatmapInstrument("TECO2", "Comunicacion", 8.2, -1.29),
            HeatmapInstrument("PAMP", "Energia", 7.9, -0.20),
            HeatmapInstrument("YPFD", "Energia", 7.4, -0.75),
            HeatmapInstrument("TXAR", "Materiales", 5.1, -2.29),
            HeatmapInstrument("BBAR", "Finanzas", 4.9, -0.75),
            HeatmapInstrument("ALUA", "Materiales", 4.2, -0.75),
            HeatmapInstrument("EDN", "Energia", 3.9, -1.68),
            HeatmapInstrument("MIRG", "Consumo", 3.4, 4.93),
            HeatmapInstrument("LOMA", "Materiales", 3.1, -0.80),
            HeatmapInstrument("CRES", "Real Estate", 2.8, 1.27),
            HeatmapInstrument("COME", "Consumo", 2.6, 0.38),
        ),
    ),
}

@Composable
fun NativeHeatmap(
    market: NativeHeatmapMarket,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .padding(4.dp),
    ) {
        HeatmapTileLayout(
            instruments = market.instruments.sortedByDescending { instrument -> instrument.marketCap },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
        )
    }
}

@Composable
private fun HeatmapTileLayout(
    instruments: List<HeatmapInstrument>,
    modifier: Modifier = Modifier,
) {
    Layout(
        content = {
            instruments.forEach { instrument ->
                HeatmapTile(instrument = instrument)
            }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val layouts = calculateTreemap(
            items = instruments,
            width = constraints.maxWidth,
            height = constraints.maxHeight,
        )

        val placeables = measurables.mapIndexed { index, measurable ->
            val rect = layouts[index]
            measurable.measure(
                constraints.copy(
                    minWidth = rect.width,
                    maxWidth = rect.width,
                    minHeight = rect.height,
                    maxHeight = rect.height,
                ),
            )
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val rect = layouts[index]
                placeable.place(rect.x, rect.y)
            }
        }
    }
}

@Composable
private fun HeatmapTile(
    instrument: HeatmapInstrument,
    modifier: Modifier = Modifier,
) {
    val background = heatmapColor(instrument.variationPercent)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        val tileWidth = maxWidth
        val tileHeight = maxHeight

        if (tileWidth > 42.dp && tileHeight > 28.dp) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 3.dp),
            ) {
                Text(
                    text = instrument.ticker,
                    color = Color.White,
                    fontSize = if (tileWidth > 90.dp && tileHeight > 52.dp) 14.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (tileWidth > 54.dp && tileHeight > 40.dp) {
                    Text(
                        text = formatPercent(instrument.variationPercent),
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private data class TileRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

private fun calculateTreemap(
    items: List<HeatmapInstrument>,
    width: Int,
    height: Int,
): List<TileRect> {
    if (items.isEmpty()) return emptyList()

    val rectsByTicker = mutableMapOf<String, TileRect>()
    splitTreemap(
        items = items,
        x = 0,
        y = 0,
        width = width,
        height = height,
        rects = rectsByTicker,
    )
    return items.map { item -> rectsByTicker.getValue(item.ticker) }
}

private fun splitTreemap(
    items: List<HeatmapInstrument>,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    rects: MutableMap<String, TileRect>,
) {
    if (items.isEmpty()) return

    if (items.size == 1) {
        rects[items.first().ticker] = TileRect(
            x = x,
            y = y,
            width = width.coerceAtLeast(1),
            height = height.coerceAtLeast(1),
        )
        return
    }

    if (width <= 2 || height <= 2) {
        items.forEach { item ->
            rects[item.ticker] = TileRect(
                x = x,
                y = y,
                width = width.coerceAtLeast(1),
                height = height.coerceAtLeast(1),
            )
        }
        return
    }

    val total = items.sumOf { item -> item.marketCap }.coerceAtLeast(1.0)
    val target = total / 2
    var running = 0.0
    var splitIndex = 1

    for (index in 1 until items.size) {
        running += items[index - 1].marketCap
        splitIndex = index
        if (running >= target) break
    }

    val firstGroup = items.take(splitIndex)
    val secondGroup = items.drop(splitIndex)
    val firstWeight = firstGroup.sumOf { item -> item.marketCap } / total

    if (width >= height) {
        val firstWidth = (width * firstWeight).toInt().coerceIn(1, width - 1)
        splitTreemap(firstGroup, x, y, firstWidth, height, rects)
        splitTreemap(secondGroup, x + firstWidth, y, width - firstWidth, height, rects)
    } else {
        val firstHeight = (height * firstWeight).toInt().coerceIn(1, height - 1)
        splitTreemap(firstGroup, x, y, width, firstHeight, rects)
        splitTreemap(secondGroup, x, y + firstHeight, width, height - firstHeight, rects)
    }
}

private fun heatmapColor(percent: Double): Color {
    val intensity = (abs(percent) / 5.0).coerceIn(0.18, 1.0).toFloat()
    val positive = Color(0xFF15803D)
    val negative = Color(0xFFB91C1C)
    val neutral = Color(0xFF64748B)

    return when {
        percent > 0.05 -> lerpColor(neutral, positive, intensity)
        percent < -0.05 -> lerpColor(neutral, negative, intensity)
        else -> neutral
    }
}

private fun lerpColor(
    start: Color,
    stop: Color,
    fraction: Float,
): Color =
    Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = 1f,
    )

private fun formatPercent(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return "$sign${"%.2f".format(value)}%"
}
