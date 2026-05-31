package com.example.socketapp.ui.securities

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.BuyInputMode
import com.example.socketapp.TradeOrderType
import com.example.socketapp.TradeViewModelState
import com.example.socketapp.R
import com.example.socketapp.Security
import com.example.socketapp.SettlementType
import com.example.socketapp.TradeInputHelper
import com.example.socketapp.TradeInputLimitPriceHelper
import com.example.socketapp.TradeOption
import com.example.socketapp.TradeValidationError
import com.example.socketapp.ui.theme.AppPrimary
import com.example.socketapp.ui.theme.PriceUpText
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(
    uiState: TradeViewModelState,
    onInputModeChange: (BuyInputMode) -> Unit,
    onInputChange: (BuyInputMode, String) -> Unit,
    onSettlementTermChange: (SettlementType) -> Unit,
    onOrderTypeChange: (TradeOrderType) -> Unit,
    onLimitPriceChange: (String) -> Unit,
    onTradeOptionsChange : (TradeOption) -> Unit = {},
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSheet by remember { mutableStateOf<TradeSheet?>(null) }
    var orderSheetKeyboardWasVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val isOrderTypeSheetOpen = activeSheet == TradeSheet.OrderType

    LaunchedEffect(
        isOrderTypeSheetOpen,
        isKeyboardVisible,
    ) {
        if (!isOrderTypeSheetOpen) {
            orderSheetKeyboardWasVisible = false
            return@LaunchedEffect
        }

        if (isKeyboardVisible) {
            orderSheetKeyboardWasVisible = true
            return@LaunchedEffect
        }

        if (orderSheetKeyboardWasVisible) {
            orderSheetKeyboardWasVisible = false
            activeSheet = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
        ) {
            if (uiState.instrument != null) {
                SecurityHeader(instrument = uiState.instrument)
            } else {
                SecurityHeaderPlaceholder()
            }

            Spacer(modifier = Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick =  {
                        onTradeOptionsChange(TradeOption.SIMPLE)
                    }
                ) {
                    Text("Simple")
                }

                Button(onClick = {
                    onTradeOptionsChange(TradeOption.ADVANCE)
                }) {
                    Text("Avanzado")
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            if (uiState.tradeOption == TradeOption.ADVANCE) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectorButton(
                    label = "Plazo: ${uiState.settlementTerm.label}",
                    selected = true,
                    onClick = { activeSheet = TradeSheet.Settlement },
                    modifier = Modifier.weight(1f),
                )
                SelectorButton(
                    label = "Orden: ${uiState.orderType.label}",
                    selected = false,
                    onClick = { activeSheet = TradeSheet.OrderType },
                    modifier = Modifier.weight(1f),
                )
            } }

            if (uiState.orderType == TradeOrderType.Limit && TradeOption.ADVANCE == uiState.tradeOption) {
                Spacer(modifier = Modifier.height(14.dp))
                LimitPriceInput(
                    value = uiState.limitPriceInput,
                    error = uiState.limitPriceError,
                    helper = uiState.limitPriceHelper,
                    onValueChange = onLimitPriceChange,
                )
            }

            Spacer(modifier = Modifier.height(72.dp))

            TradeInputSection(
                mode = uiState.inputMode,
                value = uiState.activeInputText,
                helperMessage = uiState.inputHelper.toInputHelperMessage(),
                errorMessage = uiState.inputError?.toInputErrorMessage(),
                onModeChange = { inputMode ->
                    focusManager.clearFocus(force = true)
                    onInputModeChange(inputMode)
                },
                onValueChange = onInputChange,
            )
        }

        TradeContinueButton(
            enabled = uiState.canContinue,
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        )
    }

    when (activeSheet) {
        TradeSheet.Settlement -> ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            SelectionSheet(
                title = "Plazo de liquidacion",
                options = SettlementType.entries,
                selected = uiState.settlementTerm,
                label = { it.label },
                description = { it.description },
                onDismiss = { activeSheet = null },
                onSelect = {
                    onSettlementTermChange(it)
                    activeSheet = null
                },
            )
        }

        TradeSheet.OrderType -> ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            OrderTypeSheet(
                selected = uiState.orderType,
                limitPriceInput = uiState.limitPriceInput,
                limitPriceError = uiState.limitPriceError,
                limitPriceHelper = uiState.limitPriceHelper,
                onDismiss = { activeSheet = null },
                onSelect = { orderType ->
                    onOrderTypeChange(orderType)
                    if (orderType == TradeOrderType.Market) {
                        orderSheetKeyboardWasVisible = false
                        activeSheet = null
                    }
                },
                onLimitPriceChange = onLimitPriceChange,
            )
        }

        null -> Unit
    }
}

