package com.example.socketapp.ui.securities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.Account
import com.example.socketapp.BuyOrderType
import com.example.socketapp.BuySecurityUiState
import com.example.socketapp.TradeValidationError
import com.example.socketapp.TradeType
import com.example.socketapp.ui.theme.AppPrimary
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeConfirmationScreen(
    uiState: BuySecurityUiState,
    onBack: () -> Unit,
    onFeeAccountSelected: (Account) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val instrument = uiState.instrument
    val validation = uiState.validation
    val confirmation = uiState.confirmation
    val accountValue = uiState.accountContext.selectedAccount.number
    val feeAccounts = uiState.accountContext.availableArsAccounts
    val feeAccount = uiState.accountContext.effectiveFeeAccount
    val needsFeeAccount = uiState.tradeCurrency == "USD"
    var showFeeAccountSheet by remember { mutableStateOf(false) }
    val isSell = uiState.tradeType == TradeType.Sell
    val estimatedTotalLabel = if (isSell) {
        "Monto estimado a acreditar"
    } else {
        "Monto estimado a debitar"
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
                .padding(top = 18.dp, bottom = 24.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "Revisá y confirmá",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = {},
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "Ayuda",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111111))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalance,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = instrument?.description?.takeIf { it.isNotBlank() } ?: instrument?.ticker.orEmpty(),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(28.dp))

            ConfirmationRow(
                label = if (isSell) "Cuenta a acreditar" else "Cuenta a debitar",
                value = accountValue,
            )
            ConfirmationRow(
                label = "Cuenta inversora",
                value = uiState.accountContext.investmentAccount,
            )
            if (needsFeeAccount) {
                FeeAccountRow(
                    account = feeAccount,
                    hasAccounts = feeAccounts.isNotEmpty(),
                    enabled = feeAccounts.size > 1,
                    onClick = { showFeeAccountSheet = true },
                )
            }
            ConfirmationRow(
                label = "Plazo",
                value = uiState.settlementTerm.label,
            )
            ConfirmationRow(
                label = "Orden",
                value = when (uiState.orderType) {
                    BuyOrderType.Limit -> "Precio limite"
                    BuyOrderType.Market -> "Mercado"
                },
            )
            ConfirmationRow(
                label = "Cantidad",
                value = validation.tradeNominals.toPlainQuantityString(),
            )
            ConfirmationRow(
                label = "Precio",
                value = validation.tradePrice.formatCurrency(),
            )
            ConfirmationRow(
                label = "Monto de la operacion",
                value = validation.tradeAmount.formatCurrency(),
            )
            ConfirmationRow(
                label = "Fee",
                value = confirmation.fee.formatCurrency(),
            )

            Spacer(modifier = Modifier.height(8.dp))
            ConfirmationRow(
                label = estimatedTotalLabel,
                value = confirmation.estimatedAmount.formatCurrency(),
                emphasized = true,
                showDivider = false,
            )
            confirmation.errors.firstOrNull()?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error.toConfirmationErrorMessage(),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.End,
                )
            }
        }

        Button(
            onClick = onConfirm,
            enabled = uiState.canContinue && confirmation.canConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppPrimary,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFBDBDBD),
                disabledContentColor = Color.White,
            ),
        ) {
            Text(
                text = if (isSell) "Confirmar venta" else "Confirmar compra",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }

    if (showFeeAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFeeAccountSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            FeeAccountSelectionSheet(
                accounts = feeAccounts,
                selected = feeAccount,
                onDismiss = { showFeeAccountSheet = false },
                onSelect = { account ->
                    onFeeAccountSelected(account)
                    showFeeAccountSheet = false
                },
            )
        }
    }
}

@Composable
private fun ConfirmationRow(
    label: String,
    value: String = "",
    emphasized: Boolean = false,
    showDivider: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun FeeAccountRow(
    account: Account?,
    hasAccounts: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val value = when {
        account != null -> account.number
        hasAccounts -> "Seleccionar cuenta"
        else -> "Sin cuenta en pesos"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Cuenta para fee",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (enabled) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun FeeAccountSelectionSheet(
    accounts: List<Account>,
    selected: Account?,
    onDismiss: () -> Unit,
    onSelect: (Account) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Cuenta para fee",
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

        accounts.forEachIndexed { index, account ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onSelect(account) },
                    ),
                verticalAlignment = Alignment.Top,
            ) {
                RadioButton(
                    selected = account == selected,
                    onClick = { onSelect(account) },
                )
                Spacer(modifier = Modifier.size(8.dp))
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = account.number,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = account.description.ifBlank { account.currency },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 17.sp,
                    )
                }
            }
            if (index != accounts.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 48.dp, top = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

private fun BigDecimal.toPlainQuantityString(): String =
    stripTrailingZeros().toPlainString()

private fun TradeValidationError.toConfirmationErrorMessage(): String =
    when (this) {
        TradeValidationError.InsufficientArsForFee -> "Saldo insuficiente en pesos para fee"
        TradeValidationError.MissingArsFeeAccount -> "No hay cuenta en pesos para debitar el fee"
        TradeValidationError.FeeAccountNotSelected -> "Selecciona una cuenta en pesos para debitar el fee"
        is TradeValidationError.InsufficientArs -> "Supera tu saldo disponible"
        is TradeValidationError.InsufficientUsd -> "Supera tu saldo disponible"
        TradeValidationError.SelectedAccountCurrencyMismatch -> "Selecciona una cuenta de la moneda del instrumento"
        else -> "No se puede confirmar la operacion"
    }

private fun BigDecimal.formatCurrency(): String {
    val plainValue = setScale(2, RoundingMode.HALF_UP).toPlainString()
    val integerPart = plainValue.substringBefore(".")
    val decimalPart = plainValue.substringAfter(".")
    val groupedInteger = integerPart
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()

    return "$$groupedInteger,$decimalPart"
}
