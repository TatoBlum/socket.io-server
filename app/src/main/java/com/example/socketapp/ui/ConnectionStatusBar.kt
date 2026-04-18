package com.example.socketapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.socketapp.model.ConnectionState
import com.example.socketapp.ui.theme.PriceUpText
import com.example.socketapp.ui.theme.StatusWarning

@Composable
fun ConnectionStatusBar(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val errorColor = MaterialTheme.colorScheme.error
        val (label, color) = when (val state = connectionState) {
            ConnectionState.Disconnected -> "Desconectado" to errorColor
            ConnectionState.Connecting -> "Conectando..." to StatusWarning
            ConnectionState.Connected -> "Conectado" to PriceUpText
            is ConnectionState.Failed -> "Error: ${state.cause.message ?: "desconocido"}" to errorColor
        }
        Text(text = label, color = color, style = MaterialTheme.typography.bodyMedium)

        when (connectionState) {
            ConnectionState.Connected -> {
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = errorColor,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Cerrar")
                }
            }
            ConnectionState.Disconnected, is ConnectionState.Failed -> {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PriceUpText,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Abrir")
                }
            }
            ConnectionState.Connecting -> {}
        }
    }
}
