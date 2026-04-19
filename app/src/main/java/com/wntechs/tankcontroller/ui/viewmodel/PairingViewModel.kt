package com.wntechs.tankcontroller.ui.viewmodel

import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.ble.BleDeviceInfo
import com.wntechs.tankcontroller.data.ble.BleManager
import com.wntechs.tankcontroller.data.ble.BleScanItem
import com.wntechs.tankcontroller.data.ble.MqttProvisioningPayload
import com.wntechs.tankcontroller.data.ble.WifiScanResult
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.data.repository.UserRepository
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairingUiState(
    val isLoading: Boolean = false,
    val status: String? = null,
    val error: String? = null,
    val pairingSuccess: Boolean = false,
    val discoveryMode: DiscoveryMode = DiscoveryMode.PAIRING_CODE,
    val bleDevices: List<BleScanItem> = emptyList(),
    val bleConnectionState: Int = BluetoothProfile.STATE_DISCONNECTED,
    val bleDeviceInfo: BleDeviceInfo? = null,
    val wifiNetworks: List<WifiScanResult> = emptyList(),
    val bleStatus: String = "",
    val bleClaimCode: String = ""
)



enum class DiscoveryMode {
    PAIRING_CODE, BLE
}

class PairingViewModel(
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository,
    private val bleManager: BleManager,

) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()
    private var lastAutoClaimCode: String = ""

    init {
        observeBleScanResults()
        observeConnectionState()
        observeDeviceInfo()
        observeBleStatus()
        observeBleClaimCode()
        observeWifiNetworks()
    }

    private fun observeBleScanResults() {
        viewModelScope.launch {
            bleManager.scanResults.collectLatest { item ->
                _uiState.update { state ->
                    val list = state.bleDevices.toMutableList()
                    val existingIndex = list.indexOfFirst { it.address == item.address }

                    if (existingIndex >= 0) {
                        list[existingIndex] = item
                    } else {
                        list.add(item)
                    }

                    state.copy(bleDevices = list)
                }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bleManager.connectionState.collectLatest { connectionState ->
                _uiState.update { state ->
                    state.copy(
                        bleConnectionState = connectionState,
                        isLoading = connectionState == BluetoothProfile.STATE_CONNECTING
                    )
                }

                if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                    lastAutoClaimCode = ""
                    _uiState.update { it.copy(bleClaimCode = "") }
                }
            }
        }
    }

    private fun observeDeviceInfo() {
        viewModelScope.launch {
            bleManager.deviceInfo.collectLatest { info ->
                _uiState.update { it.copy(bleDeviceInfo = info) }
            }
        }
    }

    private fun observeBleStatus() {
        viewModelScope.launch {
            bleManager.status.collectLatest { status ->
                _uiState.update { it.copy(bleStatus = status) }

                when {
                    status == "wifi_connected" -> {
                        _uiState.update {
                            it.copy(status = "WiFi saved, waiting for provisioning...")
                        }
                        // kick the next stage here if that is the intended flow
                    }

                }

            }
        }
    }

    private fun observeBleClaimCode() {
        viewModelScope.launch {
            bleManager.claimCode.collectLatest { code ->
                val claimCode = code.trim()
                _uiState.update { it.copy(bleClaimCode = claimCode) }

                if (claimCode.isNotBlank()) {
                    autoStartClaimPairing(claimCode)
                }
            }
        }
    }

    private fun observeWifiNetworks() {
        viewModelScope.launch {
            bleManager.wifiNetworks.collectLatest { network ->
                _uiState.update { state ->
                    val updatedNetworks = state.wifiNetworks
                        .filterNot { it.ssid == network.ssid } + network

                    state.copy(
                        wifiNetworks = updatedNetworks.sortedByDescending { it.rssi }
                    )
                }
            }
        }
    }

    fun setDiscoveryMode(mode: DiscoveryMode) {
        _uiState.update {
            it.copy(
                discoveryMode = mode,
                error = null,
                status = null
            )
        }

        if (mode != DiscoveryMode.BLE) {
            stopBleScan()
        }
    }

    fun startBleScan() {
        _uiState.update {
            it.copy(
                bleDevices = emptyList(),
                error = null,
                status = "Scanning for controllers..."
            )
        }

        try {
            bleManager.startScan(filterByServiceUuid = false)
        } catch (e: SecurityException) {
            _uiState.update {
                it.copy(
                    error = "Bluetooth scan permission missing",
                    status = null
                )
            }
        }
    }

    fun stopBleScan() {
        try {
            bleManager.stopScan()
            _uiState.update { it.copy(status = null) }
        } catch (e: SecurityException) {
            // Ignore
        }
    }

    fun connectBleDevice(device: BleScanItem) {
        stopBleScan()

        val displayName = device.name?.takeIf { it.isNotBlank() } ?: "Device"

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                status = "Connecting to $displayName..."
            )
        }

        try {
            bleManager.connect(device)
        } catch (e: SecurityException) {
            _uiState.update {
                it.copy(
                    error = "Bluetooth connect permission missing",
                    isLoading = false
                )
            }
        }
    }

    fun scanWifi() {
        _uiState.update {
            it.copy(
                wifiNetworks = emptyList(),
                error = null,
                status = "Scanning WiFi networks..."
            )
        }
        bleManager.scanWifi()
    }

    fun provisionWifi(ssid: String, pass: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                status = "Configuring WiFi..."
            )
        }
        bleManager.setWifi(ssid, pass)
    }



    fun startPairing(code: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    status = "Starting pairing..."
                )
            }

            when (val startResult = userRepository.startPairing(code)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(status = "Pairing started, claiming device...")
                    }

                    val responseData = startResult.data.data
                    val token = responseData?.token ?: run {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No session token returned"
                            )
                        }
                        return@launch
                    }

                    val deviceUuid = responseData.device?.uuid
                    if (!deviceUuid.isNullOrBlank()) {
                        deviceRepository.saveDeviceId(deviceUuid)
                    }

                    when (val claimResult = userRepository.claimDevice(token)) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(status = "Device claimed. Fetching provisioning data...")
                            }

                            when (val provisioningResult = deviceRepository.fetchMqttProvisioningCredentials()) {
                                is AppResult.Success -> {
                                    val creds = provisioningResult.data

                                    sendProvisioningToDevice(
                                        MqttProvisioningPayload(
                                            host = creds.host,
                                            port = creds.port,
                                            clientId = creds.clientId,
                                            username = creds.username,
                                            password = creds.password
                                        )
                                    )

                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            pairingSuccess = true,
                                            status = "MQTT provisioning sent to device"
                                        )
                                    }
                                }

                                is AppResult.Error -> {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            error = provisioningResult.message
                                        )
                                    }
                                }
                            }
                        }

                        is AppResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = claimResult.message
                                )
                            }
                        }
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = startResult.message
                        )
                    }
                }
            }
        }
    }




    private fun autoStartClaimPairing(code: String) {
        if (code.isBlank()) return
        if (code == lastAutoClaimCode) return

        lastAutoClaimCode = code
        startPairing(code)
    }

    fun readBleStatus() {
        bleManager.readStatus()
    }

    fun requestClaimCode() {
        bleManager.requestClaimCode()
    }

    private fun sendProvisioningToDevice(payload: MqttProvisioningPayload) {
        bleManager.acknowledgeClaimCode()
        bleManager.provisionMqttCredentials(payload)

        _uiState.update {
            it.copy(
                status = "Provisioning sent to device. Waiting for reboot..."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()

        try {
            bleManager.stopScan()
            bleManager.disconnect()
        } catch (e: SecurityException) {
            // Ignore
        }
    }
}