package com.example.socketapp.data

import com.example.socketapp.BuyableInstrument
import com.example.socketapp.model.Security
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class MockSecuritiesRepository @Inject constructor(
    private val remoteDataSource: SecuritiesRemoteDataSource,
) : SecuritiesRepository {
    private var cachedSecurities: List<Security>? = null

    override fun getCachedSecurities(): List<Security>? = cachedSecurities

    override suspend fun refreshSecurities(): List<Security> {
        val refreshedSecurities = remoteDataSource.fetchSecurities(cachedSecurities)
        cachedSecurities = refreshedSecurities
        return refreshedSecurities
    }

    override suspend fun getBuyableInstruments(): List<BuyableInstrument> =
        cachedSecurities.orEmpty().map { security -> security.toBuyableInstrument() }

    override suspend fun getBuyableInstrument(id: String): BuyableInstrument? =
        cachedSecurities.orEmpty()
            .firstOrNull { security -> security.id == id }
            ?.toBuyableInstrument()

    private fun Security.toBuyableInstrument(): BuyableInstrument {
        val normalizedCurrency = when (currency.lowercase()) {
            "dolares" -> "USD"
            else -> "ARS"
        }
        val spread = price.multiply(BigDecimal("0.0015")).setScale(2, RoundingMode.HALF_UP)
        val ask = price.add(spread).setScale(2, RoundingMode.HALF_UP)
        val bid = price.subtract(spread).coerceAtLeast(BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP)

        return BuyableInstrument(
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
            holdingQuantity = 10,
            minInstrumentNominals = 1,
            lotInstrumentSize = 1,
            minTradeNominals = 1,
            lastPrice = price.setScale(2, RoundingMode.HALF_UP),
            dailyVariationPercent = percentageChange.setScale(2, RoundingMode.HALF_UP),
            askPrice = ask,
            bidPrice = bid,
            percentageMovement = BigDecimal("0.15"),
        )
    }
}
