package com.example.socketapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.socketapp.CheckNetworkConnection
import com.example.socketapp.MainScreen
import com.example.socketapp.MainViewModel
import com.example.socketapp.ui.heatmap.HeatmapScreen

@Composable
fun RootScreen(
    viewModel: MainViewModel,
    networkConnection: CheckNetworkConnection,
) {
    var selectedTab by rememberSaveable { mutableStateOf(RootTab.Prices) }

    Scaffold(
        containerColor = Color(0xFF121212),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E1E1E)) {
                NavigationBarItem(
                    selected = selectedTab == RootTab.Prices,
                    onClick = { selectedTab = RootTab.Prices },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("Precios") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF4CAF50),
                        indicatorColor = Color(0xFF2E7D32),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                    ),
                )
                NavigationBarItem(
                    selected = selectedTab == RootTab.Heatmap,
                    onClick = { selectedTab = RootTab.Heatmap },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("Heatmap") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF4CAF50),
                        indicatorColor = Color(0xFF2E7D32),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                    ),
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (selectedTab) {
                RootTab.Prices -> MainScreen(viewModel, networkConnection)
                RootTab.Heatmap -> HeatmapScreen(networkConnection)
            }
        }
    }
}
