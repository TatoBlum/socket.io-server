package com.example.socketapp.ui.stocks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socketapp.Constants
import com.example.socketapp.MainViewModel
import com.example.socketapp.model.ConnectionState
import java.util.Calendar
import java.util.TimeZone

@Composable
fun StocksScreen(
    viewModel: MainViewModel,
    searchQuery: String,
) {
    val tickerMap by viewModel.tickers.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()

    // Filter + sort: recomputes only when map or query changes
    val tickerList = remember(tickerMap, searchQuery) {
        tickerMap.values
            .filter { ticker ->
                searchQuery.isBlank() ||
                        ticker.displayName.contains(searchQuery, ignoreCase = true) ||
                        ticker.symbol.contains(searchQuery, ignoreCase = true)
            }
            .sortedBy { ticker ->
                Constants.SYMBOLS.indexOf(ticker.symbol)
                    .let { if (it == -1) Int.MAX_VALUE else it }
            }
            .toList()
    }

    Column(modifier = Modifier.Companion.fillMaxSize()) {
        // Connection status bar at top
        ConnectionStatusBar(
            connectionState = connection,
            onConnect = { viewModel.subscribeToSocketEvents() },
            onDisconnect = { viewModel.stopSocket() },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (tickerList.isEmpty() && connection == ConnectionState.Connected) {
            val message = when {
                searchQuery.isNotBlank() -> "Sin resultados"
                isUsMarketOpen() -> "Cargando precios..."
                else -> "Mercado cerrado (abre 9:30 ET, lun-vie)"
            }
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(32.dp)
                    .align(Alignment.CenterHorizontally),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
        ) {
            items(
                items = tickerList,
                key = { it.symbol },
            ) { ticker ->
                StockTickerItem(ticker = ticker)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

private fun isUsMarketOpen(): Boolean {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
    val day = cal.get(Calendar.DAY_OF_WEEK)
    if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return false
    val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    return minutes in 570 until 960
}
