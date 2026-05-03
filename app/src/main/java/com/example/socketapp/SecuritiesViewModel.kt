package com.example.socketapp

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socketapp.data.SecuritiesRepository
import com.example.socketapp.model.Security
import com.example.socketapp.model.SecurityCurrency
import com.example.socketapp.model.SecurityPanel
import com.example.socketapp.model.SecuritySector
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
    val items: List<Security> = emptyList(),
    val errorMessage: String? = null,
)

class SecuritiesViewModel(
    private val repository: SecuritiesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    var uiState by mutableStateOf(SecuritiesUiState())
        private set

    private var allSecurities: List<Security> = emptyList()
    private val pendingFavouriteUpdates = mutableMapOf<String, Boolean>()

    init {
        loadSecurities()
        startPolling()
    }

    fun onSearchQueryChange(query: String) {
        uiState = uiState.copy(searchQuery = query)
        applyFilters()
    }

    fun applyFilters(filters: SecurityFilters) {
        uiState = uiState.copy(filters = filters)
        applyFilters()
    }

    fun clearFilters() {
        uiState = uiState.copy(filters = SecurityFilters())
        applyFilters()
    }

    fun onFavouriteClick(securityId: String) {
        val selectedSecurity = allSecurities.firstOrNull { security -> security.id == securityId } ?: return
        val newFavourite = !selectedSecurity.isFavourite

        pendingFavouriteUpdates[securityId] = newFavourite
        allSecurities = allSecurities.map { security ->
            if (security.id == securityId) {
                security.copy(isFavourite = newFavourite)
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
            allSecurities = securities.map { security ->
                val pendingFavourite = pendingFavouriteUpdates[security.id]

                clearPendingFavouriteUpdateIfServerIsSynced(security, pendingFavourite)

                security.copy(isFavourite = pendingFavourite ?: security.isFavourite)
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

    private fun clearPendingFavouriteUpdateIfServerIsSynced(
        security: Security,
        pendingFavourite: Boolean?,
    ) {
        if (pendingFavourite != null && security.isFavourite == pendingFavourite) {
            pendingFavouriteUpdates.remove(security.id)
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
            val filteredSecurities: List<Security> = securities.filter { security ->
                val matchesQuery = query.isBlank() ||
                        security.symbol.contains(query, ignoreCase = true) ||
                        security.name.contains(query, ignoreCase = true)
                val matchesCurrency = filters.currencies.isEmpty() || security.currency in filters.currencies
                val matchesPanel = filters.panels.isEmpty() || security.panel in filters.panels
                val matchesSector = filters.sectors.isEmpty() || security.sector in filters.sectors

                matchesQuery && matchesCurrency && matchesPanel && matchesSector
            }

            val sortedSecurities = when (filters.sortOption) {
                SecuritySortOption.HighestYield -> filteredSecurities.sortedByDescending { it.percentageChange }
                SecuritySortOption.LowestYield -> filteredSecurities.sortedBy { it.percentageChange }
                SecuritySortOption.HighestPrice -> filteredSecurities.sortedByDescending { it.price }
                SecuritySortOption.LowestPrice -> filteredSecurities.sortedBy { it.price }
            }

            sortedSecurities
        }

        uiState = uiState.copy(items = filteredItems)
    }
}

enum class SecuritySortOption(val label: String) {
    HighestYield("Mayor rendimiento"),
    LowestYield("Menor rendimiento"),
    HighestPrice("Mayor precio"),
    LowestPrice("Menor precio"),
}

@Immutable
data class SecurityFilters(
    val sortOption: SecuritySortOption = SecuritySortOption.HighestYield,
    val currencies: Set<SecurityCurrency> = emptySet(),
    val panels: Set<SecurityPanel> = emptySet(),
    val sectors: Set<SecuritySector> = emptySet(),
)
