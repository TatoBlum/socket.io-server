package com.example.socketapp

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socketapp.data.SecuritiesRepository
import com.example.socketapp.di.DefaultDispatcher
import com.example.socketapp.di.IoDispatcher
import com.example.socketapp.model.Security
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SECURITIES_POLLING_INTERVAL_MS = 60_000L

data class SecuritiesUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filters: SecurityFilters = SecurityFilters(),
    val items: List<Security> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class SecuritiesViewModel @Inject constructor(
    private val repository: SecuritiesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    var uiState by mutableStateOf(SecuritiesUiState())
        private set

    private var allSecurities: List<Security> = emptyList()
    private val favouriteOverrides = mutableMapOf<String, Boolean>()
    private var pollingJob: Job? = null
    private var filterJob: Job? = null

    init {
        loadCachedSecuritiesIfAny()
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

        favouriteOverrides[securityId] = newFavourite
        allSecurities = allSecurities.map { security ->
            if (security.id == securityId) {
                security.copy(isFavourite = newFavourite)
            } else {
                security
            }
        }
        applyFilters()
    }

    fun startPollingIfNeeded() {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                refreshSecurities()
                delay(SECURITIES_POLLING_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun loadCachedSecuritiesIfAny() {
        val cachedSecurities = repository.getCachedSecurities() ?: return
        allSecurities = applyFavouriteOverrides(cachedSecurities)
        uiState = uiState.copy(isLoading = false, errorMessage = null)
        applyFilters()
    }

    private suspend fun refreshSecurities() {
        if (allSecurities.isEmpty()) {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
        }

        runCatching {
            withContext(ioDispatcher) {
                repository.refreshSecurities()
            }
        }.onSuccess { securities ->
            allSecurities = applyFavouriteOverrides(securities)
            uiState = uiState.copy(isLoading = false, errorMessage = null)
            applyFilters()
        }.onFailure { error ->
            if (error is CancellationException) throw error

            uiState = uiState.copy(
                isLoading = false,
                errorMessage = error.message ?: "No se pudieron actualizar los securities",
            )
        }
    }

    private fun applyFavouriteOverrides(securities: List<Security>): List<Security> =
        securities.map { security ->
            val favouriteOverride = favouriteOverrides[security.id]
            security.copy(isFavourite = favouriteOverride ?: security.isFavourite)
        }

    private fun applyFilters() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
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
    val currencies: Set<String> = emptySet(),
    val panels: Set<String> = emptySet(),
    val sectors: Set<String> = emptySet(),
)
