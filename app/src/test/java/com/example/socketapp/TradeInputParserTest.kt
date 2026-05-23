package com.example.socketapp

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class TradeInputParserTest {
    @Test
    fun `whole number input parses thousand dots as separators`() {
        assertEquals(BigDecimal("19000"), TradeInputParser.parseWholeNumberInput("19.000"))
    }

    @Test
    fun `whole number input treats dot as thousand separator`() {
        assertEquals(BigDecimal("3130"), TradeInputParser.parseWholeNumberInput("313.0"))
    }

    @Test
    fun `whole number input parses formatted thousands`() {
        assertEquals(BigDecimal("31313"), TradeInputParser.parseWholeNumberInput("31.313"))
    }

    @Test
    fun `amount input parses thousand dots as separators`() {
        assertEquals(BigDecimal("19000"), TradeInputParser.parseAmountInput("19.000"))
    }

    @Test
    fun `amount input parses comma decimals`() {
        assertEquals(BigDecimal("19000.25"), TradeInputParser.parseAmountInput("19.000,25"))
    }

    @Test
    fun `amount input treats dot as thousand separator`() {
        assertEquals(BigDecimal("3135"), TradeInputParser.parseAmountInput("313.5"))
    }

    @Test
    fun `amount input parses comma decimal digits`() {
        assertEquals(BigDecimal("313.25"), TradeInputParser.parseAmountInput("313,25"))
    }

    @Test
    fun `amount input treats dot zero as thousand separator`() {
        assertEquals(BigDecimal("3130"), TradeInputParser.parseAmountInput("313.0"))
    }

    @Test
    fun `amount input keeps trailing decimal separator as whole value`() {
        assertEquals(BigDecimal("313"), TradeInputParser.parseAmountInput("313,"))
    }

    @Test
    fun `limit price parses thousand dots as separators`() {
        assertEquals(BigDecimal("19210"), TradeInputParser.parseLimitPriceInput("19.210"))
    }

    @Test
    fun `limit price parses comma decimals`() {
        assertEquals(BigDecimal("19210.25"), TradeInputParser.parseLimitPriceInput("19.210,25"))
    }

    @Test
    fun `limit price parses comma decimals without thousand separator`() {
        assertEquals(BigDecimal("19210.25"), TradeInputParser.parseLimitPriceInput("19210,25"))
    }

    @Test
    fun `limit price treats dot as thousand separator`() {
        assertEquals(BigDecimal("14089"), TradeInputParser.parseLimitPriceInput("140.89"))
    }

    @Test
    fun `limit price sanitizer keeps only digits thousand dots and decimal comma`() {
        assertEquals("19.210,25", TradeInputParser.sanitizeLimitPriceInput("abc19.210,25$"))
    }

    @Test
    fun `limit price formatter adds thousand dots`() {
        assertEquals("19.210", TradeInputParser.formatLimitPriceInput("19210"))
    }

    @Test
    fun `limit price formatter keeps comma decimals`() {
        assertEquals("19.210,25", TradeInputParser.formatLimitPriceInput("19210,25"))
    }

    @Test
    fun `limit price formatter truncates extra comma decimals`() {
        assertEquals("1.000,00", TradeInputParser.formatLimitPriceInput("1000,0001"))
    }

    @Test
    fun `limit price formatter treats dot as thousand separator`() {
        assertEquals("14.089", TradeInputParser.formatLimitPriceInput("140.89"))
    }

    @Test
    fun `limit price formatter ignores dot decimals as decimals`() {
        assertEquals("10.000.001", TradeInputParser.formatLimitPriceInput("1000.0001"))
    }

    @Test
    fun `limit price formatter keeps trailing decimal comma`() {
        assertEquals("19.210,", TradeInputParser.formatLimitPriceInput("19210,"))
    }

    @Test
    fun `limit price parser returns null for blank input`() {
        assertEquals(null, TradeInputParser.parseLimitPriceInput(""))
    }
}
