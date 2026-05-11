package com.example.socketapp

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socketapp.data.SecuritiesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.launch

enum class TradeType(val label: String) {
    Buy("Compra"),
    Sell("Venta"),
}

enum class BuyOrderType(val label: String, val description: String) {
    Market(
        label = "A mercado",
        description = "Se ejecuta al mejor precio disponible en el mercado.",
    ),
    Limit(
        label = "Limite",
        description = "Podes definir un precio maximo de compra.",
    ),
}

enum class BuyInputMode(val label: String) {
    Amount("Monto"),
    Quantity("Cantidad"),
}

@Immutable
data class Security(
    val id: Int,
    val ticker: String,
    val description: String,
    val type: String,
    val currency: String,
    val codeType: String,
    val codeValue: String,
    val industry: String,
    val liderMerval: Boolean,
    val indexationType: String?,
    val isFavorite: Boolean,
    val holdingQuantity: Int,
    val minInstrumentNominals: Int,
    val lotInstrumentSize: Int,
    val minTradeNominals: Int,
    val lastPrice: BigDecimal,
    val dailyVariationPercent: BigDecimal,
    val askPrice: BigDecimal,
    val bidPrice: BigDecimal,
    val percentageMovement: BigDecimal,
)

data class BuySecurityAccountContext(
    val monetaryAccountArs: String = "ARS-001",
    val monetaryAccountUsd: String = "USD-001",
    val investmentAccount: String = "COM-001",
    val accountBalanceArs: BigDecimal = BigDecimal("1159000.00"),
    val accountBalanceUsd: BigDecimal = BigDecimal("5000.00"),
    val tradeFee: BigDecimal = BigDecimal("0.00"),
    val minTradeAmount: BigDecimal = BigDecimal("100.00"),
    val maxTradeAmount: BigDecimal = BigDecimal("1159000.00"),
)

data class BuyValidationResult(
    val tradePrice: BigDecimal = BigDecimal.ZERO,
    val tradeNominals: BigDecimal = BigDecimal.ZERO,
    val tradeAmount: BigDecimal = BigDecimal.ZERO,
    val totalTradeAmount: BigDecimal = BigDecimal.ZERO,
    val maxInstrumentNominals: BigDecimal = BigDecimal.ZERO,
    val fee: BigDecimal = BigDecimal.ZERO,
    val errors: List<String> = emptyList(),
) {
    val canContinue: Boolean
        get() = errors.isEmpty() && tradeNominals > BigDecimal.ZERO && totalTradeAmount > BigDecimal.ZERO
}

