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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.socketapp.SecurityFilters
import com.example.socketapp.SecuritySortOption
import com.example.socketapp.model.Security
import com.example.socketapp.model.SecurityCurrency
import com.example.socketapp.model.SecurityPanel
import com.example.socketapp.model.SecuritySector
import com.example.socketapp.ui.theme.AvatarInitial
import com.example.socketapp.ui.theme.GaliciaAvatarPalette
import com.example.socketapp.ui.theme.PriceDown
import com.example.socketapp.ui.theme.PricePctTextStyle
import com.example.socketapp.ui.theme.PriceTextStyle
import com.example.socketapp.ui.theme.PriceUp
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun SecuritiesRoute(
    viewModel: SecuritiesViewModel,
    modifier: Modifier = Modifier,
) {
    SecuritiesScreen(
        uiState = viewModel.uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onApplyFilters = viewModel::applyFilters,
        onClearFilters = viewModel::clearFilters,
        onFavouriteClick = viewModel::onFavouriteClick,
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

        if (uiState.items.isEmpty()) {
            val message = when {
                uiState.searchQuery.isNotBlank() -> "Sin resultados"
                uiState.isLoading -> "Cargando acciones..."
                else -> "No hay acciones disponibles"
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
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = uiState.items,
                key = { item -> item.id },
            ) { item ->
                SecurityItem(
                    item = item,
                    onFavouriteClick = { onFavouriteClick(item.id) },
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
                onFiltersChange = { draftFilters = it },
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
private fun SecurityItem(
    item: Security,
    onFavouriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFavouriteIcon: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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
                    .background(avatarColor(item.symbol)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.symbol.take(2),
                    color = AvatarInitial,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.symbol,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.name,
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
                    isFavourite = item.isFavourite,
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
            Spacer(modifier = Modifier.width(2.dp))
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
    onFiltersChange: (SecurityFilters) -> Unit,
    onClearFilters: () -> Unit,
    onApply: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Text(
            text = "Filtrar",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

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
            SecurityCurrency.entries.forEach { currency ->
                FilterChip(
                    selected = currency in filters.currencies,
                    onClick = { onFiltersChange(filters.copy(currencies = setOf(currency))) },
                    label = { Text(currency.label) },
                )
            }
        }

        FilterSection(title = "Panel") {
            SecurityPanel.entries.forEach { panel ->
                FilterChip(
                    selected = panel in filters.panels,
                    onClick = { onFiltersChange(filters.copy(panels = setOf(panel))) },
                    label = { Text(panel.label) },
                )
            }
        }

        FilterSection(title = "Sector") {
            SecuritySector.entries.forEach { sector ->
                FilterChip(
                    selected = sector in filters.sectors,
                    onClick = { onFiltersChange(filters.copy(sectors = setOf(sector))) },
                    label = { Text(sector.label) },
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
    GaliciaAvatarPalette[(symbol.hashCode() and 0x7FFFFFFF) % GaliciaAvatarPalette.size]

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
    val sign = if (priceChange > BigDecimal.ZERO) "+" else ""

    return "$sign${priceChange.setScale(2, RoundingMode.HALF_UP)} ($sign${
        percentageChange.setScale(2, RoundingMode.HALF_UP)
    }%)"
}
