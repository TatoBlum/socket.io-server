package com.example.socketapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socketapp.ConnectionState

private val ConnectedColor = Color(0xFF4CAF50)
private val DisconnectedColor = Color(0xFFF44336)
private val ConnectingColor = Color(0xFFFFC107)

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
        val (label, color) = when (val state = connectionState) {
            ConnectionState.Disconnected -> "Desconectado" to DisconnectedColor
            ConnectionState.Connecting -> "Conectando..." to ConnectingColor
            ConnectionState.Connected -> "Conectado" to ConnectedColor
            is ConnectionState.Failed -> "Error: ${state.cause.message ?: "desconocido"}" to DisconnectedColor
        }
        Text(text = label, color = color, fontSize = 14.sp)

        when (connectionState) {
            ConnectionState.Connected -> {
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = DisconnectedColor),
                ) {
                    Text("Cerrar")
                }
            }
            ConnectionState.Disconnected, is ConnectionState.Failed -> {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectedColor),
                ) {
                    Text("Abrir")
                }
            }
            ConnectionState.Connecting -> {}
        }
    }
}
