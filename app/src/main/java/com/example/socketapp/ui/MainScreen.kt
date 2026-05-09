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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.socketapp.CheckNetworkConnection
import com.example.socketapp.BuySecurityViewModel
import com.example.socketapp.MainViewModel
import com.example.socketapp.SecuritiesViewModel
import com.example.socketapp.ui.securities.BuySecurityScreen
import com.example.socketapp.ui.securities.SecuritiesRoute
import com.example.socketapp.ui.tradingview.TradingViewScreen
import com.example.socketapp.ui.tradingview.top5Favorites
import kotlinx.serialization.Serializable

private const val TAG = "MainScreen"

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
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
                    isTitlesRoute -> "Comprar"
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
                        navController.navigate(MainRoute.Titles.createRoute("PAMP-0")) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(MainRoute.Securities.route) { backStackEntry ->
                val routeViewModel = hiltViewModel<SecuritiesViewModel>(backStackEntry)
                SecuritiesRoute(
                    viewModel = routeViewModel,
                    onSecurityClick = { security ->
                        navController.navigate(MainRoute.Titles.createRoute(security.id))
                    },
                )
            }

            composable(
                route = MainRoute.Titles.route,
                arguments = listOf(navArgument(MainRoute.Titles.SecurityIdArgument) { type = NavType.StringType }),
            ) { backStackEntry ->
                val securityId = backStackEntry.arguments
                    ?.getString(MainRoute.Titles.SecurityIdArgument)
                    .orEmpty()
                val buySecurityViewModel = hiltViewModel<BuySecurityViewModel>(backStackEntry)

                LaunchedEffect(securityId) {
                    buySecurityViewModel.loadInstrument(securityId)
                }

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
    data object Titles : MainRoute("buy/{securityId}") {
        const val SecurityIdArgument = "securityId"

        fun createRoute(securityId: String): String = "buy/$securityId"
    }
}
