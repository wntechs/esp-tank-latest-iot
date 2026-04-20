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
import androidx.compose.material3.RangeSlider
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
import kotlin.math.roundToInt

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
            Text(
                text = "Motor Levels: ${form.startLevelPercent}% (Start) - ${form.stopLevelPercent}% (Stop)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            RangeSlider(
                value = form.startLevelPercent.toFloat()..form.stopLevelPercent.toFloat(),
                onValueChange = { range ->
                    // Use roundToInt() to prevent the non-moving thumb from jittering
                    val newStart = kotlin.math.round(range.start).toInt()
                    val newStop = kotlin.math.round(range.endInclusive).toInt()

                    onUpdateField { it.copy(
                        startLevelPercent = newStart,
                        stopLevelPercent = newStop
                    ) }
                },
                valueRange = 0f..100f,
                // Removing 'steps' makes the movement smooth and independent.
                // The roundToInt() logic above handles the discretization to 0-100 integers.
                modifier = Modifier.fillMaxWidth()
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "Max Motor Runtime (sec)",
                        value = form.maxMotorRunSeconds,
                        onValueChange = { v -> onUpdateField { it.copy(maxMotorRunSeconds = v) } }
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    NumberField(
                        label = "Sensor Timeout (sec)",
                        value = form.sensorTimeoutSeconds,
                        onValueChange = { v -> onUpdateField { it.copy(sensorTimeoutSeconds = v) } }
                    )
                }
            }


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
