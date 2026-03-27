package com.wntechs.tankcontroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.ui.viewmodel.DashboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualControlScreen(
    uiState: DashboardUiState,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onReturnAuto: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Manual Relay Control") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Manual override", fontWeight = FontWeight.SemiBold)
                    Text("/relay/manual enables manual override and forces motor state immediately.")
                    Text("/relay/auto returns control back to automatic logic.")
                    Text("Device notes say manual override may auto-timeout after about 30 minutes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current state", fontWeight = FontWeight.SemiBold)
                    Text(if (uiState.status.motorOn) "Motor is ON" else "Motor is OFF")
                    Text(if (uiState.status.manualOverride) "Manual override active" else "Automatic mode active")
                }
            }

            if (uiState.error != null) MessageBanner(uiState.error, isError = true)
            if (uiState.message != null) MessageBanner(uiState.message)

            Button(onClick = onTurnOn, modifier = Modifier.fillMaxWidth()) { Text("Turn Motor ON") }
            OutlinedButton(onClick = onTurnOff, modifier = Modifier.fillMaxWidth()) { Text("Turn Motor OFF") }
            OutlinedButton(onClick = onReturnAuto, modifier = Modifier.fillMaxWidth()) { Text("Return to Auto Mode") }
        }
    }
}