@Composable
private fun LimitPriceInput(
    value: String,
    error: TradeValidationError?,
    helper: TradeInputLimitPriceHelper,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth(),
        singleLine = true,
        label = { Text("Precio limite") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        shape = RoundedCornerShape(8.dp),
        isError = error != null,
        supportingText = {
            Text(
                text = error?.toInputErrorMessage()
                    ?: helper.toLimitPriceHelperMessage(),
            )
        },
    )
}

@Composable
private fun TradeContinueButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2D2D2D),
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFBDBDBD),
            disabledContentColor = Color.White,
        ),
    ) {
        Text(
            text = "Continuar",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun TradeInputSection(
    mode: BuyInputMode,
    value: String,
    helperMessage: String,
    errorMessage: String?,
    onModeChange: (BuyInputMode) -> Unit,
    onValueChange: (BuyInputMode, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TradeInputModeChip(
                text = BuyInputMode.Amount.label,
                selected = mode == BuyInputMode.Amount,
                onClick = { onModeChange(BuyInputMode.Amount) },
            )
            TradeInputModeChip(
                text = BuyInputMode.Quantity.label,
                selected = mode == BuyInputMode.Quantity,
                onClick = { onModeChange(BuyInputMode.Quantity) },
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        key(mode) {
            BasicTextField(
                value = value,
                onValueChange = { nextValue ->
                    val filteredValue = when (mode) {
                        BuyInputMode.Amount -> nextValue.filter { it.isDigit() || it == ',' || it == '.' }
                        BuyInputMode.Quantity -> nextValue.filter { it.isDigit() || it == '.' }
                    }
                    onValueChange(mode, filteredValue)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(AppPrimary),
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    fontSize = 52.sp,
                    lineHeight = 58.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (mode == BuyInputMode.Amount) {
                                Text(
                                    text = "$",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 32.sp,
                                    lineHeight = 40.sp,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Box(contentAlignment = Alignment.Center) {
                                if (value.isBlank()) {
                                    Text(
                                        text = "0",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 52.sp,
                                        lineHeight = 58.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                innerTextField()
                            }
                        }

                        HorizontalDivider(
                            color = AppPrimary,
                            thickness = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 34.dp, vertical = 8.dp),
                        )

                        Text(
                            text = helperMessage,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun TradeInputModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.dp,
                color = if (selected) AppPrimary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(22.dp),
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) AppPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SecurityHeaderPlaceholder() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Cargando instrumento",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SecurityHeader(instrument: Security) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalance,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = instrument.ticker,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = instrument.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = instrument.price.formatCurrency(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = instrument.dailyVariationPercent.formatSignedPercent(),
                color = PriceUpText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SelectorButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (selected) AppPrimary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun <T> SelectionSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    description: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        options.forEachIndexed { index, option ->
            SheetOptionRow(
                selected = option == selected,
                title = label(option),
                description = description(option),
                onClick = { onSelect(option) },
            )
            if (index != options.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 48.dp, top = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun OrderTypeSheet(
    selected: TradeOrderType,
    limitPriceInput: String,
    limitPriceError: TradeValidationError?,
    limitPriceHelper: TradeInputLimitPriceHelper,
    onDismiss: () -> Unit,
    onSelect: (TradeOrderType) -> Unit,
    onLimitPriceChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Tipo de orden",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TradeOrderType.entries.forEachIndexed { index, orderType ->
            SheetOptionRow(
                selected = orderType == selected,
                title = orderType.label,
                description = orderType.description,
                onClick = { onSelect(orderType) },
            )

            if (orderType == TradeOrderType.Limit && selected == TradeOrderType.Limit) {
                Spacer(modifier = Modifier.height(12.dp))
                LimitPriceInput(
                    value = limitPriceInput,
                    error = limitPriceError,
                    helper = limitPriceHelper,
                    onValueChange = onLimitPriceChange,
                    modifier = Modifier.padding(start = 48.dp),
                )
            }

            if (index != TradeOrderType.entries.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 48.dp, top = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SheetOptionRow(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 17.sp,
            )
        }
    }
}

private enum class TradeSheet {
    Settlement,
    OrderType,
}

@Composable
private fun TradeInputHelper.toInputHelperMessage(): String =
    when (this) {
        TradeInputHelper.None -> ""
        is TradeInputHelper.AvailableBalance ->
            stringResource(R.string.trade_helper_available_balance, amount.formatCurrency())
        is TradeInputHelper.AvailableToBuy ->
            stringResource(R.string.trade_helper_available_to_buy, amount.formatCurrency())
        is TradeInputHelper.AvailableNominalsToBuy ->
            stringResource(R.string.trade_helper_available_nominals_to_buy, quantity.toNominalsString())
        is TradeInputHelper.AvailableNominals ->
            stringResource(R.string.trade_helper_available_nominals, quantity.toString())
        is TradeInputHelper.EquivalentAmount ->
            stringResource(R.string.trade_helper_equivalent_amount, amount.formatCurrency())
        is TradeInputHelper.ApproximateDebit ->
            stringResource(R.string.trade_helper_approximate_debit, amount.formatCurrency())
        is TradeInputHelper.ApproximateCredit ->
            stringResource(R.string.trade_helper_approximate_credit, amount.formatCurrency())
    }

@Composable
private fun TradeInputLimitPriceHelper.toLimitPriceHelperMessage(): String =
    when (this) {
        TradeInputLimitPriceHelper.None -> ""
        is TradeInputLimitPriceHelper.MaxAllowed ->
            stringResource(
                R.string.trade_helper_limit_price_max_allowed,
                amount.formatCurrency(currencySymbol),
            )
        is TradeInputLimitPriceHelper.MinAllowed ->
            stringResource(
                R.string.trade_helper_limit_price_min_allowed,
                amount.formatCurrency(currencySymbol),
            )
    }

@Composable
private fun TradeValidationError.toInputErrorMessage(): String =
    when (this) {
        TradeValidationError.InvalidLimitPrice -> stringResource(R.string.trade_error_invalid_limit_price)
        is TradeValidationError.LimitPriceOutOfBandBuy ->
            stringResource(R.string.trade_error_limit_price_out_of_band_buy, maxAllowed.toPlainMoneyString())
        is TradeValidationError.LimitPriceOutOfBandSell ->
            stringResource(R.string.trade_error_limit_price_out_of_band_sell, minAllowed.toPlainMoneyString())
        is TradeValidationError.LimitPriceNotMultiple ->
            stringResource(R.string.trade_error_limit_price_not_multiple, step.toPlainString())
        TradeValidationError.MissingTradePrice -> stringResource(R.string.trade_error_missing_trade_price)
        is TradeValidationError.AmountNotEnoughForMin ->
            stringResource(R.string.trade_error_amount_not_enough_for_min, minAmount.formatCurrency())
        TradeValidationError.NominalsInvalid -> stringResource(R.string.trade_error_nominals_invalid)
        is TradeValidationError.NominalsBelowMin ->
            stringResource(R.string.trade_error_nominals_below_min, minNominals.toPlainString())
        is TradeValidationError.NominalsNotMultiple ->
            stringResource(R.string.trade_error_nominals_not_multiple, lotSize.toPlainString())
        is TradeValidationError.NominalsOverMax ->
            stringResource(R.string.trade_error_nominals_over_max, maxNominals.toPlainString())
        is TradeValidationError.NominalsOverAvailableBalance ->
            stringResource(R.string.trade_error_nominals_over_available_balance)
        is TradeValidationError.NotEnoughNominals -> stringResource(R.string.trade_error_not_enough_available)
        is TradeValidationError.NotEnoughAvailableAmount -> stringResource(R.string.trade_error_not_enough_available)
        is TradeValidationError.InsufficientArs -> stringResource(R.string.trade_error_insufficient_balance)
        TradeValidationError.InsufficientArsForFee -> stringResource(R.string.trade_error_insufficient_ars_for_fee)
        TradeValidationError.MissingArsFeeAccount -> stringResource(R.string.trade_error_missing_ars_fee_account)
        TradeValidationError.FeeAccountNotSelected -> stringResource(R.string.trade_error_fee_account_not_selected)
        is TradeValidationError.InsufficientUsd -> stringResource(R.string.trade_error_insufficient_balance)
        TradeValidationError.SelectedAccountCurrencyMismatch ->
            stringResource(R.string.trade_error_selected_account_currency_mismatch)
        is TradeValidationError.OperationAmountBelowMin ->
            stringResource(R.string.trade_error_amount_not_enough_for_min, minAmount.formatCurrency())
        is TradeValidationError.OperationAmountAboveMax ->
            stringResource(R.string.trade_error_operation_amount_above_max, maxAmount.formatCurrency())
    }

private fun BigDecimal.toPlainMoneyString(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString()

private fun BigDecimal.toNominalsString(): String =
    stripTrailingZeros().toPlainString()

private fun BigDecimal.formatSignedPercent(): String {
    val sign = if (this >= java.math.BigDecimal.ZERO) "+" else "-"
    return "$sign${abs().setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", ",")}%"
}
