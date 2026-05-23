package com.example.socketapp

import java.math.BigDecimal

object TradeInputParser {
    private const val LIMIT_PRICE_MAX_DECIMALS = 2

    fun sanitizeWholeNumberInput(input: String): String =
        input.filter { character -> character.isDigit() || character == '.' }

    fun sanitizeAmountInput(input: String): String =
        sanitizeDecimalInput(input)

    fun sanitizeLimitPriceInput(input: String): String =
        sanitizeDecimalInput(input)

    fun formatLimitPriceInput(input: String): String {
        val sanitizedInput = sanitizeLimitPriceInput(input)
        val hasDecimalSeparator = sanitizedInput.hasCommaDecimalSeparator()
        val integerPart = sanitizedInput.integerPartDigits()
        val decimalPart = sanitizedInput.decimalPartDigits().take(LIMIT_PRICE_MAX_DECIMALS)
        val formattedIntegerPart = integerPart.formatThousands()

        return if (hasDecimalSeparator) {
            "$formattedIntegerPart,$decimalPart"
        } else {
            formattedIntegerPart
        }
    }

    fun parseWholeNumberInput(input: String): BigDecimal? =
        input
            .onlyDigits()
            .takeIf { digits -> digits.isNotBlank() }
            ?.toBigDecimalOrNull()

    fun parseAmountInput(input: String): BigDecimal? {
        val sanitizedInput = sanitizeAmountInput(input)
        return sanitizedInput
            .toCommaDecimalNumber()
            .takeIf { value -> value.isNotBlank() }
            ?.toBigDecimalOrNull()
            ?.toPlainNormalizedBigDecimal()
    }

    fun parseLimitPriceInput(input: String): BigDecimal? {
        val sanitizedInput = sanitizeLimitPriceInput(input)
        return sanitizedInput
            .toCommaDecimalNumber()
            .takeIf { value -> value.isNotBlank() }
            ?.toBigDecimalOrNull()
    }

    private fun sanitizeDecimalInput(input: String): String =
        input.filter { character -> character.isDigit() || character == '.' || character == ',' }

    private fun String.toCommaDecimalNumber(): String =
        if (hasCommaDecimalSeparator()) {
            "${integerPartDigits()}.${decimalPartDigits()}"
        } else {
            onlyDigits()
        }.trimEnd('.')

    private fun String.hasCommaDecimalSeparator(): Boolean =
        contains(',')

    private fun String.integerPartDigits(): String =
        substringBefore(',').onlyDigits()

    private fun String.decimalPartDigits(): String =
        substringAfter(',', missingDelimiterValue = "").onlyDigits()

    private fun String.onlyDigits(): String =
        filter { character -> character.isDigit() }

    private fun String.formatThousands(): String =
        reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()

    private fun BigDecimal.toPlainNormalizedBigDecimal(): BigDecimal =
        if (compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
        } else {
            stripTrailingZeros().toPlainString().toBigDecimal()
        }
}
