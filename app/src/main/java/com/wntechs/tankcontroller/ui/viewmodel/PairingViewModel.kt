package com.wntechs.tankcontroller.ui.viewmodel

import android.bluetooth.BluetoothProfile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.ble.BleDeviceInfo
import com.wntechs.tankcontroller.data.ble.BleManager
import com.wntechs.tankcontroller.data.ble.BleScanItem
import com.wntechs.tankcontroller.data.ble.WifiScanResult
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.data.repository.UserRepository
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val bleStatus: String = ""
)

enum class DiscoveryMode {
    PAIRING_CODE, BLE
}

class PairingViewModel(
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository,
    private val bleManager: BleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private var provisioningPollJob: Job? = null

    init {
        observeBleScanResults()
        observeConnectionState()
        observeDeviceInfo()
        observeBleStatus()
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

                if (status == "ok") {
                    finalizeBlePairing()
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

    private fun finalizeBlePairing() {
        val deviceId = _uiState.value.bleDeviceInfo?.device_id ?: return
        finalizePairing(deviceId)
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
                    val token = responseData?.token ?: return@launch run {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No session token returned"
                            )
                        }
                    }

                    val deviceUuid = responseData.device?.uuid

                    when (val claimResult = userRepository.claimDevice(token)) {
                        is AppResult.Success -> {
                            _uiState.update {
                                it.copy(status = "Device claimed, waiting for provisioning...")
                            }

                            val claimToken = claimResult.data.data?.token ?: token
                            pollProvisioningStatus(deviceUuid.orEmpty(), claimToken)
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

    private fun pollProvisioningStatus(deviceUuid: String, token: String) {
        provisioningPollJob?.cancel()

        provisioningPollJob = viewModelScope.launch {
            var isReady = false
            var attempts = 0

            while (!isReady && attempts < 30) {
                attempts++

                when (val statusResult = userRepository.getProvisioningStatus(deviceUuid, token)) {
                    is AppResult.Success -> {
                        val statusData = statusResult.data.data
                        if (statusData != null) {
                            val status = statusData.status

                            _uiState.update {
                                it.copy(status = "Provisioning status: $status")
                            }

                            when (status) {
                                "ready_to_finalize" -> {
                                    isReady = true
                                    finalizePairing(statusData.uuid ?: deviceUuid)
                                }

                                "expired" -> {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            error = "Pairing session expired"
                                        )
                                    }
                                    return@launch
                                }
                            }
                        }
                    }

                    is AppResult.Error -> {
                        // keep polling quietly
                    }
                }

                delay(2000)
            }

            if (!isReady) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Provisioning timed out"
                    )
                }
            }
        }
    }

    private fun finalizePairing(deviceUuid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = "Finalizing pairing...") }

            deviceRepository.saveDeviceId(deviceUuid)

            when (val mqttResult = deviceRepository.connectWithDynamicCredentials()) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = "Pairing complete!",
                            pairingSuccess = true
                        )
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to get MQTT credentials: ${mqttResult.message}"
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        provisioningPollJob?.cancel()

        try {
            bleManager.stopScan()
            bleManager.disconnect()
        } catch (e: SecurityException) {
            // Ignore
        }
    }
}