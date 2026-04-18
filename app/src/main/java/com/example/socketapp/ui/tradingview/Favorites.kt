package com.example.socketapp.ui.tradingview

import com.example.socketapp.Constants
import com.example.socketapp.model.StockTicker

fun top5Favorites(
    tickerMap: Map<String, StockTicker>,
    symbols: List<String> = Constants.SYMBOLS,
): List<StockTicker> =
    symbols.take(5).mapNotNull { tickerMap[it] }
