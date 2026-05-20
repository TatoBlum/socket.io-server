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

data class Account(
    val number: String = "",
    val type: String = "",
    val balance: BigDecimal = BigDecimal.ZERO,
    val currency: String = "ARS",
    val branchId: String = "",
    val relation: String = "",
    val CBU: String = "",
    val balanceLimitNow: String = "0",
    val balanceLimit24: String = "0",
    val balanceMarketNow: String = "0",
    val balanceMarket24: String = "0",
    val commitedLimitAmount: String = "0",
    val productTypeId: String? = null,
    val idFondo: Int = 0,
    val description: String = "",
) {
    val tradingBalances: TradingBalanceSet
        get() = TradingBalanceSet(
            limitNow = balanceLimitNow.toMoneyBigDecimal(),
            limit24 = balanceLimit24.toMoneyBigDecimal(),
            marketNow = balanceMarketNow.toMoneyBigDecimal(),
            market24 = balanceMarket24.toMoneyBigDecimal(),
    )

    val isArs: Boolean
        get() = currency.normalizedCurrency() == "ARS"

    val isUsd: Boolean
        get() = currency.normalizedCurrency() == "USD"
}

data class TradeAccountContext(
    val selectedAccount: Account = Account(
        number = "ARS-001",
        currency = "ARS",
        balanceLimitNow = "1159000.00",
        balanceLimit24 = "1159000.00",
        balanceMarketNow = "1159000.00",
        balanceMarket24 = "1159000.00",
        description = "ARS-001",
    ),
    val availableArsAccounts: List<Account> = listOf(selectedAccount).filter { it.isArs },
    val selectedFeeAccount: Account? = availableArsAccounts.singleOrNull(),
    val investmentAccount: String = "COM-001",
) {
    val effectiveFeeAccount: Account?
        get() {
            val selectedAvailableFeeAccount = selectedFeeAccount.matchingAccountIn(availableArsAccounts)
            return selectedAvailableFeeAccount ?: defaultFeeAccountFor(
                selectedAccount = selectedAccount,
                availableArsAccounts = availableArsAccounts,
            )
        }

    fun selectedBalanceFor(
        orderType: BuyOrderType,
        settlementTerm: SettlementTerm,
    ): BigDecimal =
        selectedAccount.tradingBalances.balanceFor(orderType, settlementTerm)

    fun feeBalanceFor(
        orderType: BuyOrderType,
        settlementTerm: SettlementTerm,
    ): BigDecimal? =
        effectiveFeeAccount?.tradingBalances?.balanceFor(orderType, settlementTerm)
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
    val accounts: List<Account> = defaultTradeAccounts(),
    val accountContext: TradeAccountContext = TradeAccountContext(),
    val tradeType: TradeType = TradeType.Buy,
    val orderType: BuyOrderType = BuyOrderType.Market,
    val settlementTerm: SettlementTerm = SettlementTerm.Today,
    val inputMode: BuyInputMode = BuyInputMode.Amount,
    val amountInputText: String = "",
    val quantityInputText: String = "",
    val amountInput: BigDecimal = BigDecimal.ZERO,
    val quantityInput: BigDecimal = BigDecimal.ZERO,
    val limitPriceInput: String = "",
    val validation: BuyValidationResult = BuyValidationResult(),
    val inputError: TradeValidationError? = validation.errors.firstOrNull(),
    val inputHelper: TradeInputHelper = TradeInputHelper.None,
    val limitPriceError: TradeValidationError? = null,
    val limitPriceHelper: TradeInputLimitPriceHelper = TradeInputLimitPriceHelper.None,
    val confirmation: TradeConfirmationState = TradeConfirmationState(),
) {
    val tradeCurrency: String
        get() = (instrument?.currency ?: accountContext.selectedAccount.currency).normalizedCurrency()

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
            uiState = uiState.copy(instrument = instrument)
            revalidateBuy()
        }
    }

    internal fun replaceInstrument(instrument: Security) {
        uiState = uiState.copy(instrument = instrument)
        revalidateBuy()
    }

    fun onTradeAccountSelected(selectedAccount: Account) {
        onTradeAccountSelected(
            selectedAccount = selectedAccount,
            availableArsAccounts = uiState.accounts.filter { account -> account.isArs },
        )
    }

    fun onTradeAccountSelected(
        selectedAccount: Account,
        availableArsAccounts: List<Account>,
    ) {
        val normalizedArsAccounts = if (selectedAccount.isArs) {
            (listOf(selectedAccount) + availableArsAccounts)
                .distinctBy { account -> account.number }
        } else {
            availableArsAccounts
        }.filter { account -> account.isArs }
        uiState = uiState.copy(
            accountContext = uiState.accountContext.copy(
                selectedAccount = selectedAccount,
                availableArsAccounts = normalizedArsAccounts,
                selectedFeeAccount = defaultFeeAccountFor(
                    selectedAccount = selectedAccount,
                    availableArsAccounts = normalizedArsAccounts,
                ),
            ),
        )
        revalidateBuy()
    }

    fun onFeeAccountSelected(account: Account) {
        val feeAccount = uiState.accountContext.availableArsAccounts
            .firstOrNull { availableAccount -> availableAccount.number == account.number }
            ?: return
        uiState = uiState.copy(
            accountContext = uiState.accountContext.copy(selectedFeeAccount = feeAccount),
        )
        revalidateConfirmation()
    }

    fun onTradeTypeChange(tradeType: TradeType) {
        uiState = uiState.copy(tradeType = tradeType).clearLimitPriceIfNeeded()
        revalidateBuy()
    }

    fun onOrderTypeChange(orderType: BuyOrderType) {
        uiState = uiState.copy(orderType = orderType).clearLimitPriceIfNeeded()
        revalidateBuy()
    }

    fun onSettlementTermChange(settlementTerm: SettlementTerm) {
        uiState = uiState.copy(settlementTerm = settlementTerm)
        revalidateBuy()
    }

    fun onLimitPriceChange(input: String) {
        uiState = uiState.copy(
            limitPriceInput = TradeInputParser.formatLimitPriceInput(input),
        )
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
        revalidateConfirmation()
    }

    private fun revalidateConfirmation() {
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
        val validation = if (state.shouldSkipBuyValidation()) {
            BuyValidationResult()
        } else {
            validator.validate(state)
        }
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

    private fun BuySecurityUiState.clearLimitPriceIfNeeded(): BuySecurityUiState =
        if (orderType == BuyOrderType.Limit) copy(limitPriceInput = "") else this

    private fun BuySecurityUiState.shouldSkipBuyValidation(): Boolean =
        orderType == BuyOrderType.Limit && limitPriceInput.isBlank()

    private fun buildInputHelper(
        state: BuySecurityUiState,
        validation: BuyValidationResult,
    ): TradeInputHelper {
        val context = state.accountContext
        val selectedBalance = context.selectedBalanceFor(
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
            state.inputMode == BuyInputMode.Amount -> TradeInputHelper.AvailableBalance(selectedBalance)
            else -> TradeInputHelper.AvailableToBuy(selectedBalance)
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

internal fun String.toMoneyBigDecimal(): BigDecimal {
    val normalizedValue = trim()
    if (normalizedValue.isBlank()) return BigDecimal.ZERO
    return if (normalizedValue.contains(",")) {
        normalizedValue
            .replace(".", "")
            .replace(",", ".")
            .toBigDecimal()
    } else {
        normalizedValue.toBigDecimal()
    }
}

internal fun String.normalizedCurrency(): String = trim().uppercase()

private fun defaultTradeAccounts(): List<Account> =
    listOf(
        Account(
            number = "428571545224",
            type = "CA",
            currency = "ARS",
            branchId = "522",
            relation = "TITU",
            CBU = "0070522630004285715449",
            balance = BigDecimal("101520709.11"),
            balanceLimitNow = "89690294,9278",
            balanceLimit24 = "89690294,9278",
            balanceMarketNow = "86944673,6545",
            balanceMarket24 = "86944673,6545",
            description = "CA \$ Nro 428571545224 . \$101.520.709,11",
        ),
        Account(
            number = "428571545225",
            type = "CA",
            currency = "USD",
            branchId = "522",
            relation = "TITU",
            balance = BigDecimal("25000.00"),
            balanceLimitNow = "25000.00",
            balanceLimit24 = "25000.00",
            balanceMarketNow = "25000.00",
            balanceMarket24 = "25000.00",
            description = "CA U\$S Nro 428571545225 . U\$S25.000,00",
        ),
    )

private fun Account?.matchingAccountIn(accounts: List<Account>): Account? =
    this?.let { selected ->
        accounts.firstOrNull { account -> account.number == selected.number }
    }

private fun defaultFeeAccountFor(
    selectedAccount: Account,
    availableArsAccounts: List<Account>,
): Account? =
    when {
        selectedAccount.isArs -> selectedAccount
        availableArsAccounts.size == 1 -> availableArsAccounts.first()
        else -> null
    }

internal fun BigDecimal.toMoneyString(): String =
    setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
