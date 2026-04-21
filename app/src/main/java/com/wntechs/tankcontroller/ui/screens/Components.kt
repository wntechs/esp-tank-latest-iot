package com.wntechs.tankcontroller.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.R
import com.wntechs.tankcontroller.data.model.OwnedDevice
import com.wntechs.tankcontroller.data.repository.MqttConnectionState
import com.wntechs.tankcontroller.ui.viewmodel.DashboardUiState

@Composable
fun MessageBanner(text: String, isError: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (isError) Icons.Default.Warning else Icons.Default.CheckCircle, null)
            Text(text)
        }
    }
}

@Composable
fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { ch -> ch.isDigit() }) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
fun CylindricalTank(
    modifier: Modifier = Modifier,
    progress: Int, // 0 to 100
    isSensorConnected: Boolean = true, // New parameter for sensor status

) {
    // Smoothly animate the water level change
    val animatedProgress by animateFloatAsState(
        targetValue = progress.toFloat() / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "WaterLevel"
    )

    // Animation for radio waves
    val infiniteTransition = rememberInfiniteTransition(label = "RadioWaves")
    val waveRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveScale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveAlpha"
    )

    val tankColor = MaterialTheme.colorScheme.surfaceVariant
    val waterGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2))
    )
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val sensorColor = if (isSensorConnected) Color(0xFF4CAF50) else Color(0xFFF44336)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val capHeight = h * 0.1f

            // 1. Draw the Tank Background
            val tankPath = Path().apply {
                moveTo(0f, capHeight)
                lineTo(0f, h - capHeight)
                cubicTo(0f, h, w, h, w, h - capHeight)
                lineTo(w, capHeight)
                cubicTo(w, 0f, 0f, 0f, 0f, capHeight)
                close()
            }
            drawPath(tankPath, color = tankColor)
            drawPath(
                path = tankPath,
                color = borderColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // 2. Draw the Water (Clipped)
            val waterLevelY = h - (animatedProgress * (h - capHeight)) - capHeight
            clipPath(tankPath) {
                drawRect(
                    brush = waterGradient,
                    topLeft = Offset(0f, waterLevelY),
                    size = Size(w, h - waterLevelY)
                )
                drawOval(
                    color = Color(0xFF64B5F6).copy(alpha = 0.8f),
                    topLeft = Offset(0f, waterLevelY - (capHeight / 2)),
                    size = Size(w, capHeight)
                )
            }

            // 3. Draw Top Rim
            drawOval(
                color = borderColor,
                topLeft = Offset(0f, 0f),
                size = Size(w, capHeight),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )

            // 4. Draw Sensor on top of the lid
            val sensorRadius = 6.dp.toPx()
            val sensorCenter = Offset(w * 0.5f, capHeight * 0.5f)

            // Animated Radio Waves (only if connected)
            if (isSensorConnected) {
                drawCircle(
                    color = sensorColor.copy(alpha = waveAlpha),
                    radius = sensorRadius + (waveRadiusScale * 20.dp.toPx()),
                    center = sensorCenter,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }

            // The Sensor "Bulb"
            drawCircle(
                color = sensorColor,
                radius = sensorRadius,
                center = sensorCenter
            )
        }

        // 5. Centered Percentage Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (progress > 40) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun WaterLevelCard(
    uiState: DashboardUiState,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onToggleAuto: () -> Unit
) {
    // 1. Setup the Blinking Animation (Alpha)
    val infiniteTransition = rememberInfiniteTransition(label = "PumpBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f, // Dims to 30% opacity
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse // Creates the "pulse/blink" effect
        ),
        label = "BlinkAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .height(210.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Tank Visualization
            CylindricalTank(
                progress = uiState.status.waterLevelPercent,
                isSensorConnected = uiState.status.sensorConnected,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
            )

            // Pump & Mode Control Box
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Status & Mode Display Panel
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Section: Animated Water Pump Icon & Status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.water_pump),
                                contentDescription = "Pump Status",
                                modifier = Modifier
                                    .size(34.dp),
                                // Apply the animated alpha only if motor is On
                                tint = if (uiState.status.motorOn)
                                    Color(0xFF43A047).copy(alpha = blinkAlpha)
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PUMP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = if (uiState.status.motorOn) "RUNNING" else "IDLE",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.status.motorOn) Color(0xFF43A047) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Subtle Divider
                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.width(120.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        // Bottom Section: Auto/Manual Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (!uiState.status.manualOverride) "AUTO" else "MANUAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (!uiState.status.manualOverride) Color(0xFF2196F3) else Color(
                                    0xFFF57C00
                                )
                            )
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = !uiState.status.manualOverride,
                                onCheckedChange = { onToggleAuto() },
                                enabled = uiState.connectionState is MqttConnectionState.Connected,
                                thumbContent = if (!uiState.status.manualOverride) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.DeviceHub,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }


                // Circular Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTurnOn,
                        // Enabled only in manual mode when pump is off
                        enabled = uiState.status.manualOverride && !uiState.status.motorOn && uiState.connectionState is MqttConnectionState.Connected,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                    ) {
                        Text("ON", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onTurnOff,
                        // Enabled only in manual mode when pump is on
                        enabled = uiState.status.manualOverride && uiState.status.motorOn && uiState.connectionState is MqttConnectionState.Connected,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(56.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("OFF", fontWeight = FontWeight.Bold)
                    }
                }
                Text(text = "Manual Controls", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Divider()
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelector(
    devices: List<OwnedDevice>,
    selectedUuid: String?,
    onDeviceSelected: (String) -> Unit,
    onAddNewDevice: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDevice = devices.find { it.uuid == selectedUuid }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedDevice?.serialNumber ?: "Select Device",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.DeviceHub, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            devices.forEach { device ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(device.serialNumber, fontWeight = FontWeight.Bold)
                            device.model?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    onClick = {
                        onDeviceSelected(device.uuid)
                        expanded = false
                    }
                )
            }
            Divider()
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Add New Device")
                    }
                },
                onClick = {
                    onAddNewDevice()
                    expanded = false
                }
            )
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


@Composable
fun ConnectionStatusCard(state: MqttConnectionState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is MqttConnectionState.Connected -> Color(0xFFE8F5E9)
                is MqttConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                MqttConnectionState.Connecting -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Connecting to MQTT...", style = MaterialTheme.typography.bodyMedium)
                }

                MqttConnectionState.Connected -> {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32)
                    )
                    Text(
                        "Connected to Cloud",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32)
                    )
                }

                is MqttConnectionState.Error -> {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Connection Error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                MqttConnectionState.Disconnected -> {
                    Icon(Icons.Default.CloudOff, contentDescription = null)
                    Text("Disconnected", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}