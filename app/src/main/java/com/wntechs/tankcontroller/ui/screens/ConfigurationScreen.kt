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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.ui.viewmodel.ConfigurationUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(
    uiState: ConfigurationUiState,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
) {
    val form = uiState.form
    Scaffold(topBar = { TopAppBar(title = { Text("Tank Configuration") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard("Tank shape") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = form.tankShape == 0, onClick = {}, label = { Text("Cylindrical") })
                    FilterChip(selected = form.tankShape == 1, onClick = {}, label = { Text("Rectangular") })
                }
                Text("Shape editing is wired in ViewModel form update helper. Add per-field callbacks as you continue building.")
            }

            SectionCard("Current values from device") {
                Text("tank_height_mm = ${form.tankHeightMm}")
                Text("tank_diameter_mm = ${form.tankDiameterMm}")
                Text("tank_length_mm = ${form.tankLengthMm}")
                Text("tank_breadth_mm = ${form.tankBreadthMm}")
                Text("sensor_top_offset_mm = ${form.sensorTopOffsetMm}")
                Text("sensor_dead_zone_mm = ${form.sensorDeadZoneMm}")
                Text("min_valid_distance_mm = ${form.minValidDistanceMm}")
                Text("max_valid_distance_mm = ${form.maxValidDistanceMm}")
                Text("start_level_percent = ${form.startLevelPercent}")
                Text("stop_level_percent = ${form.stopLevelPercent}")
                Text("min_motor_run_seconds = ${form.minMotorRunSeconds}")
                Text("min_motor_off_seconds = ${form.minMotorOffSeconds}")
                Text("sensor_timeout_seconds = ${form.sensorTimeoutSeconds}")
            }

            if (uiState.error != null) MessageBanner(uiState.error, isError = true)
            if (uiState.message != null) MessageBanner(uiState.message)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text(if (uiState.loading) "Loading..." else "Reload") }
                Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text(if (uiState.saving) "Saving..." else "Save") }
            }
        }
    }
}
