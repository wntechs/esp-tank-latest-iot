package com.wntechs.tankcontroller.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.saveable.rememberSaveable
import com.wntechs.tankcontroller.ui.theme.TankControllerTheme
import com.wntechs.tankcontroller.ui.viewmodel.DiscoveryMode
import com.wntechs.tankcontroller.ui.viewmodel.PairingUiState
import androidx.core.app.ActivityCompat
import com.wntechs.tankcontroller.data.ble.BleDeviceInfo
import com.wntechs.tankcontroller.data.ble.BleScanItem

@Composable
fun DeviceDiscoveryScreen(
    uiState: PairingUiState,
    onStartPairing: (String) -> Unit,
    onDiscoveryModeChanged: (DiscoveryMode) -> Unit,
    onScanBleDevices: () -> Unit,
    onConnectBleDevice: (BleScanItem) -> Unit,
    onScanWifi: () -> Unit,
    onProvisionWifi: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TabRow(selectedTabIndex = uiState.discoveryMode.ordinal) {
            Tab(
                selected = uiState.discoveryMode == DiscoveryMode.PAIRING_CODE,
                onClick = { onDiscoveryModeChanged(DiscoveryMode.PAIRING_CODE) },
                text = { Text("Pairing Code") }
            )
            Tab(
                selected = uiState.discoveryMode == DiscoveryMode.BLE,
                onClick = { onDiscoveryModeChanged(DiscoveryMode.BLE) },
                text = { Text("Bluetooth (BLE)") }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (uiState.discoveryMode) {
            DiscoveryMode.PAIRING_CODE -> {
                PairingCodeContent(
                    uiState = uiState,
                    onStartPairing = onStartPairing
                )
            }
            DiscoveryMode.BLE -> {
                BleProvisioningContent(
                    uiState = uiState,
                    onScanDevices = onScanBleDevices,
                    onConnect = onConnectBleDevice,
                    onScanWifi = onScanWifi,
                    onProvision = onProvisionWifi
                )
            }
        }
    }
}

@Composable
private fun PairingCodeContent(
    uiState: PairingUiState,
    onStartPairing: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Enter Device Code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Enter the 6-digit code shown on your tank controller device screen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Pairing Code") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text("e.g. 123456") }
        )

        if (uiState.error != null) {
            MessageBanner(text = uiState.error, isError = true)
        }

        if (uiState.status != null) {
            Text(
                text = uiState.status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Button(
            onClick = { onStartPairing(code) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && code.length == 6
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("Connect Device")
            }
        }
    }
}


@Composable
private fun BleProvisioningContent(
    uiState: PairingUiState,
    onScanDevices: () -> Unit,
    onConnect: (BleScanItem) -> Unit,
    onScanWifi: () -> Unit,
    onProvision: (String, String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    fun hasAllPermissions(): Boolean =
        requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    fun isPermanentlyDenied(permission: String): Boolean =
        permissionRequested &&
                activity != null &&
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    fun hasPermanentDenial(): Boolean =
        requiredPermissions.any(::isPermanentlyDenied)

    var hasPermissions by remember { mutableStateOf(hasAllPermissions()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRequested = true
        hasPermissions = hasAllPermissions()
    }

    fun requestPermissions() {
        permissionRequested = true
        permissionLauncher.launch(requiredPermissions)
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    LaunchedEffect(Unit) {
        hasPermissions = hasAllPermissions()
    }

    LaunchedEffect(uiState.discoveryMode) {
        hasPermissions = hasAllPermissions()

        if (!hasPermissions && !hasPermanentDenial()) {
            requestPermissions()
        }
    }

    var showWifiDialog by remember { mutableStateOf(false) }
    var selectedSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }

    if (!hasPermissions) {
        val permanentlyDenied = hasPermanentDenial()

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothDisabled,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (permanentlyDenied) {
                    "Bluetooth permission was denied. Please enable it from app settings."
                } else {
                    "Bluetooth permission is required for direct setup."
                }
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (permanentlyDenied) openAppSettings() else requestPermissions()
                }
            ) {
                Text(if (permanentlyDenied) "Open Settings" else "Grant Permission")
            }
        }

        return
    }

    if (showWifiDialog) {
        AlertDialog(
            onDismissRequest = { showWifiDialog = false },
            title = { Text("Connect to WiFi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = selectedSsid,
                        onValueChange = { selectedSsid = it },
                        label = { Text("Network SSID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onProvision(selectedSsid, wifiPassword)
                        showWifiDialog = false
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWifiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when {
            uiState.bleConnectionState == BluetoothProfile.STATE_DISCONNECTED -> {
                Button(
                    onClick = onScanDevices,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BluetoothSearching, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan for Controllers")
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(
                        items = uiState.bleDevices,
                        key = { it.address }
                    ) { device ->
                        val displayName = device.name?.takeIf { it.isNotBlank() } ?: "Unnamed Device"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onConnect(device) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bluetooth,
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = device.address,
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    Text(
                                        text = "Signal: ${device.rssi} dBm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (device.serviceUuids.isNotEmpty()) {
                                        Text(
                                            text = "Services: ${
                                                device.serviceUuids.joinToString(
                                                    limit = 2,
                                                    truncated = " +more"
                                                ) { it.toString() }
                                            }",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            uiState.bleConnectionState == BluetoothProfile.STATE_CONNECTING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = "Connecting to Controller...",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }

            uiState.bleConnectionState == BluetoothProfile.STATE_CONNECTED -> {
                SectionCard("Controller Info") {
                    uiState.bleDeviceInfo?.let { info ->
                        BleInfoRow("ID", info.device_id)
                        BleInfoRow(
                            "WiFi",
                            if (info.wifi_connected) "Connected (${info.ssid})" else "Not Connected"
                        )
                        BleInfoRow("State", info.state)
                    }

                    if (uiState.bleStatus.isNotEmpty()) {
                        Text(
                            text = "Status: ${uiState.bleStatus}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                SectionCard("Provision WiFi") {
                    Button(
                        onClick = onScanWifi,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan WiFi Networks")
                    }

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(uiState.wifiNetworks) { network ->
                            ListItem(
                                headlineContent = { Text(network.ssid) },
                                supportingContent = {
                                    Text(
                                        "Signal: ${network.rssi} dBm • " +
                                                if (network.secure) "Secured" else "Open"
                                    )
                                },
                                modifier = Modifier.clickable {
                                    selectedSsid = network.ssid
                                    showWifiDialog = true
                                }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showWifiDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter WiFi Manually")
                    }
                }
            }
        }

        if (uiState.error != null) {
            MessageBanner(text = uiState.error, isError = true)
        }
    }
}

@Composable
private fun BleInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceDiscoveryCodePreview() {
    TankControllerTheme {
        DeviceDiscoveryScreen(
            uiState = PairingUiState(
                discoveryMode = DiscoveryMode.PAIRING_CODE,
                status = "Starting pairing..."
            ),
            onStartPairing = {},
            onDiscoveryModeChanged = {},
            onScanBleDevices = {},
            onConnectBleDevice = {},
            onScanWifi = {},
            onProvisionWifi = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceDiscoveryBlePreview() {
    TankControllerTheme {
        DeviceDiscoveryScreen(
            uiState = PairingUiState(
                discoveryMode = DiscoveryMode.BLE,
                bleConnectionState = BluetoothProfile.STATE_CONNECTED,
                bleDeviceInfo = BleDeviceInfo(
                    device_id = "ESP32-WNT-01",
                    wifi_connected = false,
                    state = "ready"
                ),
                bleStatus = "Scanning WiFi..."
            ),
            onStartPairing = {},
            onDiscoveryModeChanged = {},
            onScanBleDevices = {},
            onConnectBleDevice = {},
            onScanWifi = {},
            onProvisionWifi = { _, _ -> }
        )
    }
}
