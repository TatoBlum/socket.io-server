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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import com.example.socketapp.BuySecurityUiState
import com.example.socketapp.normalizedCurrency
import com.example.socketapp.ui.theme.AppPrimary
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun TradeAccountSelectionScreen(
    uiState: BuySecurityUiState,
    onAccountSelected: (Account) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val instrument = uiState.instrument
    val tradeCurrency = instrument?.currency?.normalizedCurrency()
    val accounts = remember(uiState.accounts, tradeCurrency) {
        if (tradeCurrency == null) {
            emptyList()
        } else {
            uiState.accounts.filter { account -> account.currency.normalizedCurrency() == tradeCurrency }
        }
    }
    var selectedAccount by remember(accounts) {
        mutableStateOf(accounts.firstOrNull())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        Text(
            text = "Selecciona una cuenta",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        instrument?.let { loadedInstrument ->
            Text(
                text = loadedInstrument.description.takeIf { it.isNotBlank() } ?: loadedInstrument.ticker,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        } ?: Text(
            text = "Cargando especie",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(22.dp))

        when {
            instrument == null -> LoadingAccountState()
            accounts.isEmpty() -> EmptyAccountState(currency = tradeCurrency.orEmpty())
            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                ) {
                    items(
                        items = accounts,
                        key = { account -> account.number },
                    ) { account ->
                        AccountSelectionRow(
                            account = account,
                            selected = account == selectedAccount,
                            onClick = { selectedAccount = account },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        Button(
            onClick = {
                selectedAccount?.let { account ->
                    onAccountSelected(account)
                    onContinue()
                }
            },
            enabled = instrument != null && selectedAccount != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppPrimary,
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
}

@Composable
private fun AccountSelectionRow(
    account: Account,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalance,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accountTitle(account),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Disponible ${account.tradingBalances.marketNow.formatCurrency()}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}

@Composable
private fun LoadingAccountState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Cargando cuentas",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyAccountState(currency: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No tenes cuentas disponibles en $currency",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Selecciona otra especie o intenta mas tarde.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

private fun accountTitle(account: Account): String =
    "${account.type.ifBlank { "Cuenta" }} ${account.currency.normalizedCurrency()} ${account.number}"

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
