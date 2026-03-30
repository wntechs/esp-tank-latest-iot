package com.wntechs.tankcontroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.ui.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBaseUrlChanged: (String) -> Unit,
    onDeviceIdChanged: (String) -> Unit, // Replaces onPreferMdnsChanged
    onSave: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("App Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.baseUrl,
                onValueChange = onBaseUrlChanged,
                label = { Text("MQTT Broker IP/Host") }
            )

            OutlinedTextField(
                value = uiState.deviceId,
                onValueChange = onDeviceIdChanged,
                label = { Text("Device Identifier (Topic)") }
            )
            uiState.savedMessage?.let { MessageBanner(it) }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save Settings") }

        }
    }
}