data class BuySecurityUiState(
    val instrument: Security? = null,
    val accountContext: BuySecurityAccountContext = BuySecurityAccountContext(),
    val tradeType: TradeType = TradeType.Buy,
    val orderType: BuyOrderType = BuyOrderType.Market,
    val inputMode: BuyInputMode = BuyInputMode.Amount,
    val tradeCurrency: String = "ARS",
    val amountInput: String = "",
    val quantityInput: String = "",
    val limitPriceInput: String = "19.210,00",
    val validation: BuyValidationResult = BuyValidationResult(),
) {
    val activeInputText: String
        get() = when (inputMode) {
            BuyInputMode.Amount -> amountInput
            BuyInputMode.Quantity -> quantityInput
        }

    val activeInput: BigDecimal?
        get() = activeInputText
            .takeUnless { input -> input.isPendingDecimalInput() }
            ?.toBigDecimalOrNullForLocale()

    val validationMessage: String?
        get() = validation.errors.firstOrNull()
}

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val repository: SecuritiesRepository,
) : ViewModel() {
    var uiState by mutableStateOf(BuySecurityUiState())
        private set

    fun loadInstrument(securityId: String) {
        viewModelScope.launch {
            val instrument = repository.getBuyableInstrument(securityId) ?: return@launch
            uiState = uiState.copy(
                instrument = instrument,
                tradeCurrency = instrument.currency,
                limitPriceInput = instrument.askPrice.toMoneyString().replace(".", ","),
            )
            revalidate()
        }
    }

    internal fun replaceInstrument(instrument: Security) {
        uiState = uiState.copy(instrument = instrument)
        revalidate()
    }

    internal fun replaceAccountContext(accountContext: BuySecurityAccountContext) {
        uiState = uiState.copy(accountContext = accountContext)
        revalidate()
    }

    internal fun replaceTradeCurrency(tradeCurrency: String) {
        uiState = uiState.copy(tradeCurrency = tradeCurrency)
        revalidate()
    }

    fun onTradeTypeChange(tradeType: TradeType) {
        uiState = uiState.copy(tradeType = tradeType)
        revalidate()
    }

    fun onOrderTypeChange(orderType: BuyOrderType) {
        uiState = uiState.copy(orderType = orderType)
        revalidate()
    }

    fun onLimitPriceChange(input: String) {
        uiState = uiState.copy(limitPriceInput = sanitizeDecimalInput(input))
        revalidate()
    }

    fun onInputModeChange(inputMode: BuyInputMode) {
        uiState = uiState.copy(inputMode = inputMode)
        revalidate()
    }

    fun onInputChange(input: String) {
        val sanitizedInput = sanitizeDecimalInput(input)
        uiState = when (uiState.inputMode) {
            BuyInputMode.Amount -> uiState.copy(amountInput = sanitizedInput)
            BuyInputMode.Quantity -> uiState.copy(quantityInput = sanitizedInput)
        }
        revalidate()
    }

    private fun revalidate() {
        uiState = uiState.copy(validation = validate(uiState))
    }

    private fun validate(state: BuySecurityUiState): BuyValidationResult {
        val instrument = state.instrument
            ?: return BuyValidationResult(fee = state.accountContext.tradeFee)
        val context = state.accountContext
        val errors = mutableListOf<String>()

        val limitPrice = state.limitPriceInput.toBigDecimalOrNullForLocale()
        if (state.orderType == BuyOrderType.Limit) {
            if (limitPrice == null || limitPrice <= BigDecimal.ZERO) {
                errors += "Ingresa un precio limite valido"
            } else {
                errors += validateLimitPriceBand(
                    tradeType = state.tradeType,
                    limitPrice = limitPrice,
                    askPrice = instrument.askPrice,
                    bidPrice = instrument.bidPrice,
                    percentageMovement = instrument.percentageMovement,
                )
                errors += validateLimitPriceMultiple(
                    instrumentSubType = instrument.type,
                    isLiderMerval = instrument.liderMerval,
                    limitPrice = limitPrice,
                )
            }
        }

        val tradePrice = resolveTradePrice(
            orderType = state.orderType,
            tradeType = state.tradeType,
            limitPrice = limitPrice,
            instrument = instrument,
        )
        if (tradePrice == null) {
            if (errors.isEmpty()) {
                errors += "No se pudo obtener un precio valido para la operacion"
            }
            return BuyValidationResult(
                tradePrice = BigDecimal.ZERO,
                fee = context.tradeFee,
                errors = errors,
            )
        }

        val activeInput = state.activeInput?.stripTrailingZeros()
        if (activeInput == null) {
            return BuyValidationResult(
                tradePrice = tradePrice,
                maxInstrumentNominals = computeMaxInstrumentNominals(state, tradePrice),
                fee = context.tradeFee,
                errors = errors,
            )
        }

        val (tradeNominals, tradeAmount) = when (state.inputMode) {
            BuyInputMode.Quantity -> {
                val nominals = activeInput
                nominals to nominals.multiply(tradePrice).setScale(2, RoundingMode.HALF_UP)
            }
            BuyInputMode.Amount -> {
                val amount = activeInput
                val nominals = computeNominalsFromAmount(
                    tradeAmount = amount,
                    tradePrice = tradePrice,
                    lotSize = instrument.lotInstrumentSize.toBigDecimal(),
                )
                nominals to amount.setScale(2, RoundingMode.HALF_UP)
            }
        }

        if (state.inputMode == BuyInputMode.Amount && tradeNominals == BigDecimal.ZERO) {
            errors += "El monto ingresado no alcanza para la lamina minima"
        }

        val maxInstrumentNominals = computeMaxInstrumentNominals(state, tradePrice)
        errors += validateNominals(
            tradeType = state.tradeType,
            tradeNominals = tradeNominals,
            minNominals = instrument.minInstrumentNominals.toBigDecimal(),
            lotSize = instrument.lotInstrumentSize.toBigDecimal(),
            maxInstrumentNominals = maxInstrumentNominals,
            nominalsAvailable = instrument.holdingQuantity.toBigDecimal(),
        )

        if (state.tradeType == TradeType.Sell && state.inputMode == BuyInputMode.Amount) {
            errors += validateSellByAmountMax(
                orderType = state.orderType,
                tradeAmount = tradeAmount,
                nominalsAvailable = instrument.holdingQuantity.toBigDecimal(),
                bidPrice = instrument.bidPrice,
                limitPrice = limitPrice,
            )
        }

        errors += validateBalances(
            tradeType = state.tradeType,
            currency = state.tradeCurrency,
            operationMode = state.inputMode,
            tradeAmount = tradeAmount,
            tradeFee = context.tradeFee,
            balanceArs = context.accountBalanceArs,
            balanceUsd = context.accountBalanceUsd,
        )

        val totalTradeAmount = computeTotalTradeAmount(
            currency = state.tradeCurrency,
            tradeAmount = tradeAmount,
            tradeFee = context.tradeFee,
        )
        errors += validateMinMaxTotal(
            totalTradeAmount = totalTradeAmount,
            minTradeAmount = context.minTradeAmount,
            maxTradeAmount = context.maxTradeAmount,
        )

        return BuyValidationResult(
            tradePrice = tradePrice,
            tradeNominals = tradeNominals,
            tradeAmount = tradeAmount,
            totalTradeAmount = totalTradeAmount,
            maxInstrumentNominals = maxInstrumentNominals,
            fee = context.tradeFee,
            errors = errors.filter { it.isNotBlank() },
        )
    }

    private fun resolveTradePrice(
        orderType: BuyOrderType,
        tradeType: TradeType,
        limitPrice: BigDecimal?,
        instrument: Security,
    ): BigDecimal? =
        when (orderType) {
            BuyOrderType.Limit -> limitPrice?.takeIf { price -> price > BigDecimal.ZERO }
            BuyOrderType.Market -> when (tradeType) {
                TradeType.Buy -> instrument.askPrice.takeIf { price -> price > BigDecimal.ZERO }
                TradeType.Sell -> instrument.bidPrice.takeIf { price -> price > BigDecimal.ZERO }
            }
        }

    private fun validateLimitPriceBand(
        tradeType: TradeType,
        limitPrice: BigDecimal,
        askPrice: BigDecimal,
        bidPrice: BigDecimal,
        percentageMovement: BigDecimal,
    ): List<String> {
        return when (tradeType) {
            TradeType.Buy -> {
                val maxAllowed = askPrice.multiply(BigDecimal.ONE.add(percentageMovement))
                if (limitPrice > maxAllowed) {
                    listOf("El precio limite supera el maximo permitido de ${maxAllowed.toMoneyString()}")
                } else {
                    emptyList()
                }
            }
            TradeType.Sell -> {
                val minAllowed = bidPrice.multiply(BigDecimal.ONE.subtract(percentageMovement))
                if (limitPrice < minAllowed) {
                    listOf("El precio limite esta por debajo del minimo permitido de ${minAllowed.toMoneyString()}")
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun validateLimitPriceMultiple(
        instrumentSubType: String,
        isLiderMerval: Boolean,
        limitPrice: BigDecimal,
    ): List<String> {
        val priceStep = resolvePriceMultiple(instrumentSubType, isLiderMerval, limitPrice)

        return if (!limitPrice.isMultipleOf(priceStep)) {
            listOf("El precio limite debe respetar el multiplo de ${priceStep.toPlainString()}")
        } else {
            emptyList()
        }
    }

    private fun resolvePriceMultiple(
        instrumentSubType: String,
        isLiderMerval: Boolean,
        limitPrice: BigDecimal,
    ): BigDecimal {
        val normalizedSubType = instrumentSubType.trim().lowercase()

        return when {
            normalizedSubType in setOf("letras") -> BigDecimal("0.001")
            normalizedSubType in setOf("bonos", "obligaciones negociables", "on") ->
                rangedPriceStep(
                    limitPrice = limitPrice,
                    upTo50 = BigDecimal("0.001"),
                    upTo500 = BigDecimal("0.01"),
                    upTo5000 = BigDecimal("0.10"),
                    upTo50000 = BigDecimal("1"),
                    above50000 = BigDecimal("10"),
                )
            normalizedSubType in setOf("cedears", "cedear", "etfs", "etf") ->
                rangedPriceStep(
                    limitPrice = limitPrice,
                    upTo50 = BigDecimal("0.01"),
                    upTo500 = BigDecimal("0.05"),
                    upTo5000 = BigDecimal("0.10"),
                    upTo50000 = BigDecimal("1"),
                    above50000 = BigDecimal("10"),
                )
            normalizedSubType == "acciones" && isLiderMerval ->
                rangedPriceStep(
                    limitPrice = limitPrice,
                    upTo50 = BigDecimal("0.01"),
                    upTo500 = BigDecimal("0.05"),
                    upTo5000 = BigDecimal("0.10"),
                    upTo50000 = BigDecimal("1"),
                    above50000 = BigDecimal("10"),
                )
            normalizedSubType == "acciones" -> BigDecimal("0.01")
            else -> BigDecimal("0.01")
        }
    }

    private fun rangedPriceStep(
        limitPrice: BigDecimal,
        upTo50: BigDecimal,
        upTo500: BigDecimal,
        upTo5000: BigDecimal,
        upTo50000: BigDecimal,
        above50000: BigDecimal,
    ): BigDecimal =
        when {
            limitPrice <= BigDecimal("50") -> upTo50
            limitPrice <= BigDecimal("500") -> upTo500
            limitPrice <= BigDecimal("5000") -> upTo5000
            limitPrice <= BigDecimal("50000") -> upTo50000
            else -> above50000
        }

    private fun computeNominalsFromAmount(
        tradeAmount: BigDecimal,
        tradePrice: BigDecimal,
        lotSize: BigDecimal,
    ): BigDecimal {
        if (tradePrice <= BigDecimal.ZERO || lotSize <= BigDecimal.ZERO) return BigDecimal.ZERO
        val rawNominals = tradeAmount.divide(tradePrice, 8, RoundingMode.DOWN)
        return rawNominals.divide(lotSize, 0, RoundingMode.DOWN).multiply(lotSize)
    }

    private fun computeMaxInstrumentNominals(
        state: BuySecurityUiState,
        tradePrice: BigDecimal,
    ): BigDecimal {
        val instrument = state.instrument ?: return BigDecimal.ZERO
        if (tradePrice <= BigDecimal.ZERO) return BigDecimal.ZERO
        if (state.tradeType == TradeType.Sell) return instrument.holdingQuantity.toBigDecimal()

        val balance = if (state.tradeCurrency == "USD") {
            state.accountContext.accountBalanceUsd
        } else {
            state.accountContext.accountBalanceArs
        }
        return computeNominalsFromAmount(balance, tradePrice, instrument.lotInstrumentSize.toBigDecimal())
    }

    private fun validateNominals(
        tradeType: TradeType,
        tradeNominals: BigDecimal,
        minNominals: BigDecimal,
        lotSize: BigDecimal,
        maxInstrumentNominals: BigDecimal,
        nominalsAvailable: BigDecimal,
    ): List<String> {
        val errors = mutableListOf<String>()

        if (tradeNominals <= BigDecimal.ZERO) {
            errors += "La cantidad debe ser mayor a cero"
        } else if (tradeNominals < minNominals) {
            errors += "La cantidad minima es ${minNominals.toPlainString()} nominales"
        }
        if (lotSize > BigDecimal.ZERO && tradeNominals.remainder(lotSize) != BigDecimal.ZERO) {
            errors += "La cantidad debe ser multiplo de ${lotSize.toPlainString()}"
        }
        if (tradeNominals > maxInstrumentNominals) {
            errors += "La cantidad maxima para operar es ${maxInstrumentNominals.toPlainString()} nominales"
        }
        if (tradeType == TradeType.Sell && tradeNominals > nominalsAvailable) {
            errors += "Tenes ${nominalsAvailable.toPlainString()} nominales disponibles"
        }

        return errors
    }

    private fun validateSellByAmountMax(
        orderType: BuyOrderType,
        tradeAmount: BigDecimal,
        nominalsAvailable: BigDecimal,
        bidPrice: BigDecimal,
        limitPrice: BigDecimal?,
    ): List<String> {
        val price = if (orderType == BuyOrderType.Limit) limitPrice ?: BigDecimal.ZERO else bidPrice
        val maxAmount = nominalsAvailable.multiply(price).setScale(2, RoundingMode.HALF_UP)
        return if (tradeAmount > maxAmount) {
            listOf("El monto maximo vendible es $${maxAmount.toPlainString()}")
        } else {
            emptyList()
        }
    }

    private fun validateBalances(
        tradeType: TradeType,
        currency: String,
        operationMode: BuyInputMode,
        tradeAmount: BigDecimal,
        tradeFee: BigDecimal,
        balanceArs: BigDecimal,
        balanceUsd: BigDecimal,
    ): List<String> {
        val errors = mutableListOf<String>()

        val operationLabel = if (operationMode == BuyInputMode.Amount) "monto" else "cantidad"

        if (tradeType == TradeType.Buy && currency == "ARS" && balanceArs < tradeAmount) {
            errors += "Saldo insuficiente para operar por $operationLabel"
        }
        if (tradeType == TradeType.Buy && currency == "USD") {
            if (balanceArs < tradeFee) {
                errors += "Saldo insuficiente en pesos para comisiones"
            }
            if (balanceUsd < tradeAmount) {
                errors += "Saldo insuficiente en dolares para operar por $operationLabel"
            }
        }
        if (tradeType == TradeType.Sell && currency == "USD" && balanceArs < tradeFee) {
            errors += "Saldo insuficiente en pesos para comisiones"
        }

        return errors
    }

    private fun computeTotalTradeAmount(
        currency: String,
        tradeAmount: BigDecimal,
        tradeFee: BigDecimal,
    ): BigDecimal =
        if (currency == "ARS") {
            tradeAmount.add(tradeFee).setScale(2, RoundingMode.HALF_UP)
        } else {
            tradeAmount.setScale(2, RoundingMode.HALF_UP)
        }

    private fun validateMinMaxTotal(
        totalTradeAmount: BigDecimal,
        minTradeAmount: BigDecimal,
        maxTradeAmount: BigDecimal,
    ): List<String> =
        when {
            totalTradeAmount < minTradeAmount -> listOf("El monto minimo para operar es $${minTradeAmount.toPlainString()}")
            totalTradeAmount > maxTradeAmount -> listOf("El monto maximo para operar es $${maxTradeAmount.toPlainString()}")
            else -> emptyList()
        }

    private fun sanitizeDecimalInput(input: String): String {
        val filtered = input.filter { character -> character.isDigit() || character == ',' || character == '.' }
        if (filtered.isBlank()) return filtered

        val commaIndex = filtered.lastIndexOf(',')
        if (commaIndex != -1) {
            val integerPart = filtered.take(commaIndex).filter { character -> character.isDigit() }
            val decimalPart = filtered.drop(commaIndex + 1).filter { character -> character.isDigit() }
            return "$integerPart,$decimalPart"
        }

        val dotIndex = filtered.lastIndexOf('.')
        if (dotIndex == -1) return filtered

        val digitsAfterDot = filtered.length - dotIndex - 1
        if (digitsAfterDot == 3) {
            return filtered.filter { character -> character.isDigit() }
        }

        val integerPart = filtered.take(dotIndex).filter { character -> character.isDigit() }
        val decimalPart = filtered.drop(dotIndex + 1).filter { character -> character.isDigit() }
        return "$integerPart,$decimalPart"
    }
}

private fun String.toBigDecimalOrNullForLocale(): BigDecimal? =
    when {
        contains(",") -> replace(".", "").replace(",", ".")
        count { character -> character == '.' } == 1 && substringAfter('.').length != 3 -> this
        else -> replace(".", "")
    }.toBigDecimalOrNull()

private fun String.isPendingDecimalInput(): Boolean =
    endsWith(",") || endsWith(".")

private fun BigDecimal.isMultipleOf(step: BigDecimal): Boolean {
    if (step <= BigDecimal.ZERO) return false
    val scale = maxOf(scale(), step.scale()).coerceAtLeast(0)
    val multiplier = BigDecimal.TEN.pow(scale)
    val scaledValue = multiply(multiplier).setScale(0, RoundingMode.HALF_UP)
    val scaledStep = step.multiply(multiplier).setScale(0, RoundingMode.HALF_UP)
    return scaledValue.remainder(scaledStep) == BigDecimal.ZERO
}

private fun BigDecimal.toMoneyString(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString()
