package com.example.socketapp.data

import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSecuritiesRepositoryTest {
    @Test
    fun `getBuyableInstrument refreshes cache and matches by ticker when code value omits suffix`() = runTest {
        val repository = MockSecuritiesRepository()

        val instrument = repository.getBuyableInstrument(
            codeType = "MOCK_SECURITY_ID",
            codeValue = "PAMP",
        )

        assertEquals("PAMP-0", instrument?.codeValue)
        assertEquals("PAMP", instrument?.ticker)
    }

    @Test
    fun `getBuyableInstrument returns tradeable fallback when remote data has no match`() = runTest {
        val repository = MockSecuritiesRepository()

        val instrument = repository.getBuyableInstrument(
            codeType = "UNKNOWN_TYPE",
            codeValue = "UNKNOWN_VALUE",
        )

        assertNotNull(instrument)
        requireNotNull(instrument)
        assertEquals("UNKNOWN_TYPE", instrument.codeType)
        assertEquals("UNKNOWN_VALUE", instrument.codeValue)
        assertTrue(instrument.hasRequiredTradingConfiguration)
        assertTrue(instrument.holdingQuantity > 0)
        assertTrue(instrument.askPrice00 > BigDecimal.ZERO)
        assertTrue(instrument.bidPrice00 > BigDecimal.ZERO)
    }

    @Test
    fun `clearCache removes cached securities`() = runTest {
        val repository = MockSecuritiesRepository()

        repository.refreshSecurities()

        assertNotNull(repository.getCachedSecurities())

        repository.clearCache()

        assertEquals(null, repository.getCachedSecurities())
    }
}
