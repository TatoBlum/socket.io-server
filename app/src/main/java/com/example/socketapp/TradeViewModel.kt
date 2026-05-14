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
    val minInstrumentNominals: Int,
    val maxInstrumentNominals: Int,
    val lotInstrumentSize: Int,
    val maxTradeNominals: Int,
    val minTradeAmount: BigDecimal,
    val maxTradeAmount: BigDecimal,
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
    val availableNominals: Int = 0,
)

data class BuyValidationResult(
    val tradePrice: BigDecimal = BigDecimal.ZERO,
    val tradeNominals: BigDecimal = BigDecimal.ZERO,
    val tradeAmount: BigDecimal = BigDecimal.ZERO,
    val totalTradeAmount: BigDecimal = BigDecimal.ZERO,
    val maxNominals: BigDecimal = BigDecimal.ZERO,
    val fee: BigDecimal = BigDecimal.ZERO,
    val errors: List<TradeValidationError> = emptyList(),
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
    val amountInputText: String = "",
    val quantityInputText: String = "",
    val amountInput: BigDecimal = BigDecimal.ZERO,
    val quantityInput: BigDecimal = BigDecimal.ZERO,
    val limitPriceInput: String = "19.210,00",
    val validation: BuyValidationResult = BuyValidationResult(),
    val inputError: TradeValidationError? = validation.errors.firstOrNull(),
    val inputHelper: TradeInputHelper = TradeInputHelper.None,
    val limitPriceError: TradeValidationError? = null,
    val limitPriceHelper: TradeInputLimitPriceHelper = TradeInputLimitPriceHelper.None,
) {
    val activeInputText: String
        get() = when (inputMode) {
            BuyInputMode.Amount -> amountInputText
            BuyInputMode.Quantity -> quantityInputText
        }

    val activeInput: BigDecimal?
        get() = when (inputMode) {
            BuyInputMode.Amount -> TradeInputParser.parseAmountInput(activeInputText)
            BuyInputMode.Quantity -> TradeInputParser.parseWholeNumberInput(activeInputText)
        }
}

@HiltViewModel
class TradeViewModel @Inject constructor(
    private val repository: SecuritiesRepository,
    private val validator: TradeValidator,
) : ViewModel() {
    var uiState by mutableStateOf(BuySecurityUiState())
        private set

    fun loadInstrument(securityId: String) {
        viewModelScope.launch {
            val instrument = repository.getBuyableInstrument(securityId) ?: return@launch
            uiState = uiState.copy(
                instrument = instrument,
                tradeCurrency = instrument.currency,
                limitPriceInput = TradeInputParser.formatLimitPriceInput(
                    instrument.askPrice.toMoneyString().replace(".", ","),
                ),
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
        uiState = uiState.copy(limitPriceInput = TradeInputParser.formatLimitPriceInput(input))
        revalidate()
    }

    fun onInputModeChange(inputMode: BuyInputMode) {
        uiState = when (inputMode) {
            BuyInputMode.Amount -> uiState.copy(
                inputMode = inputMode,
                amountInputText = "",
            )
            BuyInputMode.Quantity -> uiState.copy(
                inputMode = inputMode,
                quantityInputText = "",
            )
        }
        revalidate()
    }

    fun onInputChange(inputMode: BuyInputMode, input: String) {
        if (inputMode != uiState.inputMode) return
        val sanitizedInput = sanitizeInput(inputMode, input)
        uiState = uiState.withInputText(inputMode, sanitizedInput)
        revalidate()
    }

    private fun sanitizeInput(inputMode: BuyInputMode, input: String): String =
        when (inputMode) {
            BuyInputMode.Amount -> TradeInputParser.sanitizeAmountInput(input)
            BuyInputMode.Quantity -> TradeInputParser.sanitizeWholeNumberInput(input)
        }

    private fun BuySecurityUiState.withInputText(
        inputMode: BuyInputMode,
        input: String,
    ): BuySecurityUiState =
        when (inputMode) {
            BuyInputMode.Amount -> copy(amountInputText = input)
            BuyInputMode.Quantity -> copy(quantityInputText = input)
        }

    private fun revalidate() {
        val validation = validator.validate(uiState)
        uiState = uiState.copy(
            amountInput = validation.tradeAmount,
            quantityInput = validation.tradeNominals,
            validation = validation,
            inputError = validation.errors.firstOrNull { error -> !error.isLimitPriceError() },
            inputHelper = buildInputHelper(uiState, validation),
            limitPriceError = validation.errors.firstOrNull { error -> error.isLimitPriceError() },
            limitPriceHelper = buildLimitPriceHelper(uiState),
        )
    }

    private fun buildInputHelper(
        state: BuySecurityUiState,
        validation: BuyValidationResult,
    ): TradeInputHelper {
        val context = state.accountContext
        val currencyBalance = if (state.tradeCurrency == "USD") {
            context.accountBalanceUsd
        } else {
            context.accountBalanceArs
        }

        if (state.tradeType == TradeType.Sell) {
            return if (validation.tradeAmount > BigDecimal.ZERO) {
                TradeInputHelper.ApproximateCredit(validation.tradeAmount)
            } else {
                TradeInputHelper.AvailableNominals(context.availableNominals)
            }
        }

        return when {
            validation.totalTradeAmount > BigDecimal.ZERO -> TradeInputHelper.ApproximateDebit(validation.totalTradeAmount)
            state.inputMode == BuyInputMode.Amount -> TradeInputHelper.AvailableBalance(currencyBalance)
            else -> TradeInputHelper.AvailableToBuy(currencyBalance)
        }
    }

    private fun buildLimitPriceHelper(state: BuySecurityUiState): TradeInputLimitPriceHelper {
        val instrument = state.instrument ?: return TradeInputLimitPriceHelper.None
        if (state.orderType != BuyOrderType.Limit) return TradeInputLimitPriceHelper.None

        return when (state.tradeType) {
            TradeType.Buy -> TradeInputLimitPriceHelper.MaxAllowed(
                instrument.askPrice
                    .multiply(BigDecimal.ONE.add(instrument.percentageMovement))
                    .setScale(2, RoundingMode.HALF_UP),
            )

            TradeType.Sell -> TradeInputLimitPriceHelper.MinAllowed(
                instrument.bidPrice
                    .multiply(BigDecimal.ONE.subtract(instrument.percentageMovement))
                    .setScale(2, RoundingMode.HALF_UP),
            )
        }
    }
}

internal fun BigDecimal.toMoneyString(): String =
    setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
