package com.example.socketapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socketapp.data.SecuritiesRepository
import com.example.socketapp.model.Security
import com.example.socketapp.model.SecurityCurrency
import com.example.socketapp.model.SecurityFilters
import com.example.socketapp.model.SecurityPanel
import com.example.socketapp.model.SecuritySector
import com.example.socketapp.model.SecuritySortOption
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SecuritiesUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filters: SecurityFilters = SecurityFilters(),
    val items: List<SecurityUiItem> = emptyList(),
    val errorMessage: String? = null,
)

data class SecurityUiItem(
    val id: String,
    val symbol: String,
    val name: String,
    val price: String,
    val variation: String,
    val variationDirection: PriceVariationDirection,
    val isFavourite: Boolean,
)

enum class PriceVariationDirection {
    Up,
    Down,
    Neutral,
}

class SecuritiesViewModel(
    private val repository: SecuritiesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    var uiState by mutableStateOf(SecuritiesUiState())
        private set

    private var allSecurities: List<Security> = emptyList()

    init {
        loadSecurities()
        startPolling()
    }

    fun onSearchQueryChange(query: String) {
        uiState = uiState.copy(searchQuery = query)
        applyFilters()
    }

    fun onSortOptionChange(option: SecuritySortOption) {
        uiState = uiState.copy(filters = uiState.filters.copy(sortOption = option))
        applyFilters()
    }

    fun onCurrencyToggle(currency: SecurityCurrency) {
        uiState = uiState.copy(
            filters = uiState.filters.copy(
                currencies = uiState.filters.currencies.toggle(currency),
            ),
        )
        applyFilters()
    }

    fun onPanelToggle(panel: SecurityPanel) {
        uiState = uiState.copy(
            filters = uiState.filters.copy(
                panels = uiState.filters.panels.toggle(panel),
            ),
        )
        applyFilters()
    }

    fun onSectorToggle(sector: SecuritySector) {
        uiState = uiState.copy(
            filters = uiState.filters.copy(
                sectors = uiState.filters.sectors.toggle(sector),
            ),
        )
        applyFilters()
    }

    fun clearFilters() {
        uiState = uiState.copy(filters = SecurityFilters())
        applyFilters()
    }

    fun onFavouriteClick(securityId: String) {
        allSecurities = allSecurities.map { security ->
            if (security.id == securityId) {
                security.copy(isFavourite = !security.isFavourite)
            } else {
                security
            }
        }
        applyFilters()
    }

    private fun loadSecurities() {
        viewModelScope.launch {
            refreshSecurities(showLoading = true)
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                delay(60_000)
                refreshSecurities(showLoading = false)
            }
        }
    }

    private suspend fun refreshSecurities(showLoading: Boolean) {
        if (showLoading) {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
        }

        runCatching {
            withContext(ioDispatcher) {
                repository.getSecurities()
            }
        }.onSuccess { securities ->
            val favouriteIds = allSecurities
                .asSequence()
                .filter { security -> security.isFavourite }
                .map { security -> security.id }
                .toSet()

            allSecurities = securities.map { security ->
                security.copy(isFavourite = security.isFavourite || security.id in favouriteIds)
            }
            applyFiltersImmediately()
            uiState = uiState.copy(isLoading = false, errorMessage = null)
        }.onFailure { error ->
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = error.message ?: "No se pudieron actualizar los securities",
            )
        }
    }

    private fun applyFilters() {
        viewModelScope.launch {
            applyFiltersImmediately()
        }
    }

    private suspend fun applyFiltersImmediately() {
        val query = uiState.searchQuery.trim()
        val filters = uiState.filters
        val securities = allSecurities

        val filteredItems = withContext(defaultDispatcher) {
            securities
                .asSequence()
                .filter { security ->
                    query.isBlank() ||
                        security.symbol.contains(query, ignoreCase = true) ||
                        security.name.contains(query, ignoreCase = true)
                }
                .filter { security -> filters.currencies.isEmpty() || security.currency in filters.currencies }
                .filter { security -> filters.panels.isEmpty() || security.panel in filters.panels }
                .filter { security -> filters.sectors.isEmpty() || security.sector in filters.sectors }
                .let { sequence ->
                    when (filters.sortOption) {
                        SecuritySortOption.HighestYield -> sequence.sortedByDescending { it.percentageChange }
                        SecuritySortOption.LowestYield -> sequence.sortedBy { it.percentageChange }
                        SecuritySortOption.HighestPrice -> sequence.sortedByDescending { it.price }
                        SecuritySortOption.LowestPrice -> sequence.sortedBy { it.price }
                    }
                }
                .map { security -> security.toUiItem() }
                .toList()
        }

        uiState = uiState.copy(items = filteredItems)
    }
}

private fun Security.toUiItem(): SecurityUiItem {
    val direction = when {
        priceChange > BigDecimal.ZERO -> PriceVariationDirection.Up
        priceChange < BigDecimal.ZERO -> PriceVariationDirection.Down
        else -> PriceVariationDirection.Neutral
    }
    val sign = if (priceChange > BigDecimal.ZERO) "+" else ""

    return SecurityUiItem(
        id = id,
        symbol = symbol,
        name = name,
        price = "$ ${price.setScale(2, RoundingMode.HALF_UP)}",
        variation = "$sign${priceChange.setScale(2, RoundingMode.HALF_UP)} ($sign${
            percentageChange.setScale(2, RoundingMode.HALF_UP)
        }%)",
        variationDirection = direction,
        isFavourite = isFavourite,
    )
}

private fun <T> Set<T>.toggle(value: T): Set<T> =
    if (value in this) this - value else this + value
