package com.wntechs.tankcontroller.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.data.model.TankModel
import com.wntechs.tankcontroller.ui.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBaseUrlChanged: (String) -> Unit,
    onDeviceIdChanged: (String) -> Unit,
    onSelectPreset: (TankModel, String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("App Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard("Connection") {
                OutlinedTextField(
                    value = uiState.baseUrl,
                    onValueChange = onBaseUrlChanged,
                    label = { Text("MQTT Broker IP/Host") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.deviceId,
                    onValueChange = onDeviceIdChanged,
                    label = { Text("Device Identifier (Topic)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            uiState.tankMeasurements?.tank_families?.forEach { family ->
                SectionCard("Preset: ${family.family.replace("_", " ").capitalize()}") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        family.models.forEach { model ->
                            TankPresetItem(
                                model = model,
                                onClick = { onSelectPreset(model, family.shape) }
                            )
                        }
                    }
                }
            }

            uiState.savedMessage?.let { MessageBanner(it) }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save Settings") }
        }
    }
}

@Composable
fun TankPresetItem(model: TankModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(model.code, fontWeight = FontWeight.Bold)
                Text("${model.capacity_litres} L", color = MaterialTheme.colorScheme.primary)
            }
            val dim = model.dimensions
            val dimText = if (dim.diameter_mm != null) {
                "H: ${dim.height_mm}mm, D: ${dim.diameter_mm}mm"
            } else {
                "L: ${dim.length_mm}mm, B: ${dim.breadth_mm}mm, H: ${dim.height_mm}mm"
            }
            Text(dimText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
