package com.example.socketapp.model

import com.google.gson.Gson
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SecurityJsonTest {
    private val gson = Gson()

    @Test
    fun `security parses decimal strings from json and exposes big decimals`() {
        val json = """
            {
              "id": "AAPL-0",
              "symbol": "AAPL",
              "name": "Apple",
              "price": "1234.567890123",
              "priceChange": "-8.125",
              "percentageChange": "-0.6579",
              "currency": "Dollars",
              "panel": "General",
              "sector": "Technology",
              "isFavourite": true
            }
        """.trimIndent()

        val security = gson.fromJson(json, Security::class.java)

        assertNotNull(security)
        assertEquals("1234.567890123", security.rawPrice)
        assertEquals("-8.125", security.rawPriceChange)
        assertEquals("-0.6579", security.rawPercentageChange)
        assertEquals(BigDecimal("1234.567890123"), security.price)
        assertEquals(BigDecimal("-8.125"), security.priceChange)
        assertEquals(BigDecimal("-0.6579"), security.percentageChange)
    }
}
