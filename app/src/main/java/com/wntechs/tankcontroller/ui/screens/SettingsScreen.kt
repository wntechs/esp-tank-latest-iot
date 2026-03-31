package com.wntechs.tankcontroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.data.model.TankFamily
import com.wntechs.tankcontroller.data.model.TankModel
import com.wntechs.tankcontroller.ui.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBaseUrlChanged: (String) -> Unit,
    onDeviceIdChanged: (String) -> Unit,
    onFamilySelected: (TankFamily?) -> Unit,
    onModelSelected: (TankModel?) -> Unit,
    onApplyPreset: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("App Settings") },
        // Add this line to remove the automatic top padding
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) }) { padding ->
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
                
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { 
                    Text("Save Connection Settings") 
                }
            }

            SectionCard("Tank Preset Configuration") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. Choose Family/Type
                    var familyExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = familyExpanded,
                        onExpandedChange = { familyExpanded = !familyExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedFamily?.family?.formatFamilyName() ?: "Select Tank Type",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tank Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = familyExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = familyExpanded,
                            onDismissRequest = { familyExpanded = false }
                        ) {
                            uiState.tankMeasurements?.tank_families?.forEach { family ->
                                DropdownMenuItem(
                                    text = { Text(family.family.formatFamilyName()) },
                                    onClick = {
                                        onFamilySelected(family)
                                        familyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Choose Model (if family selected)
                    if (uiState.selectedFamily != null) {
                        var modelExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = modelExpanded,
                            onExpandedChange = { modelExpanded = !modelExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.selectedModel?.code ?: "Select Capacity",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Model / Capacity") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = modelExpanded,
                                onDismissRequest = { modelExpanded = false }
                            ) {
                                uiState.selectedFamily.models.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text("${model.code} (${model.capacity_litres} L)") },
                                        onClick = {
                                            onModelSelected(model)
                                            modelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Display Measurements & Apply Button
                    uiState.selectedModel?.let { model ->
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Specifications:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            val dim = model.dimensions
                            if (dim.diameter_mm != null) {
                                MeasurementRow("Height", "${dim.height_mm} mm")
                                MeasurementRow("Diameter", "${dim.diameter_mm} mm")
                            } else {
                                MeasurementRow("Length", "${dim.length_mm} mm")
                                MeasurementRow("Breadth", "${dim.breadth_mm} mm")
                                MeasurementRow("Height", "${dim.height_mm} mm")
                            }
                            MeasurementRow("Capacity", "${model.capacity_litres} Litres")
                            
                            Button(
                                onClick = onApplyPreset,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("Apply Preset to Device")
                            }
                        }
                    }
                }
            }

            uiState.savedMessage?.let { MessageBanner(it) }
        }
    }
}

@Composable
fun MeasurementRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun String.formatFamilyName() = this.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
