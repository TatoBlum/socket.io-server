package com.example.socketapp.ui

import android.util.Log
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.example.socketapp.CheckNetworkConnection
import com.example.socketapp.TradeViewModel
import com.example.socketapp.MainViewModel
import com.example.socketapp.SecuritiesViewModel
import com.example.socketapp.TradeType
import com.example.socketapp.ui.securities.SecuritiesRoute
import com.example.socketapp.ui.securities.TradeAccountSelectionScreen
import com.example.socketapp.ui.securities.TradeConfirmationScreen
import com.example.socketapp.ui.securities.TradeScreen
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
    var activeTradeTypeName by rememberSaveable { mutableStateOf(TradeType.Buy.name) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val isSecuritiesRoute = currentDestination?.hasRoute(MainRoute.Securities::class) == true
    val isTradeAccountSelectionRoute =
        currentDestination?.hasRoute(MainRoute.TradeAccountSelection::class) == true
    val isBuyRoute = currentDestination?.hasRoute(MainRoute.BuyAndSell::class) == true
    val isTradeConfirmationRoute =
        currentDestination?.hasRoute(MainRoute.TradeConfirmation::class) == true
    val isDetailRoute =
        isSecuritiesRoute || isTradeAccountSelectionRoute || isBuyRoute || isTradeConfirmationRoute

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
            if (!isTradeConfirmationRoute) {
                SearchableTopBar(
                    title = when {
                        isSecuritiesRoute -> "Acciones"
                        isTradeAccountSelectionRoute -> "Seleccionar cuenta"
                        isBuyRoute -> activeTradeTypeName.toTradeType().topBarTitle
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
                            navController.navigate(MainRoute.Securities) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onQueryChange = {},
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainRoute.Home,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            composable<MainRoute.Home> {
                TradingViewScreen(
                    networkConnection = networkConnection,
                    favorites = favoritesTop5,
                    onTradeAction = { codeType, codeValue, tradeType ->
                        activeTradeTypeName = tradeType.name
                        navController.navigate(MainRoute.TradeFlow(codeType, codeValue, tradeType.name)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable<MainRoute.Securities> { backStackEntry ->
                val routeViewModel = hiltViewModel<SecuritiesViewModel>(backStackEntry)
                SecuritiesRoute(
                    viewModel = routeViewModel,
                    onSecurityClick = { security ->
                        if (navController.currentDestination?.hasRoute(MainRoute.Securities::class) == true) {
                            activeTradeTypeName = TradeType.Buy.name
                            navController.navigate(
                                MainRoute.TradeFlow(
                                    codeType = security.codeType,
                                    codeValue = security.codeValue,
                                    tradeType = TradeType.Buy.name,
                                ),
                            ) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }

            navigation<MainRoute.TradeFlow>(
                startDestination = MainRoute.TradeAccountSelection,
            ) {
                composable<MainRoute.TradeAccountSelection> { backStackEntry ->
                    val tradeFlowEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<MainRoute.TradeFlow>()
                    }
                    val tradeRoute = tradeFlowEntry.toRoute<MainRoute.TradeFlow>()
                    val codeType = tradeRoute.codeType
                    val codeValue = tradeRoute.codeValue
                    val tradeType = tradeRoute.tradeType.toTradeType()
                    val tradeViewModel = hiltViewModel<TradeViewModel>(tradeFlowEntry)

                    LaunchedEffect(codeType, codeValue, tradeType) {
                        activeTradeTypeName = tradeType.name
                        tradeViewModel.onTradeTypeChange(tradeType)
                        tradeViewModel.loadInstrument(
                            codeType = codeType,
                            codeValue = codeValue,
                        )
                    }

                    TradeAccountSelectionScreen(
                        uiState = tradeViewModel.uiState,
                        onAccountSelected = tradeViewModel::onTradeMonetaryAccountSelected,
                        onContinue = {
                            navController.navigate(MainRoute.BuyAndSell)
                        },
                    )
                }

                composable<MainRoute.BuyAndSell> { backStackEntry ->
                    val tradeFlowEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<MainRoute.TradeFlow>()
                    }
                    val tradeViewModel = hiltViewModel<TradeViewModel>(tradeFlowEntry)

                    TradeScreen(
                        uiState = tradeViewModel.uiState,
                        onInputModeChange = tradeViewModel::onInputModeChange,
                        onInputChange = { inputMode, input -> tradeViewModel.onInputChange(inputMode, input) },
                        onSettlementTermChange = tradeViewModel::onSettlementTermChange,
                        onOrderTypeChange = tradeViewModel::onOrderTypeChange,
                        onLimitPriceChange = tradeViewModel::onLimitPriceChange,
                        onContinue = {
                            if (tradeViewModel.uiState.canContinue) {
                                tradeViewModel.prepareConfirmation()
                                navController.navigate(MainRoute.TradeConfirmation)
                            }
                        },
                        onTradeOptionsChange = { option ->
                            tradeViewModel.onTradeOptionsChange(option)
                        }
                    )
                }

                composable<MainRoute.TradeConfirmation> { backStackEntry ->
                    val tradeFlowEntry = remember(backStackEntry) {
                        navController.getBackStackEntry<MainRoute.TradeFlow>()
                    }
                    val tradeViewModel = hiltViewModel<TradeViewModel>(tradeFlowEntry)

                    TradeConfirmationScreen(
                        uiState = tradeViewModel.uiState,
                        onBack = { navController.popBackStack() },
                        onFeeCommissionAccountSelected = tradeViewModel::onFeeCommissionAccountSelected,
                        onConfirm = {},
                    )
                }
            }
        }
    }
}

@Serializable
private sealed interface MainRoute {
    @Serializable
    data object Home : MainRoute

    @Serializable
    data object Securities : MainRoute

    @Serializable
    data class TradeFlow(
        val codeType: String,
        val codeValue: String,
        val tradeType: String = "Buy",
    ) : MainRoute

    @Serializable
    data object TradeAccountSelection : MainRoute

    @Serializable
    data object BuyAndSell : MainRoute

    @Serializable
    data object TradeConfirmation : MainRoute
}

private fun String.toTradeType(): TradeType =
    runCatching { TradeType.valueOf(this) }.getOrDefault(TradeType.Buy)

private val TradeType.topBarTitle: String
    get() = when (this) {
        TradeType.Buy -> "Comprar"
        TradeType.Sell -> "Vender"
    }
