package com.example.socketapp.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedTopBarSandbox(
    title: String,
    searchPlaceholder: String,
    isSearchMode: Boolean,
    searchQuery: String,
    onSearchModeChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    SearchableTopBar(
        title = title,
        isSearchMode = isSearchMode,
        searchQuery = searchQuery,
        searchPlaceholder = searchPlaceholder,
        onBack = {
            //
        },
        onCloseSearch = {
            onSearchModeChange(false)
            onSearchQueryChange("")
        },
        onOpenSearch = { onSearchModeChange(true) },
        onQueryChange = onSearchQueryChange,
        extraActions = {
            IconButton(onClick = {
                //
            }) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "info",
                )
            }
        }
    )
}

@Composable
fun PlaceholderScreen(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight(700),
            ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AnimatedTopBarSandboxPreview() {
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    // Scaffold() {
    AnimatedTopBarSandbox(
        title = "Títulos",
        searchPlaceholder = "Buscá el título",
        isSearchMode = isSearchMode,
        searchQuery = searchQuery,
        onSearchModeChange = { isSearchMode = it },
        onSearchQueryChange = { searchQuery = it },
    )

    Crossfade(
        targetState = isSearchMode,
        animationSpec = tween(durationMillis = 280),
        label = "titulos-body",
    ) { searchMode ->
        if (searchMode) {
            PlaceholderScreen(label = "Pantalla Precios")
        } else {
            PlaceholderScreen(label = "Trading View Screen")
        }
    }
    //   }
}
