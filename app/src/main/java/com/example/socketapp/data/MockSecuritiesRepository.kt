package com.example.socketapp.data

import com.example.socketapp.Security
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

    override suspend fun getBuyableInstruments(): List<Security> =
        cachedSecurities.orEmpty()

    override suspend fun getBuyableInstrument(id: String): Security? =
        cachedSecurities.orEmpty()
            .firstOrNull { security -> security.codeValue == id }
}
