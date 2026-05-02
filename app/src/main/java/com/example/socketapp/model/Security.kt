package com.example.socketapp.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
@Immutable
data class Security(
    val id: String,
    val symbol: String,
    val name: String,
    @Json(name = "price") val rawPrice: String,
    @Json(name = "priceChange") val rawPriceChange: String,
    @Json(name = "percentageChange") val rawPercentageChange: String,
    val currency: SecurityCurrency,
    val panel: SecurityPanel,
    val sector: SecuritySector,
    val isFavourite: Boolean = false,
) {
    val price: BigDecimal = rawPrice.toBigDecimal()
    val priceChange: BigDecimal = rawPriceChange.toBigDecimal()
    val percentageChange: BigDecimal = rawPercentageChange.toBigDecimal()
}

enum class SecurityCurrency(val label: String) {
    Pesos("Pesos"),
    Dollars("Dolares"),
}

enum class SecurityPanel(val label: String) {
    Merval("S&P Merval"),
    General("General"),
}

enum class SecuritySector(val label: String) {
    Energy("Energia"),
    Technology("Tecnologia"),
    Ai("IA"),
    Industrial("Industriales"),
    Transport("Transporte"),
    Finance("Finanzas"),
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
