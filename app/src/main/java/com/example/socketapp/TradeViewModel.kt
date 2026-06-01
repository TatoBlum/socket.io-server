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

enum class TradeOrderType(val label: String, val description: String) {
    Market(
        label = "A mercado",
        description = "Se ejecuta al mejor precio disponible en el mercado.",
    ),
    Limit(
        label = "Limite",
        description = "Podes definir un precio limite para la operacion.",
    ),
}

enum class SettlementType(
    val label: String,
    val description: String,
) {
    TWENTY_FOUR_HOURS(
        label = "24 h",
        description = "La operacion se ejecuta hoy y el dinero se liquida el dia habil siguiente.",
    ),
    TODAY(
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
    val panel: String = "",
    val liderMerval: Boolean = false,
    val indexationType: String? = null,
    val isFavorite: Boolean = false,
    val minInstrumentNominals: Int = 0,
    val maxInstrumentNominals: Int = Int.MAX_VALUE,
    val lotInstrumentSize: Int = 0,
    val holdingQuantity: Int = 0,
    val price: BigDecimal = BigDecimal.ZERO,
    val priceChange: BigDecimal = BigDecimal.ZERO,
    val dailyVariationPercent: BigDecimal = BigDecimal.ZERO,
    val askPrice00: BigDecimal = BigDecimal.ZERO,
    val askPrice24: BigDecimal = BigDecimal.ZERO,
    val bidPrice00: BigDecimal = BigDecimal.ZERO,
    val bidPrice24: BigDecimal = BigDecimal.ZERO,
    val percentageMovement: BigDecimal = BigDecimal.ZERO,
    val minBuyArsAmount: BigDecimal = BigDecimal.ZERO,
) {
    val hasRequiredTradingConfiguration: Boolean
        get() = minInstrumentNominals > 0 && lotInstrumentSize > 0

    val currencySymbol: String
        get() = if (currency.normalizedCurrency() == "USD") "USD" else "\$"

    fun askPriceFor(settlementTerm: SettlementType): BigDecimal =
        when (settlementTerm) {
            SettlementType.TODAY -> askPrice00
            SettlementType.TWENTY_FOUR_HOURS -> askPrice24
        }

    fun bidPriceFor(settlementTerm: SettlementType): BigDecimal =
        when (settlementTerm) {
            SettlementType.TODAY -> bidPrice00
            SettlementType.TWENTY_FOUR_HOURS -> bidPrice24
        }
}

data class TradingBalanceSet(
    val limitNow: BigDecimal = BigDecimal.ZERO,
    val limit24: BigDecimal = BigDecimal.ZERO,
    val marketNow: BigDecimal = BigDecimal.ZERO,
    val market24: BigDecimal = BigDecimal.ZERO,
) {
    fun balanceFor(
        orderType: TradeOrderType,
        settlementTerm: SettlementType,
    ): BigDecimal =
        when (orderType) {
            TradeOrderType.Limit -> when (settlementTerm) {
                SettlementType.TODAY -> limitNow
                SettlementType.TWENTY_FOUR_HOURS -> limit24
            }

            TradeOrderType.Market -> when (settlementTerm) {
                SettlementType.TODAY -> marketNow
                SettlementType.TWENTY_FOUR_HOURS -> market24
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
    val selectedAccount: Account? = null,
    val availableArsAccounts: List<Account> = emptyList(),
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
        orderType: TradeOrderType,
        settlementTerm: SettlementType,
    ): BigDecimal =
        selectedAccount
            ?.tradingBalances
            ?.balanceFor(orderType, settlementTerm)
            ?: BigDecimal.ZERO

    fun feeBalanceFor(
        orderType: TradeOrderType,
        settlementTerm: SettlementType,
    ): BigDecimal? =
        effectiveFeeAccount?.tradingBalances?.balanceFor(orderType, settlementTerm)
}

data class TradeValidationResult(
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
    val marketFee: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
    val vat: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
    val amountWithFee: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
    val estimatedAmount: BigDecimal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
    val errors: List<TradeValidationError> = emptyList(),
) {
    val totalFees: BigDecimal
        get() = fee.add(marketFee).add(vat).setScale(2, RoundingMode.HALF_UP)

    val canConfirm: Boolean
        get() = errors.isEmpty() && estimatedAmount > BigDecimal.ZERO
}

enum class TradeOption(val label: String)  {
    SIMPLE("Simple"),
    ADVANCE("Avanzado")
}

data class TradeViewModelState(
    val instrument: Security? = null,
    val accounts: List<Account> = defaultTradeAccounts(),
    val accountContext: TradeAccountContext = TradeAccountContext(),
    val tradeType: TradeType = TradeType.Buy,
    val orderType: TradeOrderType = TradeOrderType.Market,
    val settlementTerm: SettlementType = SettlementType.TODAY,
    val tradeOption: TradeOption = TradeOption.SIMPLE,
    val inputMode: BuyInputMode = BuyInputMode.Amount,
    val amountInputText: String = "",
    val quantityInputText: String = "",
    val amountInput: BigDecimal = BigDecimal.ZERO,
    val quantityInput: BigDecimal = BigDecimal.ZERO,
    val limitPriceInput: String = "",
    val isLimitPriceInputTouched: Boolean = false,
    val validation: TradeValidationResult = TradeValidationResult(),
    val inputError: TradeValidationError? = validation.errors.firstOrNull(),
    val inputHelper: TradeInputHelper = TradeInputHelper.None,
    val limitPriceError: TradeValidationError? = null,
    val limitPriceHelper: TradeInputLimitPriceHelper = TradeInputLimitPriceHelper.None,
    val confirmation: TradeConfirmationState = TradeConfirmationState(),
) {
    val tradeCurrency: String
        get() = (instrument?.currency ?: accountContext.selectedAccount?.currency.orEmpty()).normalizedCurrency()

    val canContinue: Boolean
        get() = validation.canContinue &&
            accountContext.selectedAccount != null

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
    var uiState by mutableStateOf(TradeViewModelState())
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

    fun onTradeOptionsChange(tradeOption: TradeOption) {
        uiState = uiState.copy(tradeOption = tradeOption)
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
        uiState = uiState.copy(tradeType = tradeType).clearLimitPrice()
        revalidateBuy()
    }

    fun onOrderTypeChange(orderType: TradeOrderType) {
        uiState = uiState.copy(orderType = orderType).clearLimitPrice()
        revalidateBuy()
    }

    fun onSettlementTermChange(settlementTerm: SettlementType) {
        uiState = uiState.copy(settlementTerm = settlementTerm)
        revalidateBuy()
    }

    fun onLimitPriceChange(input: String) {
        uiState = uiState.copy(
            limitPriceInput = TradeInputParser.formatLimitPriceInput(input),
            isLimitPriceInputTouched = true,
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
        val mockFees = buildMockConfirmationFees(uiState.validation.tradeAmount)
        val confirmation = validator.validateConfirmation(
            state = uiState,
            validation = uiState.validation,
            fee = mockFees.fee,
            marketFee = mockFees.marketFee,
            vat = mockFees.vat,
        )
        uiState = uiState.copy(confirmation = confirmation)
    }

    private fun sanitizeInput(inputMode: BuyInputMode, input: String): String =
        when (inputMode) {
            BuyInputMode.Amount -> TradeInputParser.sanitizeAmountInput(input)
            BuyInputMode.Quantity -> TradeInputParser.sanitizeWholeNumberInput(input)
        }

    private fun TradeViewModelState.withInputText(
        inputMode: BuyInputMode,
        input: String,
    ): TradeViewModelState =
        when (inputMode) {
            BuyInputMode.Amount -> copy(amountInputText = input)
            BuyInputMode.Quantity -> copy(quantityInputText = input)
        }

    private fun revalidateBuy() {
        val state = uiState
        val validation = if (state.shouldSkipBuyValidation()) {
            TradeValidationResult()
        } else {
            validator.validate(state)
        }
        uiState = state.copy(
            amountInput = validation.tradeAmount,
            quantityInput = validation.tradeNominals,
            validation = validation,
            inputError = validation.errors.primaryInputError(state.inputMode),
            inputHelper = buildInputHelper(state, validation),
            limitPriceError = validation.visibleLimitPriceError(state),
            limitPriceHelper = buildLimitPriceHelper(state),
            confirmation = TradeConfirmationState(),
        )
    }

    private fun TradeViewModelState.clearLimitPrice(): TradeViewModelState =
        if (orderType == TradeOrderType.Limit) {
            copy(
                limitPriceInput = "",
                isLimitPriceInputTouched = false,
            )
        } else {
            this
        }

    private fun TradeViewModelState.shouldSkipBuyValidation(): Boolean =
        orderType == TradeOrderType.Limit &&
            limitPriceInput.isBlank() &&
            activeInput == null

    private fun buildInputHelper(
        state: TradeViewModelState,
        validation: TradeValidationResult,
    ): TradeInputHelper {
        if (state.accountContext.selectedAccount == null) return TradeInputHelper.None

        return when (state.tradeType) {
            TradeType.Sell -> buildSellInputHelper(state, validation)
            TradeType.Buy -> buildBuyInputHelper(state, validation)
        }
    }

    private fun buildSellInputHelper(
        state: TradeViewModelState,
        validation: TradeValidationResult,
    ): TradeInputHelper =
        when {
            validation.tradeAmount > BigDecimal.ZERO && state.inputMode == BuyInputMode.Quantity ->
                TradeInputHelper.EquivalentAmount(validation.tradeAmount)

            validation.tradeAmount > BigDecimal.ZERO -> TradeInputHelper.ApproximateCredit(validation.tradeAmount)
            else -> TradeInputHelper.AvailableNominals(state.instrument?.holdingQuantity ?: 0)
        }

    private fun buildBuyInputHelper(
        state: TradeViewModelState,
        validation: TradeValidationResult,
    ): TradeInputHelper {
        if (validation.errors.contains(TradeValidationError.InvalidLimitPrice)) {
            return TradeInputHelper.None
        }

        val selectedBalance = state.accountContext.selectedBalanceFor(
            orderType = state.orderType,
            settlementTerm = state.settlementTerm,
        )

        return when {
            validation.tradeAmount > BigDecimal.ZERO && state.inputMode == BuyInputMode.Quantity && state.tradeOption == TradeOption.ADVANCE ->
                TradeInputHelper.EquivalentAmount(validation.tradeAmount)

            validation.tradeAmount > BigDecimal.ZERO -> TradeInputHelper.ApproximateDebit(validation.tradeAmount)
            state.inputMode == BuyInputMode.Amount -> TradeInputHelper.AvailableBalance(selectedBalance)
            else -> TradeInputHelper.AvailableNominalsToBuy(validation.maxNominals)
        }
    }

    private fun buildLimitPriceHelper(state: TradeViewModelState): TradeInputLimitPriceHelper {
        val instrument = state.instrument ?: return TradeInputLimitPriceHelper.None
        if (state.orderType != TradeOrderType.Limit) return TradeInputLimitPriceHelper.None

        return when (state.tradeType) {
            TradeType.Buy -> TradeInputLimitPriceHelper.MaxAllowed(
                amount = instrument.askPriceFor(state.settlementTerm)
                    .multiply(BigDecimal.ONE.add(instrument.percentageMovement.toPriceBandMovement()))
                    .setScale(2, RoundingMode.HALF_UP),
                currencySymbol = instrument.currencySymbol,
            )

            TradeType.Sell -> TradeInputLimitPriceHelper.MinAllowed(
                amount = instrument.bidPriceFor(state.settlementTerm)
                    .multiply(BigDecimal.ONE.subtract(instrument.percentageMovement.toPriceBandMovement()))
                    .setScale(2, RoundingMode.HALF_UP),
                currencySymbol = instrument.currencySymbol,
            )
        }
    }

}

private fun List<TradeValidationError>.primaryInputError(inputMode: BuyInputMode): TradeValidationError? {
    val inputErrors = filter { error -> !error.isLimitPriceError() }
    return when (inputMode) {
        BuyInputMode.Amount -> inputErrors.firstOrNull { error -> error is TradeValidationError.OperationAmountAboveMax }
        BuyInputMode.Quantity -> inputErrors.firstOrNull { error -> error is TradeValidationError.NominalsOverMax }
            ?: inputErrors.firstOrNull { error -> error is TradeValidationError.NominalsOverAvailableBalance }
    }
        ?: inputErrors.firstOrNull()
}

private fun TradeValidationResult.visibleLimitPriceError(
    state: TradeViewModelState,
): TradeValidationError? {
    val limitPriceError = errors.firstOrNull { error -> error.isLimitPriceError() }
        ?: return null
    val userInteractedWithLimitPrice = state.isLimitPriceInputTouched || state.limitPriceInput.isNotBlank()

    return if (userInteractedWithLimitPrice) limitPriceError else null
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
    selectedAccount: Account?,
    availableArsAccounts: List<Account>,
): Account? =
    when {
        selectedAccount?.isArs == true -> selectedAccount
        availableArsAccounts.size == 1 -> availableArsAccounts.first()
        else -> null
    }

private fun calculateMockFee(tradeAmount: BigDecimal): BigDecimal =
    if (tradeAmount <= BigDecimal.ZERO) {
        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    } else {
        tradeAmount.multiply(MOCK_FEE_RATE).setScale(2, RoundingMode.HALF_UP)
    }

private fun buildMockConfirmationFees(tradeAmount: BigDecimal): TradeConfirmationState =
    TradeConfirmationState(
        fee = calculateMockFee(tradeAmount),
        marketFee = MOCK_MARKET_FEE,
        vat = MOCK_VAT,
    )

private val MOCK_FEE_RATE: BigDecimal = BigDecimal("0.00702")
private val MOCK_MARKET_FEE: BigDecimal = BigDecimal("5.00")
private val MOCK_VAT: BigDecimal = BigDecimal("300.00")
