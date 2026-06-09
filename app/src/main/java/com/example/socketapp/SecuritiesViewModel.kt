package com.example.socketapp

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socketapp.data.MockSecuritiesRepository
import com.example.socketapp.di.DefaultDispatcher
import com.example.socketapp.di.IoDispatcher
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
private const val FAVORITES_SYNC_DEBOUNCE_MS = 800L

data class SecuritiesUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filters: SecurityFilters = SecurityFilters(),
    val items: List<Security> = emptyList(),
    val sectorOptions: List<String> = emptyList(),
    val errorState: Pair<Boolean, String?> = false to null,
)

@HiltViewModel
class SecuritiesViewModel @Inject constructor(
    private val repository: MockSecuritiesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    var uiState by mutableStateOf(SecuritiesUiState())
        private set

    private var allSecurities: List<Security> = emptyList()
    private val favouriteOverrides = mutableMapOf<String, Boolean>()
    private var pendingFavoriteToggles: Set<String> = emptySet()
    private var favoritesSyncInFlight = false
    private var pollingJob: Job? = null
    private var filterJob: Job? = null
    private var favoritesSyncJob: Job? = null

    init {
        loadSecuritySectors()
        loadInitialSecurities()
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

    fun onFavouriteClick(ticker: String) {
        val selectedSecurity = allSecurities.firstOrNull { security -> security.ticker == ticker } ?: return
        val newFavourite = !selectedSecurity.isFavorite

        favouriteOverrides[ticker] = newFavourite
        pendingFavoriteToggles = pendingFavoriteToggles.toggled(ticker)

        allSecurities = allSecurities.map { security ->
            if (security.ticker == ticker) {
                security.copy(isFavorite = newFavourite)
            } else {
                security
            }
        }
        applyFilters()

        if (pendingFavoriteToggles.isEmpty()) {
            if (!favoritesSyncInFlight) {
                favoritesSyncJob?.cancel()
            }
        } else {
            scheduleFavoritesSync()
        }
    }

    fun startPollingIfNeeded() {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(SECURITIES_POLLING_INTERVAL_MS)
                refreshSecurities()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun flushFavorites() {
        if (favoritesSyncInFlight) return

        favoritesSyncJob?.cancel()
        favoritesSyncJob = viewModelScope.launch {
            syncFavoritesIfNeeded()
        }
    }

    private fun loadInitialSecurities() {
        val cachedSecurities = repository.getCachedSecurities()
        if (cachedSecurities == null) {
            viewModelScope.launch {
                refreshSecurities()
            }
            return
        }

        allSecurities = applyFavouriteOverrides(cachedSecurities)
        applyFilters(
            isLoading = false,
            errorState = false to null,
        )
    }

    private fun loadSecuritySectors() {
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    repository.refreshSecuritySectors()
                }
            }.onSuccess { sectors ->
                uiState = uiState.copy(sectorOptions = sectors.distinct())
            }
        }
    }

    private suspend fun refreshSecurities() {
        if (allSecurities.isEmpty()) {
            uiState = uiState.copy(isLoading = true, errorState = false to null)
        }

        runCatching {
            withContext(ioDispatcher) {
                repository.refreshSecurities()
            }
        }.onSuccess { securities ->
            allSecurities = applyFavouriteOverrides(securities)
            applyFilters(
                isLoading = false,
                errorState = false to null,
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error

            uiState = uiState.copy(
                isLoading = false,
                errorState = true to (error.message ?: "No se pudieron actualizar los securities"),
            )
        }
    }

    private fun scheduleFavoritesSync() {
        if (favoritesSyncInFlight) return

        favoritesSyncJob?.cancel()
        favoritesSyncJob = viewModelScope.launch {
            delay(FAVORITES_SYNC_DEBOUNCE_MS)
            syncFavoritesIfNeeded()
        }
    }

    private suspend fun syncFavoritesIfNeeded() {
        if (pendingFavoriteToggles.isEmpty() || favoritesSyncInFlight) return

        val togglesToSync = pendingFavoriteToggles
        pendingFavoriteToggles = emptySet()
        favoritesSyncInFlight = true

        val result = runCatching {
            withContext(ioDispatcher) {
                repository.toggleFavorites(togglesToSync.toList())
            }
        }
        favoritesSyncInFlight = false

        result.onSuccess {
            if (pendingFavoriteToggles.isNotEmpty()) {
                scheduleFavoritesSync()
            }
        }.onFailure {
            pendingFavoriteToggles = pendingFavoriteToggles.withToggles(togglesToSync)
        }
    }

    private fun applyFavouriteOverrides(securities: List<Security>): List<Security> =
        securities.map { security ->
            val favouriteOverride = favouriteOverrides[security.ticker]
            security.copy(isFavorite = favouriteOverride ?: security.isFavorite)
        }

    private fun Set<String>.toggled(ticker: String): Set<String> =
        if (ticker in this) this - ticker else this + ticker

    private fun Set<String>.withToggles(tickers: Set<String>): Set<String> =
        tickers.fold(this) { favorites, ticker -> favorites.toggled(ticker) }

    private fun applyFilters(
        isLoading: Boolean = uiState.isLoading,
        errorState: Pair<Boolean, String?> = uiState.errorState,
    ) {
        filterJob?.cancel()
        val query = uiState.searchQuery.trim()
        val filters = uiState.filters
        val securities = allSecurities

        filterJob = viewModelScope.launch {
            val filteredItems = filterSecurities(
                query = query,
                filters = filters,
                securities = securities,
            )

            uiState = uiState.copy(
                isLoading = isLoading,
                items = filteredItems,
                errorState = errorState,
            )
        }
    }

    private suspend fun filterSecurities(
        query: String,
        filters: SecurityFilters,
        securities: List<Security>,
    ): List<Security> = withContext(defaultDispatcher) {
        val filteredSecurities: List<Security> = securities.filter { security ->
            val matchesQuery = query.isBlank() ||
                    security.ticker.contains(query, ignoreCase = true) ||
                    security.description.contains(query, ignoreCase = true)
            val matchesCurrency = filters.currencies.isEmpty() || security.currency in filters.currencies
            val matchesPanel = filters.panels.isEmpty() || security.panel in filters.panels
            val matchesSector = filters.sectors.isEmpty() || security.industry in filters.sectors

            matchesQuery && matchesCurrency && matchesPanel && matchesSector
        }

        when (filters.sortOption) {
            SecuritySortOption.HighestYield -> filteredSecurities.sortedByDescending { it.dailyVariationPercent }
            SecuritySortOption.LowestYield -> filteredSecurities.sortedBy { it.dailyVariationPercent }
            SecuritySortOption.HighestPrice -> filteredSecurities.sortedByDescending { it.price }
            SecuritySortOption.LowestPrice -> filteredSecurities.sortedBy { it.price }
        }
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
