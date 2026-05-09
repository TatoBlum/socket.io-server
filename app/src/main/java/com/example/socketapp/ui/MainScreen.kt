package com.example.socketapp.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.socketapp.CheckNetworkConnection
import com.example.socketapp.BuySecurityViewModel
import com.example.socketapp.MainViewModel
import com.example.socketapp.SecuritiesViewModel
import com.example.socketapp.ViewModelFactory
import com.example.socketapp.ui.securities.BuySecurityScreen
import com.example.socketapp.ui.securities.SecuritiesRoute
import com.example.socketapp.ui.tradingview.TradingViewScreen
import com.example.socketapp.ui.tradingview.top5Favorites
import kotlinx.serialization.Serializable

private const val TAG = "MainScreen"

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    viewModelFactory: ViewModelFactory,
    networkConnection: CheckNetworkConnection,
) {
    val tickerMap by mainViewModel.tickers.collectAsStateWithLifecycle()
    val isConnected by networkConnection.observeAsState(initial = false)
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val isSecuritiesRoute = currentRoute == MainRoute.Securities.route
    val isTitlesRoute = currentRoute == MainRoute.Titles.route
    val isDetailRoute = isSecuritiesRoute || isTitlesRoute

    LaunchedEffect(isConnected) {
        if (isConnected) {
            Log.i(TAG, "Network connected — subscribing")
            mainViewModel.subscribeToSocketEvents()
        } else {
            Log.w(TAG, "Network disconnected — stopping")
            mainViewModel.stopSocket()
        }
    }

    val favoritesTop5 = remember(tickerMap) { top5Favorites(tickerMap) }

    Scaffold(
        topBar = {
            SearchableTopBar(
                title = when {
                    isSecuritiesRoute -> "Acciones"
                    isTitlesRoute -> "Comprar PAMP"
                    else -> "Trading View"
                },
                searchPlaceholder = "Buscar",
                isSearchMode = false,
                searchQuery = "",
                showNavigationIcon = isDetailRoute,
                showSearchAction = !isDetailRoute,
                onBack = {
                    navController.popBackStack()
                },
                onCloseSearch = {
                    navController.popBackStack()
                },
                onOpenSearch = {
                    if (!isSecuritiesRoute) {
                        navController.navigate(MainRoute.Securities.route) {
                            launchSingleTop = true
                        }
                    }
                },
                onQueryChange = {},
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainRoute.Home.route,
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) {
            composable(MainRoute.Home.route) {
                TradingViewScreen(
                    networkConnection = networkConnection,
                    favorites = favoritesTop5,
                    onOpenTitles = {
                        navController.navigate(MainRoute.Titles.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(MainRoute.Securities.route) { backStackEntry ->
                val routeViewModel = viewModel<SecuritiesViewModel>(
                    viewModelStoreOwner = backStackEntry, factory = viewModelFactory,
                )
                SecuritiesRoute(viewModel = routeViewModel)
            }

            composable(MainRoute.Titles.route) {
                val buySecurityViewModel = viewModel<BuySecurityViewModel>(
                    viewModelStoreOwner = it,
                    factory = viewModelFactory,
                )
                BuySecurityScreen(
                    uiState = buySecurityViewModel.uiState,
                    onInputModeChange = buySecurityViewModel::onInputModeChange,
                    onInputChange = buySecurityViewModel::onInputChange,
                    onOrderTypeChange = buySecurityViewModel::onOrderTypeChange,
                    onLimitPriceChange = buySecurityViewModel::onLimitPriceChange,
                )
            }
        }
    }
}

@Serializable
private sealed class MainRoute(val route: String) {
    @Serializable
    data object Home : MainRoute("home")

    @Serializable
    data object Securities : MainRoute("securities")

    @Serializable
    data object Titles : MainRoute("titles")
}
