package com.example.socketapp.data

import com.example.socketapp.Security as BuyableSecurity
import com.example.socketapp.model.Security as MarketSecurity
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class MockSecuritiesRepository @Inject constructor(
    private val remoteDataSource: SecuritiesRemoteDataSource,
) : SecuritiesRepository {
    private var cachedSecurities: List<MarketSecurity>? = null

    override fun getCachedSecurities(): List<MarketSecurity>? = cachedSecurities

    override suspend fun refreshSecurities(): List<MarketSecurity> {
        val refreshedSecurities = remoteDataSource.fetchSecurities(cachedSecurities)
        cachedSecurities = refreshedSecurities
        return refreshedSecurities
    }

    override suspend fun getBuyableInstruments(): List<BuyableSecurity> =
        cachedSecurities.orEmpty().map { security -> security.toBuyableInstrument() }

    override suspend fun getBuyableInstrument(id: String): BuyableSecurity? =
        cachedSecurities.orEmpty()
            .firstOrNull { security -> security.id == id }
            ?.toBuyableInstrument()

    private fun MarketSecurity.toBuyableInstrument(): BuyableSecurity {
        val normalizedCurrency = when (currency.lowercase()) {
            "dolares" -> "USD"
            else -> "ARS"
        }
        return BuyableSecurity(
            id = id.hashCode() and Int.MAX_VALUE,
            ticker = symbol,
            description = name,
            type = "Acciones",
            currency = normalizedCurrency,
            codeType = "MOCK_SECURITY_ID",
            codeValue = id,
            industry = sector,
            liderMerval = panel == "S&P Merval",
            indexationType = null,
            isFavorite = isFavourite,
            minInstrumentNominals = 1,
            maxInstrumentNominals = 999999999,
            lotInstrumentSize = 1,
            holdingQuantity = 0,
            price = price.setScale(2, RoundingMode.HALF_UP),
            dailyVariationPercent = percentageChange.setScale(2, RoundingMode.HALF_UP),
            askPrice00 = askPrice00,
            askPrice24 = askPrice24,
            bidPrice00 = bidPrice00,
            bidPrice24 = bidPrice24,
            percentageMovement = BigDecimal("0.015"),
            minBuyArsAmount = minBuyArsAmount,
        )
    }
}
