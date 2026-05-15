package com.example.socketapp.ui.securities

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.BuyOrderType
import com.example.socketapp.BuySecurityUiState
import com.example.socketapp.TradeType
import com.example.socketapp.ui.theme.AppPrimary
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun TradeConfirmationScreen(
    uiState: BuySecurityUiState,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val instrument = uiState.instrument
    val validation = uiState.validation
    val accountValue = when (uiState.tradeCurrency) {
        "USD" -> uiState.accountContext.monetaryAccountUsd
        else -> uiState.accountContext.monetaryAccountArs
    }
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
            ConfirmationRow(label = "Comisiones")
            ConfirmationRow(label = "Derechos de mercado")
            ConfirmationRow(label = "IVA")

            Spacer(modifier = Modifier.height(8.dp))
            ConfirmationRow(
                label = estimatedTotalLabel,
                value = "",
                emphasized = true,
                showDivider = false,
            )
        }

        Button(
            onClick = onConfirm,
            enabled = uiState.canContinue,
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

private fun BigDecimal.toPlainQuantityString(): String =
    stripTrailingZeros().toPlainString()

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
