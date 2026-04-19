package com.wntechs.tankcontroller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.data.model.TankFamily
import com.wntechs.tankcontroller.data.model.TankModel
import com.wntechs.tankcontroller.ui.viewmodel.AuthUiState
import com.wntechs.tankcontroller.ui.viewmodel.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    authUiState: AuthUiState,
    onFamilySelected: (TankFamily?) -> Unit,
    onModelSelected: (TankModel?) -> Unit,
    onApplyPreset: () -> Unit,
    onLogout: () -> Unit,
    onResetDevice: () -> Unit,
    onRefreshSensors: () -> Unit,
    onSelectSensor: (Int) -> Unit,
    onUnselectSensor: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Device?") },
            text = { Text("This will revoke existing MQTT credentials. You will need to re-provision the device hardware, but you will remain the owner. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetDevice()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // User Account Section
        SectionCard("User Account") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(authUiState.name ?: "Unknown User", fontWeight = FontWeight.Bold)
                Text(authUiState.email ?: "No email", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
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

        SectionCard("Wireless Sensor Pairing") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Available Sensors", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (uiState.pairedSensorId.isNotBlank())
                                "Paired: ${uiState.pairedSensorId}" else "No sensor paired",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    OutlinedButton(
                        onClick = onRefreshSensors,
                        enabled = !uiState.isFetchingSensors,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        if (uiState.isFetchingSensors) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh")
                        }
                    }
                }

                var sensorExpanded by remember { mutableStateOf(false) }
                val selectedSensor = uiState.sensorList.find { it.paired || it.selected }

                ExposedDropdownMenuBox(
                    expanded = sensorExpanded,
                    onExpandedChange = { sensorExpanded = !sensorExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSensor?.id ?: "Select a sensor",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Discovered Sensors") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sensorExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = sensorExpanded,
                        onDismissRequest = { sensorExpanded = false }
                    ) {
                        if (uiState.sensorList.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No sensors found. Refresh to scan.") },
                                onClick = { sensorExpanded = false },
                                enabled = false
                            )
                        } else {
                            uiState.sensorList.forEach { sensor ->
                                DropdownMenuItem(
                                    text = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(sensor.id)
                                            if (sensor.paired) {
                                                Text("(Paired)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectSensor(sensor.index)
                                        sensorExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (uiState.pairedSensorId.isNotBlank()) {
                    TextButton(
                        onClick = onUnselectSensor,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Unpair Current Sensor")
                    }
                }
            }
        }
        // Recovery Section
        SectionCard("Device Maintenance") {
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                enabled = uiState.currentDeviceId.isNotBlank()
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.padding(end = 8.dp))
                Text("Request Device Reset")
            }
            Text(
                "Use this if you need to re-provision the hardware. Ownership will be retained.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        uiState.savedMessage?.let { MessageBanner(it) }
        uiState.error?.let { MessageBanner(it, isError = true) }
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
