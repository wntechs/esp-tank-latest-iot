package com.wntechs.tankcontroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.data.model.StatusResponse
import com.wntechs.tankcontroller.data.repository.MqttConnectionState
import com.wntechs.tankcontroller.ui.theme.TankControllerTheme
import com.wntechs.tankcontroller.ui.viewmodel.DashboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onReturnAuto: () -> Unit,
    onOpenConfig: () -> Unit,
    onNavigateToDiscovery: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Connection Status Header
        ConnectionStatusCard(uiState.connectionState)

        if (uiState.baseUrl.isBlank()) {
            SectionCard("No Device Paired") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("You haven't paired any water tank controller yet.")
                    Button(
                        onClick = onNavigateToDiscovery,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pair New Device")
                    }
                }
            }
        } else {
            WaterLevelCard(uiState)

            // Current State / Status Indicators
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatusCard("Sensor", if (uiState.status.sensorConnected) "CONNECTED" else "TIMEOUT")
                StatusCard("Motor", if (uiState.status.motorOn) "ON" else "OFF")
                StatusCard("Mode", if (uiState.status.manualOverride) "MANUAL" else "AUTO")
                StatusCard("Reading", if (uiState.status.readingValid) "VALID" else "INVALID")
                StatusCard("Device UUID", uiState.baseUrl)
            }

            // Manual Relay Control Section
            SectionCard("Manual Relay Control") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            enabled = !uiState.status.motorOn && uiState.connectionState is MqttConnectionState.Connected,
                            onClick = onTurnOn,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)) // Green
                        ) {
                            Text("Turn ON")
                        }
                        Button(
                            enabled = uiState.status.motorOn && uiState.connectionState is MqttConnectionState.Connected,
                            onClick = onTurnOff,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Turn OFF")
                        }
                    }

                    OutlinedButton(
                        onClick = onReturnAuto,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.status.manualOverride && uiState.connectionState is MqttConnectionState.Connected
                    ) {
                        Text("Return to Auto Mode")
                    }

                    if (uiState.status.manualOverride) {
                        Text(
                            "Manual override active. Auto logic is disabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        if (uiState.error != null) MessageBanner(uiState.error, isError = true)
        if (uiState.message != null) MessageBanner(uiState.message)
    }
}

@Composable
fun ConnectionStatusCard(state: MqttConnectionState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is MqttConnectionState.Connected -> Color(0xFFE8F5E9)
                is MqttConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                MqttConnectionState.Connecting -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Connecting to MQTT...", style = MaterialTheme.typography.bodyMedium)
                }
                MqttConnectionState.Connected -> {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF2E7D32))
                    Text("Connected to Cloud", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32))
                }
                is MqttConnectionState.Error -> {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Connection Error", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                MqttConnectionState.Disconnected -> {
                    Icon(Icons.Default.CloudOff, contentDescription = null)
                    Text("Disconnected", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(label: String, value: String) {
    Card {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    TankControllerTheme {
        DashboardScreen(
            uiState = DashboardUiState(
                baseUrl = "dca344fb-f89e-478f-8f5a-8d34c3486f77",
                status = StatusResponse(
                    motorOn = true,
                    autoModeEnabled = false,
                    manualOverride = true,
                    sensorConnected = true,
                    readingValid = true,
                    waterLevelPercent = 75,
                    litres = 1500,
                    waterHeightMm = 1200,
                    filteredDistanceMm = 400
                ),
                connectionState = MqttConnectionState.Connected
            ),
            onTurnOn = {},
            onTurnOff = {},
            onReturnAuto = {},
            onOpenConfig = {},
            onNavigateToDiscovery = {}
        )
    }
}
