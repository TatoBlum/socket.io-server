package com.example.socketapp.ui.securities

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.socketapp.SecuritiesUiState
import com.example.socketapp.SecuritiesViewModel
import com.example.socketapp.Security
import com.example.socketapp.SecurityFilters
import com.example.socketapp.SecuritySortOption
import com.example.socketapp.ui.theme.AvatarInitial
import com.example.socketapp.ui.theme.AvatarPalette
import com.example.socketapp.ui.theme.PriceDown
import com.example.socketapp.ui.theme.PricePctTextStyle
import com.example.socketapp.ui.theme.PriceTextStyle
import com.example.socketapp.ui.theme.PriceUp
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SecuritiesRoute(
    viewModel: SecuritiesViewModel,
    onSecurityClick: (Security) -> Unit,
    modifier: Modifier = Modifier,
) {
    LifecycleResumeEffect(viewModel) {
        viewModel.startPollingIfNeeded()

        onPauseOrDispose {
            viewModel.stopPolling()
        }
    }

    SecuritiesScreen(
        uiState = viewModel.uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onApplyFilters = viewModel::applyFilters,
        onClearFilters = viewModel::clearFilters,
        onFavouriteClick = viewModel::onFavouriteClick,
        onSecurityClick = onSecurityClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritiesScreen(
    uiState: SecuritiesUiState,
    onSearchQueryChange: (String) -> Unit,
    onApplyFilters: (SecurityFilters) -> Unit,
    onClearFilters: () -> Unit,
    onFavouriteClick: (String) -> Unit,
    onSecurityClick: (Security) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilters by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            placeholder = { Text(text = "Buscar") },
            shape = RoundedCornerShape(8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${uiState.items.size} acciones",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = { showFilters = true }) {
                Text(text = "Filtrar")
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (uiState.errorState.first) {
            Text(
                text = uiState.errorState.second.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (uiState.isLoading && uiState.items.isEmpty()) {
            SecuritiesLoadingState()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!uiState.isLoading && uiState.items.isEmpty()) {
                item(key = "empty-state") {
                    SecuritiesEmptyState(uiState = uiState)
                }
            }

            items(
                items = uiState.items,
                key = { item -> item.codeValue },
            ) { item ->
                SecurityItem(
                    item = item,
                    onFavouriteClick = { onFavouriteClick(item.ticker) },
                    onClick = { onSecurityClick(item) },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 60.dp),
                )
            }
        }
    }

    if (showFilters) {
        var draftFilters by remember { mutableStateOf(uiState.filters) }

        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            SecuritiesFilterSheet(
                filters = draftFilters,
                sectorOptions = uiState.sectorOptions,
                onFiltersChange = { draftFilters = it },
                onDismiss = { showFilters = false },
                onClearFilters = {
                    draftFilters = SecurityFilters()
                    onClearFilters()
                },
                onApply = {
                    onApplyFilters(draftFilters)
                    showFilters = false
                },
            )
        }
    }
}

@Composable
private fun SecuritiesLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Cargando acciones...",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SecuritiesEmptyState(uiState: SecuritiesUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val message = if (uiState.searchQuery.isNotBlank()) {
            "Sin resultados"
        } else {
            "No hay acciones disponibles"
        }
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun SecurityItem(
    item: Security,
    onFavouriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFavouriteIcon: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(avatarColor(item.ticker)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.ticker.take(2),
                    color = AvatarInitial,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.ticker,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SecurityPriceColumn(item = item)

            if (showFavouriteIcon) {
                Spacer(modifier = Modifier.width(8.dp))
                FavouriteIconButton(
                    isFavourite = item.isFavorite,
                    onClick = onFavouriteClick,
                )
            }
        }
    }
}

