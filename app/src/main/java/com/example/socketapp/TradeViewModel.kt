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

enum class SettlementTerm(
    val label: String,
    val description: String,
) {
    TwentyFourHours(
        label = "24 h",
        description = "La operacion se ejecuta hoy y el dinero se liquida el dia habil siguiente.",
    ),
    Today(
        label = "Hoy",
        description = "La operacion se ejecuta hoy y el dinero se liquida el mismo dia.",
    ),
}

enum class BuyInputMode(val label: String) {
    Amount("Monto"),
    Quantity("Cantidad"),
}

@Immutable
data class Security(
    val id: Int,
    val ticker: String = "",
    val description: String = "",
    val type: String = "",
    val currency: String = "ARS",
    val codeType: String = "",
    val codeValue: String = "",
    val industry: String = "",
    val liderMerval: Boolean = false,
    val indexationType: String? = null,
    val isFavorite: Boolean = false,
    val minInstrumentNominals: Int = 0,
    val maxInstrumentNominals: Int = Int.MAX_VALUE,
    val lotInstrumentSize: Int = 0,
    val holdingQuantity: Int = 0,
    val lastPrice: BigDecimal = BigDecimal.ZERO,
    val dailyVariationPercent: BigDecimal = BigDecimal.ZERO,
    val askPrice: BigDecimal = BigDecimal.ZERO,
    val bidPrice: BigDecimal = BigDecimal.ZERO,
    val percentageMovement: BigDecimal = BigDecimal.ZERO,
) {
    val hasRequiredTradingConfiguration: Boolean
        get() = minInstrumentNominals > 0 && lotInstrumentSize > 0
}

data class TradingBalanceSet(
    val limitNow: BigDecimal = BigDecimal.ZERO,
    val limit24: BigDecimal = BigDecimal.ZERO,
    val marketNow: BigDecimal = BigDecimal.ZERO,
    val market24: BigDecimal = BigDecimal.ZERO,
) {
    fun balanceFor(
        orderType: BuyOrderType,
        settlementTerm: SettlementTerm,
    ): BigDecimal =
        when (orderType) {
            BuyOrderType.Limit -> when (settlementTerm) {
                SettlementTerm.Today -> limitNow
                SettlementTerm.TwentyFourHours -> limit24
            }

            BuyOrderType.Market -> when (settlementTerm) {
                SettlementTerm.Today -> marketNow
                SettlementTerm.TwentyFourHours -> market24
            }
        }
}

data class BuySecurityAccountContext(
    val monetaryAccountArs: String = "ARS-001",
    val monetaryAccountUsd: String = "USD-001",
    val investmentAccount: String = "COM-001",
    val arsBalances: TradingBalanceSet = TradingBalanceSet(
        limitNow = BigDecimal("1159000.00"),
        limit24 = BigDecimal("1159000.00"),
        marketNow = BigDecimal("1159000.00"),
        market24 = BigDecimal("1159000.00"),
    ),
    val usdBalances: TradingBalanceSet = TradingBalanceSet(
        limitNow = BigDecimal("5000.00"),
        limit24 = BigDecimal("5000.00"),
        marketNow = BigDecimal("5000.00"),
        market24 = BigDecimal("5000.00"),
    ),
) {
    fun balanceFor(
        currency: String,
        orderType: BuyOrderType,
        settlementTerm: SettlementTerm,
    ): BigDecimal {
        val balances = if (currency == "USD") usdBalances else arsBalances
        return balances.balanceFor(orderType, settlementTerm)
    }
}

data class BuyValidationResult(
    val tradePrice: BigDecimal = BigDecimal.ZERO,
    val tradeNominals: BigDecimal = BigDecimal.ZERO,
    val tradeAmount: BigDecimal = BigDecimal.ZERO,
    val maxNominals: BigDecimal = BigDecimal.ZERO,
    val errors: List<TradeValidationError> = emptyList(),
) {
    val canContinue: Boolean
        get() = errors.isEmpty() && tradeAmount > BigDecimal.ZERO
}

data class TradeConfirmationState(
    val fee: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
    val amountWithFee: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
    val estimatedAmount: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
    val errors: List<TradeValidationError> = emptyList(),
) {
    val canConfirm: Boolean
        get() = errors.isEmpty() && estimatedAmount > BigDecimal.ZERO
}

