package com.example.socketapp.ui.securities

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

private const val MONEY_PATTERN = "\$#,##0.00"

private val moneyFormatSymbols = DecimalFormatSymbols().apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

internal fun BigDecimal.formatCurrency(): String =
    DecimalFormat(MONEY_PATTERN, moneyFormatSymbols).apply {
        roundingMode = RoundingMode.HALF_UP
    }.format(this)
