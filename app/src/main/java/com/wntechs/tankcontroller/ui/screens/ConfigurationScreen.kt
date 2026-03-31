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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.ui.viewmodel.ConfigurationForm
import com.wntechs.tankcontroller.ui.viewmodel.ConfigurationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    uiState: ConfigurationUiState,
    onUpdateField: ((ConfigurationForm) -> ConfigurationForm) -> Unit,
    onSave: () -> Unit,
) {
    val form = uiState.form
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Group 1: Tank Dimensions
        SectionCard("Tank Dimensions") {
            NumberField(
                label = "Height (mm)",
                value = form.tankHeightMm,
                onValueChange = { v -> onUpdateField { it.copy(tankHeightMm = v) } }
            )
            if (form.tankShape == 0) { // Cylindrical
                NumberField(
                    label = "Diameter (mm)",
                    value = form.tankDiameterMm,
                    onValueChange = { v -> onUpdateField { it.copy(tankDiameterMm = v) } }
                )
            } else { // Rectangular
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        NumberField(
                            label = "Length (mm)",
                            value = form.tankLengthMm,
                            onValueChange = { v -> onUpdateField { it.copy(tankLengthMm = v) } }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        NumberField(
                            label = "Breadth (mm)",
                            value = form.tankBreadthMm,
                            onValueChange = { v -> onUpdateField { it.copy(tankBreadthMm = v) } }
                        )
                    }
                }
            }
        }

        // Group 2: Sensor Calibration
        SectionCard("Sensor Calibration") {
            NumberField(
                label = "Top Offset (mm)",
                value = form.sensorTopOffsetMm,
                onValueChange = { v -> onUpdateField { it.copy(sensorTopOffsetMm = v) } }
            )
            NumberField(
                label = "Dead Zone (mm)",
                value = form.sensorDeadZoneMm,
                onValueChange = { v -> onUpdateField { it.copy(sensorDeadZoneMm = v) } }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "Min Valid Dist (mm)",
                        value = form.minValidDistanceMm,
                        onValueChange = { v -> onUpdateField { it.copy(minValidDistanceMm = v) } }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "Max Valid Dist (mm)",
                        value = form.maxValidDistanceMm,
                        onValueChange = { v -> onUpdateField { it.copy(maxValidDistanceMm = v) } }
                    )
                }
            }
        }

        // Group 3: Automation Logic
        SectionCard("Automation & Limits") {
            Text("Start Motor at: ${form.startLevelPercent}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = form.startLevelPercent.toFloat(),
                onValueChange = { v -> onUpdateField { it.copy(startLevelPercent = v.toInt()) } },
                valueRange = 0f..100f,
                steps = 100
            )

            Text("Stop Motor at: ${form.stopLevelPercent}%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = form.stopLevelPercent.toFloat(),
                onValueChange = { v -> onUpdateField { it.copy(stopLevelPercent = v.toInt()) } },
                valueRange = 0f..100f,
                steps = 100
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "Min Run (sec)",
                        value = form.minMotorRunSeconds,
                        onValueChange = { v -> onUpdateField { it.copy(minMotorRunSeconds = v) } }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "Min Off (sec)",
                        value = form.minMotorOffSeconds,
                        onValueChange = { v -> onUpdateField { it.copy(minMotorOffSeconds = v) } }
                    )
                }
            }

            NumberField(
                label = "Sensor Timeout (sec)",
                value = form.sensorTimeoutSeconds,
                onValueChange = { v -> onUpdateField { it.copy(sensorTimeoutSeconds = v) } }
            )
        }

        if (uiState.error != null) MessageBanner(uiState.error, isError = true)
        if (uiState.message != null) MessageBanner(uiState.message)

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isDirty && !uiState.saving
        ) {
            Text(if (uiState.saving) "Saving..." else "Save Configuration")
        }
    }
}
