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
import com.wntechs.tankcontroller.data.model.MobileProvisioningRequest
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.data.repository.UserRepository
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairingUiState(
    val isLoading: Boolean = false,
    val status: String? = null,
    val error: String? = null,
    val pairingSuccess: Boolean = false,
    // discoveryMode removed
    val bleDevices: List<BleScanItem> = emptyList(),
    val bleConnectionState: Int = BluetoothProfile.STATE_DISCONNECTED,
    val bleDeviceInfo: BleDeviceInfo? = null,
    val wifiNetworks: List<WifiScanResult> = emptyList(),
    val bleStatus: String = "",
    val bleClaimCode: String = "",
    val isMqttProvisioning: Boolean = false
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
        observeBleStatusAndQueue()
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

    private fun observeBleStatusAndQueue() {
        viewModelScope.launch {
            combine(
                bleManager.status,
                bleManager.isCommandQueueEmpty,
                bleManager.connectionState // Added connectionState to the mix
            ) { status, isQueueEmpty, connState ->
                Triple(status, isQueueEmpty, connState)
            }.collectLatest { (status, isQueueEmpty, connState) ->
                _uiState.update { it.copy(bleStatus = status) }

                val currentState = _uiState.value
                if (currentState.pairingSuccess) return@collectLatest

                // Trigger provisioning when hardware is ready
                if (status == "ok" && !currentState.isMqttProvisioning) {
                    // Only trigger if we aren't already middle-of-cloud-provisioning
                    // We check !currentState.isMqttProvisioning specifically.
                    Log.d("PairingViewModel", "Status is 'ok'. Starting Cloud Finalization...")
                    finalizeBleProvisioning()
                }

                // NAVIGATION TRIGGER LOGIC
                if (currentState.isMqttProvisioning && isQueueEmpty) {
                    // Scenario A: Hardware explicitly says "done"
                    if (status == "done") {
                        Log.d("PairingViewModel", "Provisioning confirmed by device (done)")
                        completeBlePairing()
                    }
                    // Scenario B: Hardware disconnects after pv|c (common during reboot)
                    else if (connState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d("PairingViewModel", "Success condition met (Status: $status, Conn: $connState)")
                        completeBlePairing()
                    }
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


    fun startBleScan() {
        _uiState.update { it.copy(bleDevices = emptyList(), error = null, status = "Scanning for controllers...") }
        try {
            bleManager.startScan(filterByServiceUuid = false)
        } catch (e: SecurityException) {
            _uiState.update { it.copy(error = "Bluetooth scan permission missing", status = null) }
        }
    }

    fun stopBleScan() {
        try {
            bleManager.stopScan()
            _uiState.update { it.copy(status = null) }
        } catch (e: SecurityException) { }
    }

    fun connectBleDevice(device: BleScanItem) {
        stopBleScan()
        val displayName = device.name?.takeIf { it.isNotBlank() } ?: "Device"
        _uiState.update { it.copy(isLoading = true, error = null, status = "Connecting to $displayName...") }
        try {
            bleManager.connect(device)
        } catch (e: SecurityException) {
            _uiState.update { it.copy(error = "Bluetooth connect permission missing", isLoading = false) }
        }
    }

    fun scanWifi() {
        _uiState.update { it.copy(wifiNetworks = emptyList(), error = null, status = "Scanning WiFi networks...") }
        bleManager.scanWifi()
    }

    fun provisionWifi(ssid: String, pass: String) {
        _uiState.update { it.copy(isLoading = true, error = null, status = "Configuring WiFi...") }
        bleManager.setWifi(ssid, pass)
    }

    private fun finalizeBleProvisioning() {
        val info = _uiState.value.bleDeviceInfo ?: return
        // If we are already doing MQTT provisioning, don't restart
        if (_uiState.value.isMqttProvisioning) return

        if (info.device_uuid.isBlank() || info.factory_bootstrap_token.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Device not ready for cloud registration") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, status = "Hardware connected. Registering with cloud...") }
            
            // Call the new 1-step provisioning API
            val request = MobileProvisioningRequest(
                uuid = info.device_uuid,
                deviceSecret = info.factory_bootstrap_token,
                firmwareVersion = info.firmware_version
            )
            
            when (val result = userRepository.provisionDeviceMobile(request)) {
                is AppResult.Success -> {
                    val mqtt = result.data.data.mqtt

                    // ADD THIS DEBUG LOG
                    Log.d("PairingViewModel", """
                            MQTT Provisioning Data:
                            Host: ${mqtt.host}
                            Port: ${mqtt.port}
                            ClientID: ${mqtt.clientId}
                            Username: ${mqtt.username}
                            Password Length: ${mqtt.password?.length ?: "NULL"}
                        """.trimIndent())

                    if (mqtt.password.isNullOrBlank()) {
                        Log.e("PairingViewModel", "ABORTING: MQTT Password is null or empty from backend!")
                        _uiState.update { it.copy(isLoading = false, error = "Cloud error: No MQTT password provided") }
                        return@launch
                    }
                    _uiState.update { it.copy(
                        status = "Cloud registered. Configuring device MQTT...",
                        isMqttProvisioning = true
                    ) }
                    
                    // Push MQTT credentials back to device over BLE
                    bleManager.provisionMqttCredentials(MqttProvisioningPayload(
                        host = mqtt.host,
                        port = mqtt.port,
                        clientId = mqtt.clientId,
                        username = mqtt.username,
                        password = mqtt.password ?: ""
                    ))
                    
                    // We wait for the device to report back status AND queue to empty in observeBleStatusAndQueue
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }
    fun resetPairingState() {
        _uiState.update {
            PairingUiState() // Resets everything to default
        }
        bleManager.disconnect()
    }

    private fun completeBlePairing() {
        val deviceUuid = _uiState.value.bleDeviceInfo?.device_uuid ?: run {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isMqttProvisioning = false,
                    error = "Missing device UUID"
                )
            }
            return
        }
        
        // Idempotency check: if we already signaled success, don't repeat
        if (_uiState.value.pairingSuccess) return

        viewModelScope.launch {
            Log.d("PairingViewModel", "Completing BLE pairing for $deviceUuid")
            deviceRepository.saveDeviceId(deviceUuid)
            bleManager.disconnect()

            _uiState.update { it.copy(
                isLoading = false, 
                pairingSuccess = true, 
                status = "Provisioning complete!",
                isMqttProvisioning = false
            ) }

        }
    }

    fun startPairing(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, status = "Starting pairing...") }
            when (val startResult = userRepository.startPairing(code)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(status = "Pairing started, claiming device...") }
                    val responseData = startResult.data.data
                    val token = responseData?.token ?: run {
                        _uiState.update { it.copy(isLoading = false, error = "No session token returned") }
                        return@launch
                    }
                    val deviceUuid = responseData.device?.uuid
                    
                    when (val claimResult = userRepository.claimDevice(token)) {
                        is AppResult.Success -> {
                            if (!deviceUuid.isNullOrBlank()) {
                                deviceRepository.saveDeviceId(deviceUuid)
                            }
                            _uiState.update { it.copy(status = "Device claimed. Ready!") }
                            _uiState.update { it.copy(isLoading = false, pairingSuccess = true) }
                            deviceRepository.connectWithDynamicCredentials()
                        }
                        is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = claimResult.message) }
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = startResult.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            bleManager.stopScan()
            bleManager.disconnect()
        } catch (e: SecurityException) { }
    }
}