@Composable
private fun SecurityPriceColumn(item: Security) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = item.formattedPrice(),
            color = MaterialTheme.colorScheme.onSurface,
            style = PriceTextStyle,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val variationDirection = item.variationDirection()
            val variationColor = when (variationDirection) {
                PriceVariationDirection.Up -> PriceUp
                PriceVariationDirection.Down -> PriceDown
                PriceVariationDirection.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            when (variationDirection) {
                PriceVariationDirection.Up -> Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = "Subio",
                    tint = variationColor,
                    modifier = Modifier.size(14.dp),
                )
                PriceVariationDirection.Down -> Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = "Bajo",
                    tint = variationColor,
                    modifier = Modifier.size(14.dp),
                )
                PriceVariationDirection.Neutral -> Unit
            }
            Text(
                text = item.formattedVariation(),
                color = variationColor,
                style = PricePctTextStyle,
            )
        }
    }
}

@Composable
private fun FavouriteIconButton(
    isFavourite: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isFavourite,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 120)) + scaleIn(tween(durationMillis = 120), initialScale = 0.82f)) togetherWith
                    (fadeOut(tween(durationMillis = 90)) + scaleOut(tween(durationMillis = 90), targetScale = 0.82f)) using
                    SizeTransform(clip = false)
            },
            label = "favourite-icon",
        ) { favourite ->
            Icon(
                imageVector = if (favourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = if (favourite) "Quitar favorito" else "Agregar favorito",
                tint = if (favourite) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecuritiesFilterSheet(
    filters: SecurityFilters,
    sectorOptions: List<String>,
    onFiltersChange: (SecurityFilters) -> Unit,
    onDismiss: () -> Unit,
    onClearFilters: () -> Unit,
    onApply: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Filtrar",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Ordenar por",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))

        SecuritySortOption.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = filters.sortOption == option,
                    onClick = { onFiltersChange(filters.copy(sortOption = option)) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        FilterSection(title = "Moneda") {
            SecurityCurrencyFilters.forEach { currency ->
                FilterChip(
                    selected = currency in filters.currencies,
                    onClick = {
                        onFiltersChange(
                            filters.copy(currencies = filters.currencies.toggleSingleSelection(currency)),
                        )
                    },
                    label = { Text(currency) },
                )
            }
        }

        FilterSection(title = "Panel") {
            SecurityPanelFilters.forEach { panel ->
                FilterChip(
                    selected = panel in filters.panels,
                    onClick = {
                        onFiltersChange(
                            filters.copy(panels = filters.panels.toggleSingleSelection(panel)),
                        )
                    },
                    label = { Text(panel) },
                )
            }
        }

        FilterSection(title = "Sector") {
            sectorOptions.forEach { sector ->
                FilterChip(
                    selected = sector in filters.sectors,
                    onClick = {
                        onFiltersChange(
                            filters.copy(sectors = filters.sectors.toggleSingleSelection(sector)),
                        )
                    },
                    label = { Text(sector) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onClearFilters,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Borrar")
            }
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Aplicar")
            }
        }
    }
}

private val SecurityCurrencyFilters = listOf("ARS", "USD")
private val SecurityPanelFilters = listOf("S&P Merval", "General")

private fun Set<String>.toggleSingleSelection(value: String): Set<String> =
    if (value in this) {
        emptySet()
    } else {
        setOf(value)
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 16.dp),
    )
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = { content() },
    )
}

private fun avatarColor(symbol: String): Color =
    AvatarPalette[(symbol.hashCode() and 0x7FFFFFFF) % AvatarPalette.size]

private enum class PriceVariationDirection {
    Up,
    Down,
    Neutral,
}

private fun Security.variationDirection(): PriceVariationDirection =
    when {
        priceChange > BigDecimal.ZERO -> PriceVariationDirection.Up
        priceChange < BigDecimal.ZERO -> PriceVariationDirection.Down
        else -> PriceVariationDirection.Neutral
    }

private fun Security.formattedPrice(): String =
    "$ ${price.setScale(2, RoundingMode.HALF_UP)}"

private fun Security.formattedVariation(): String {
    val roundedPriceChange = priceChange.abs().setScale(2, RoundingMode.HALF_UP)
    val roundedPercentageChange = dailyVariationPercent.abs().setScale(2, RoundingMode.HALF_UP)

    return "$roundedPriceChange ($roundedPercentageChange%)"
}
