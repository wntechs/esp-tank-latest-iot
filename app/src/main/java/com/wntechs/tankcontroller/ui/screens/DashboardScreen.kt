package com.wntechs.tankcontroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onReboot: () -> Unit,
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

        if (uiState.deviceId.isBlank()) {
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
            WaterLevelCard(
                uiState = uiState,
                onTurnOn = onTurnOn,
                onTurnOff = onTurnOff,
                onToggleAuto = {
                    if (uiState.status.manualOverride) {
                        onReturnAuto()
                    } else {
                        // If the user flips from Auto to Manual,
                        // we default to current pump state but allow manual control
                        onTurnOff() // Or a specific 'setManual' command if your MQTT supports it
                    }
                })




            SectionCard("Maintenance") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            enabled = uiState.connectionState is MqttConnectionState.Connected,
                            onClick = onReboot,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)) // Green
                        ) {
                            Text("Reboot Controller")
                        }

                    }

                }
            }
        }

        if (uiState.error != null) MessageBanner(uiState.error, isError = true)
        if (uiState.message != null) MessageBanner(uiState.message)
    }
}





@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    TankControllerTheme {
        DashboardScreen(
            uiState = DashboardUiState(
                deviceId = "dca344fb-f89e-478f-8f5a-8d34c3486f77",
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
            onReboot = {},
            onNavigateToDiscovery = {},
        )
    }
}
