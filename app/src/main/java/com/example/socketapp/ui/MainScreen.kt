package com.example.socketapp.ui

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.socketapp.CheckNetworkConnection
import com.example.socketapp.MainViewModel
import com.example.socketapp.ui.stocks.StocksScreen
import com.example.socketapp.ui.tradingview.TradingViewScreen
import com.example.socketapp.ui.tradingview.top5Favorites

private const val TAG = "MainScreen"

@Composable
fun MainScreen(viewModel: MainViewModel, networkConnection: CheckNetworkConnection) {
    val tickerMap by viewModel.tickers.collectAsStateWithLifecycle()
    val isConnected by networkConnection.observeAsState(initial = false)

    LaunchedEffect(isConnected) {
        if (isConnected) {
            Log.i(TAG, "Network connected — subscribing")
            viewModel.subscribeToSocketEvents()
        } else {
            Log.w(TAG, "Network disconnected — stopping")
            viewModel.stopSocket()
        }
    }

    val favoritesTop5 = remember(tickerMap) { top5Favorites(tickerMap) }

    var isSearchMode by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SearchableTopBar(
                title = "Trading View",
                searchPlaceholder = "Buscar",
                isSearchMode = isSearchMode,
                searchQuery = viewModel.searchQuery,
                showNavigationIcon = isSearchMode,
                onBack = {},
                onCloseSearch = {
                    isSearchMode = false
                    viewModel.onSearchQueryChange("")
                },
                onOpenSearch = { isSearchMode = true },
                onQueryChange = viewModel::onSearchQueryChange,
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = isSearchMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(durationMillis = 320)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 320),
                        initialOffsetY = { fullHeight -> fullHeight / 10 },
                    )) togetherWith
                    (fadeOut(animationSpec = tween(durationMillis = 220)) +
                        slideOutVertically(
                            animationSpec = tween(durationMillis = 220),
                            targetOffsetY = { fullHeight -> -fullHeight / 10 },
                        )) using SizeTransform(clip = false) { _, _ ->
                    tween(durationMillis = 0)
                }
            },
            label = "titulos-body",
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) { searchMode ->
            if (searchMode) {
                StocksScreen(
                    viewModel = viewModel,
                    searchQuery = viewModel.searchQuery,
                )
            } else {
                TradingViewScreen(networkConnection, favoritesTop5)
            }
        }
    }
}
