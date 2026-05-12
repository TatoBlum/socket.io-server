package com.example.socketapp

import java.math.BigDecimal

object TradeInputParser {
    fun sanitizeWholeNumberInput(input: String): String =
        input.filter { character -> character.isDigit() || character == '.' }

    fun sanitizeLimitPriceInput(input: String): String =
        input.filter { character -> character.isDigit() || character == '.' || character == ',' }

    fun formatLimitPriceInput(input: String): String {
        val sanitizedInput = sanitizeLimitPriceInput(input)
        val integerPart = sanitizedInput.substringBefore(',').filter { character -> character.isDigit() }
        val decimalPart = sanitizedInput.substringAfter(',', missingDelimiterValue = "")
            .filter { character -> character.isDigit() }
        val formattedIntegerPart = integerPart.formatThousands()

        return when {
            sanitizedInput.contains(',') -> "$formattedIntegerPart,$decimalPart"
            else -> formattedIntegerPart
        }
    }

    fun parseWholeNumberInput(input: String): BigDecimal? =
        input
            .filter { character -> character.isDigit() }
            .takeIf { digits -> digits.isNotBlank() }
            ?.toBigDecimalOrNull()

    fun parseLimitPriceInput(input: String): BigDecimal? =
        input
            .replace(".", "")
            .replace(",", ".")
            .takeIf { value -> value.isNotBlank() }
            ?.toBigDecimalOrNull()

    private fun String.formatThousands(): String =
        reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
}
