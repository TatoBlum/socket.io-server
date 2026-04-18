package com.example.socketapp.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.socketapp.CheckNetworkConnection
import com.example.socketapp.MainScreen
import com.example.socketapp.MainViewModel
import com.example.socketapp.ui.tradingview.TradingViewScreen

@Composable
fun RootScreen(viewModel: MainViewModel, networkConnection: CheckNetworkConnection) {
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            SearchableTopBar(
                title = "Trading View",
                searchPlaceholder = "Buscar",
                isSearchMode = isSearchMode,
                searchQuery = searchQuery,
                showNavigationIcon = isSearchMode,
                onBack = {},
                onCloseSearch = {
                    isSearchMode = false
                    searchQuery = ""
                },
                onOpenSearch = { isSearchMode = true },
                onQueryChange = { searchQuery = it },
            )
        },
    ) { innerPadding ->
        Crossfade(
            targetState = isSearchMode,
            animationSpec = tween(durationMillis = 280),
            label = "titulos-body",
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) { searchMode ->
            if (searchMode) {
                MainScreen(
                    viewModel = viewModel,
                    networkConnection = networkConnection,
                    searchQuery = searchQuery,
                )
            } else {
                TradingViewScreen(networkConnection)
            }
        }
    }
}