data class BuySecurityUiState(
    val instrument: Security? = null,
    val accountContext: BuySecurityAccountContext = BuySecurityAccountContext(),
    val tradeType: TradeType = TradeType.Buy,
    val orderType: BuyOrderType = BuyOrderType.Market,
    val settlementTerm: SettlementTerm = SettlementTerm.Today,
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
    val confirmation: TradeConfirmationState = TradeConfirmationState(),
) {
    val canContinue: Boolean
        get() = validation.canContinue && instrument?.hasRequiredTradingConfiguration == true

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
            revalidateBuy()
        }
    }

    internal fun replaceInstrument(instrument: Security) {
        uiState = uiState.copy(instrument = instrument)
        revalidateBuy()
    }

    internal fun replaceAccountContext(accountContext: BuySecurityAccountContext) {
        uiState = uiState.copy(accountContext = accountContext)
        revalidateBuy()
    }

    internal fun replaceTradeCurrency(tradeCurrency: String) {
        uiState = uiState.copy(tradeCurrency = tradeCurrency)
        revalidateBuy()
    }

    fun onTradeTypeChange(tradeType: TradeType) {
        uiState = uiState.copy(tradeType = tradeType)
        revalidateBuy()
    }

    fun onOrderTypeChange(orderType: BuyOrderType) {
        uiState = uiState.copy(orderType = orderType)
        revalidateBuy()
    }

    fun onSettlementTermChange(settlementTerm: SettlementTerm) {
        uiState = uiState.copy(settlementTerm = settlementTerm)
        revalidateBuy()
    }

    fun onLimitPriceChange(input: String) {
        uiState = uiState.copy(limitPriceInput = TradeInputParser.formatLimitPriceInput(input))
        revalidateBuy()
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
        revalidateBuy()
    }

    fun onInputChange(inputMode: BuyInputMode, input: String) {
        if (inputMode != uiState.inputMode) return
        val sanitizedInput = sanitizeInput(inputMode, input)
        uiState = uiState.withInputText(inputMode, sanitizedInput)
        revalidateBuy()
    }

    fun prepareConfirmation() {
        val confirmation = validator.validateConfirmation(uiState, uiState.validation)
        uiState = uiState.copy(confirmation = confirmation)
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

    private fun revalidateBuy() {
        val state = uiState
        val validation = validator.validate(state)
        uiState = state.copy(
            amountInput = validation.tradeAmount,
            quantityInput = validation.tradeNominals,
            validation = validation,
            inputError = validation.errors.firstOrNull { error -> !error.isLimitPriceError() },
            inputHelper = buildInputHelper(state, validation),
            limitPriceError = validation.errors.firstOrNull { error -> error.isLimitPriceError() },
            limitPriceHelper = buildLimitPriceHelper(state),
            confirmation = TradeConfirmationState(),
        )
    }

    private fun buildInputHelper(
        state: BuySecurityUiState,
        validation: BuyValidationResult,
    ): TradeInputHelper {
        val context = state.accountContext
        val currencyBalance = context.balanceFor(
            currency = state.tradeCurrency,
            orderType = state.orderType,
            settlementTerm = state.settlementTerm,
        )

        if (state.tradeType == TradeType.Sell) {
            return if (validation.tradeAmount > BigDecimal.ZERO) {
                TradeInputHelper.ApproximateCredit(validation.tradeAmount)
            } else {
                TradeInputHelper.AvailableNominals(state.instrument?.holdingQuantity ?: 0)
            }
        }

        return when {
            validation.tradeAmount > BigDecimal.ZERO -> TradeInputHelper.ApproximateDebit(validation.tradeAmount)
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
                    .multiply(BigDecimal.ONE.add(instrument.percentageMovement.toPriceBandMovement()))
                    .setScale(2, RoundingMode.HALF_UP),
            )

            TradeType.Sell -> TradeInputLimitPriceHelper.MinAllowed(
                instrument.bidPrice
                    .multiply(BigDecimal.ONE.subtract(instrument.percentageMovement.toPriceBandMovement()))
                    .setScale(2, RoundingMode.HALF_UP),
            )
        }
    }
}

internal fun BigDecimal.toMoneyString(): String =
    setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
