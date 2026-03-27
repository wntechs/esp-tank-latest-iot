package com.wntechs.tankcontroller.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wntechs.tankcontroller.data.model.DiscoveredDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDiscoveryScreen(
    discoveredDevices: List<DiscoveredDevice>,
    manualHost: String,
    isScanning: Boolean,
    selectedIndex: Int,
    onManualHostChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onDeviceSelected: (Int) -> Unit,
    onConnectClick: () -> Unit,
    onUseManualAddress: () -> Unit,
    message: String?,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Find Relay Device") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("mDNS / DNS-SD discovery", fontWeight = FontWeight.SemiBold)
                        Text("Scan for relay board on local network. Default discovery uses _http._tcp. service type.")
                        Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) {
                            Text(if (isScanning) "Scanning..." else "Scan for Device")
                        }
                    }
                }
            }

            if (discoveredDevices.isNotEmpty()) {
                item {
                    Text("Detected devices", fontWeight = FontWeight.SemiBold)
                }
                itemsIndexed(discoveredDevices) { index, device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDeviceSelected(index) },
                        colors = CardDefaults.cardColors(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(selected = index == selectedIndex, onClick = { onDeviceSelected(index) })
                            Column {
                                Text(device.serviceName, fontWeight = FontWeight.SemiBold)
                                Text("Host: ${device.hostName}")
                                Text("IP: ${device.ipAddress}:${device.port}")
                            }
                        }
                    }
                }
                item {
                    Button(onClick = onConnectClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Use Selected Device")
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Manual fallback", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = manualHost,
                            onValueChange = onManualHostChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Host or IP") },
                            placeholder = { Text("tank-relay.local or 192.168.1.34") },
                        )
                        OutlinedButton(onClick = onUseManualAddress, modifier = Modifier.fillMaxWidth()) {
                            Text("Use Manual Address")
                        }
                    }
                }
            }

            message?.let {
                item { MessageBanner(it, isError = true) }
            }
        }
    }
}
