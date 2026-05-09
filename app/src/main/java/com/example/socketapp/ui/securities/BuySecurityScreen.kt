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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.BuyInputMode
import com.example.socketapp.BuyOrderType
import com.example.socketapp.BuySecurityUiState
import com.example.socketapp.BuyableInstrument
import com.example.socketapp.ui.theme.GaliciaPrimary
import com.example.socketapp.ui.theme.PriceUpText
import java.math.RoundingMode

private enum class SettlementTerm(
    val label: String,
    val description: String,
) {
    TwentyFourHours(
        label = "24 h",
        description = "La compra se ejecuta hoy y el dinero se liquida el dia habil siguiente.",
    ),
    Today(
        label = "Hoy",
        description = "La compra se ejecuta hoy y el dinero se liquida el mismo dia.",
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuySecurityScreen(
    uiState: BuySecurityUiState,
    onInputModeChange: (BuyInputMode) -> Unit,
    onInputChange: (String) -> Unit,
    onOrderTypeChange: (BuyOrderType) -> Unit,
    onLimitPriceChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var settlementTerm by remember { mutableStateOf(SettlementTerm.Today) }
    var activeSheet by remember { mutableStateOf<BuySheet?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 24.dp),
    ) {
        SecurityHeader(instrument = uiState.instrument)

        Spacer(modifier = Modifier.height(26.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectorButton(
                label = "Plazo: ${settlementTerm.label}",
                selected = true,
                onClick = { activeSheet = BuySheet.Settlement },
                modifier = Modifier.weight(1f),
            )
            SelectorButton(
                label = "Orden: ${uiState.orderType.label}",
                selected = false,
                onClick = { activeSheet = BuySheet.OrderType },
                modifier = Modifier.weight(1f),
            )
        }

        if (uiState.orderType == BuyOrderType.Limit) {
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = uiState.limitPriceInput,
                onValueChange = onLimitPriceChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Precio limite") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(8.dp),
            )
        }

        Spacer(modifier = Modifier.height(72.dp))

        PurchaseInputSection(
            mode = uiState.inputMode,
            value = uiState.activeInput,
            available = "$ 1.159.000,00",
            validationMessage = uiState.validationMessage,
            onModeChange = onInputModeChange,
            onValueChange = onInputChange,
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = {},
            enabled = uiState.validation.canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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

    when (activeSheet) {
        BuySheet.Settlement -> ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            SelectionSheet(
                title = "Plazo de liquidacion",
                options = SettlementTerm.entries,
                selected = settlementTerm,
                label = { it.label },
                description = { it.description },
                onDismiss = { activeSheet = null },
                onSelect = {
                    settlementTerm = it
                    activeSheet = null
                },
            )
        }

        BuySheet.OrderType -> ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            SelectionSheet(
                title = "Tipo de orden",
                options = BuyOrderType.entries,
                selected = uiState.orderType,
                label = { it.label },
                description = { it.description },
                onDismiss = { activeSheet = null },
                onSelect = {
                    onOrderTypeChange(it)
                    activeSheet = null
                },
            )
        }

        null -> Unit
    }
}

@Composable
private fun PurchaseInputSection(
    mode: BuyInputMode,
    value: String,
    available: String,
    validationMessage: String?,
    onModeChange: (BuyInputMode) -> Unit,
    onValueChange: (String) -> Unit,
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
            PurchaseModeChip(
                text = BuyInputMode.Amount.label,
                selected = mode == BuyInputMode.Amount,
                onClick = { onModeChange(BuyInputMode.Amount) },
            )
            PurchaseModeChip(
                text = BuyInputMode.Quantity.label,
                selected = mode == BuyInputMode.Quantity,
                onClick = { onModeChange(BuyInputMode.Quantity) },
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        BasicTextField(
            value = value,
            onValueChange = { nextValue ->
                onValueChange(nextValue.filter { it.isDigit() || it == ',' || it == '.' })
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(GaliciaPrimary),
            textStyle = MaterialTheme.typography.displayMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                fontSize = 52.sp,
                lineHeight = 58.sp,
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
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
            },
        )

        HorizontalDivider(
            color = GaliciaPrimary,
            thickness = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp, vertical = 8.dp),
        )

        Text(
            text = if (mode == BuyInputMode.Amount) {
                "Disponible $available"
            } else {
                "Disponible para comprar $available"
            },
            color = if (validationMessage == null) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (validationMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = validationMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PurchaseModeChip(
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
                color = if (selected) GaliciaPrimary else MaterialTheme.colorScheme.outline,
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
            color = if (selected) GaliciaPrimary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SecurityHeader(instrument: BuyableInstrument) {
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
                text = instrument.lastPrice.formatCurrency(),
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
                color = if (selected) GaliciaPrimary else MaterialTheme.colorScheme.outline,
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

private enum class BuySheet {
    Settlement,
    OrderType,
}

private fun java.math.BigDecimal.formatCurrency(): String {
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

private fun java.math.BigDecimal.formatSignedPercent(): String {
    val sign = if (this >= java.math.BigDecimal.ZERO) "+" else "-"
    return "$sign${abs().setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", ",")}%"
}
