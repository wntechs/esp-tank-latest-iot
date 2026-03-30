package com.wntechs.tankcontroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.data.model.StatusResponse
import com.wntechs.tankcontroller.ui.theme.TankControllerTheme
import com.wntechs.tankcontroller.ui.viewmodel.DashboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onOpenManual: () -> Unit,
    onOpenConfig: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Water Tank Dashboard") }) }) { padding ->


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {



            WaterLevelCard(uiState)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

                StatusCard("Sensor", if (uiState.status.sensorConnected) "CONNECTED" else "TIMEOUT")
                StatusCard("Connected URL", uiState.baseUrl.ifBlank { "Not connected" })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusCard("Motor", if (uiState.status.motorOn) "ON" else "OFF")
                StatusCard("Mode", if (uiState.status.autoModeEnabled) "AUTO" else "MANUAL")
                StatusCard("Reading", if (uiState.status.readingValid) "VALID" else "INVALID")

            }

            if (uiState.error != null) MessageBanner(uiState.error, isError = true)
            if (uiState.message != null) MessageBanner(uiState.message)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text(if (uiState.loading) "Refreshing..." else "Refresh") }

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
                baseUrl = "http://192.168.1.100",
                status = StatusResponse(
                    motorOn = true,
                    autoModeEnabled = true,
                    sensorConnected = true,
                    readingValid = true,
                    waterLevelPercent = 75,
                    litres = 1500,
                    waterHeightMm = 1200,
                    filteredDistanceMm = 400
                )
            ),
            onRefresh = {},
            onOpenManual = {},
            onOpenConfig = {}
        )
    }
}
