package com.example.socketapp

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.math.BigDecimal

enum class BuyInputMode(val label: String) {
    Amount("Monto"),
    Quantity("Cantidad"),
}

@Immutable
data class BuyableInstrument(
    val id: Int,
    val ticker: String,
    val description: String,
    val subType: String,
    val currency: String,
    val codeType: String,
    val codeValue: String,
    val industry: String,
    val liderMerval: Boolean,
    val indexationType: String?,
    val isFavorite: Boolean,
    val holdingQuantity: BigDecimal,
    val minInstrumentNominals: BigDecimal,
    val lotInstrumentSize: BigDecimal,
    val minTradeNominals: BigDecimal,
    val lastPrice: BigDecimal,
    val dailyVariationPercent: BigDecimal,
)

data class BuySecurityUiState(
    val instrument: BuyableInstrument = MockBuyableInstrument,
    val inputMode: BuyInputMode = BuyInputMode.Amount,
    val amountInput: String = "",
    val quantityInput: String = "",
    val validationMessage: String? = null,
) {
    val activeInput: String
        get() = when (inputMode) {
            BuyInputMode.Amount -> amountInput
            BuyInputMode.Quantity -> quantityInput
        }
}

class BuySecurityViewModel : ViewModel() {
    var uiState by mutableStateOf(BuySecurityUiState())
        private set

    fun onInputModeChange(inputMode: BuyInputMode) {
        uiState = uiState.copy(
            inputMode = inputMode,
            validationMessage = validate(
                inputMode = inputMode,
                input = when (inputMode) {
                    BuyInputMode.Amount -> uiState.amountInput
                    BuyInputMode.Quantity -> uiState.quantityInput
                },
                instrument = uiState.instrument,
            ),
        )
    }

    fun onInputChange(input: String) {
        val sanitizedInput = sanitizeDecimalInput(input)
        uiState = when (uiState.inputMode) {
            BuyInputMode.Amount -> uiState.copy(
                amountInput = sanitizedInput,
                validationMessage = validate(BuyInputMode.Amount, sanitizedInput, uiState.instrument),
            )
            BuyInputMode.Quantity -> uiState.copy(
                quantityInput = sanitizedInput,
                validationMessage = validate(BuyInputMode.Quantity, sanitizedInput, uiState.instrument),
            )
        }
    }

    private fun validate(
        inputMode: BuyInputMode,
        input: String,
        instrument: BuyableInstrument,
    ): String? {
        val value = input.toBigDecimalOrNullForLocale() ?: return null
        if (value == BigDecimal.ZERO) return null

        return when (inputMode) {
            BuyInputMode.Amount -> {
                if (value < instrument.minTradeNominals) {
                    "El monto minimo para comprar es $${instrument.minTradeNominals.toPlainString()}"
                } else {
                    null
                }
            }
            BuyInputMode.Quantity -> {
                when {
                    value < instrument.minInstrumentNominals ->
                        "La lamina minima es ${instrument.minInstrumentNominals.toPlainString()}"
                    value > instrument.holdingQuantity ->
                        "Tenes ${instrument.holdingQuantity.toPlainString()} nominales disponibles"
                    value.remainder(instrument.lotInstrumentSize) != BigDecimal.ZERO ->
                        "La cantidad debe ser multiplo de ${instrument.lotInstrumentSize.toPlainString()}"
                    else -> null
                }
            }
        }
    }

    private fun sanitizeDecimalInput(input: String): String {
        val filtered = input.filter { character -> character.isDigit() || character == ',' || character == '.' }
        val decimalSeparatorIndex = filtered.indexOfFirst { character -> character == ',' || character == '.' }

        if (decimalSeparatorIndex == -1) return filtered

        val integerPart = filtered.take(decimalSeparatorIndex)
        val decimalSeparator = filtered[decimalSeparatorIndex]
        val decimalPart = filtered
            .drop(decimalSeparatorIndex + 1)
            .filter { character -> character.isDigit() }

        return "$integerPart$decimalSeparator$decimalPart"
    }
}

private val MockBuyableInstrument = BuyableInstrument(
    id = 66238,
    ticker = "PAMP",
    description = "PAMPA HOLDING SA ORD. 1V.",
    subType = "Acciones",
    currency = "ARS",
    codeType = "CAJA_VALOR",
    codeValue = "457",
    industry = "Electric Utilities",
    liderMerval = false,
    indexationType = null,
    isFavorite = false,
    holdingQuantity = BigDecimal("10"),
    minInstrumentNominals = BigDecimal("1"),
    lotInstrumentSize = BigDecimal("1"),
    minTradeNominals = BigDecimal("100"),
    lastPrice = BigDecimal("40005.75"),
    dailyVariationPercent = BigDecimal("0.27"),
)

private fun String.toBigDecimalOrNullForLocale(): BigDecimal? =
    replace(".", "")
        .replace(",", ".")
        .toBigDecimalOrNull()
