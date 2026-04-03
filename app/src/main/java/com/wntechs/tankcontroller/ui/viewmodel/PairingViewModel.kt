package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.data.repository.UserRepository
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairingUiState(
    val isLoading: Boolean = false,
    val status: String? = null,
    val error: String? = null,
    val pairingSuccess: Boolean = false
)

class PairingViewModel(
    private val userRepository: UserRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState = _uiState.asStateFlow()

    fun startPairing(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, status = "Starting pairing...") }
            
            // 3. Start Pairing
            when (val startResult = userRepository.startPairing(code)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(status = "Pairing started, claiming device...") }
                    
                    val responseData = startResult.data.data
                    val token = responseData?.token ?: return@launch run {
                        _uiState.update { it.copy(isLoading = false, error = "No session token returned") }
                    }
                    
                    // Extract the device UUID from the initial pairing response
                    val deviceUuid = responseData.device?.uuid
                    
                    when (val claimResult = userRepository.claimDevice(token)) {
                        is AppResult.Success -> {
                            _uiState.update { it.copy(status = "Device claimed, waiting for provisioning...") }
                            val claimToken = claimResult.data.data?.token ?: token
                            // Pass the deviceUuid to the polling function
                            pollProvisioningStatus(deviceUuid ?: "", claimToken)
                        }
                        is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = claimResult.message) }
                    }
                }
                is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = startResult.message) }
            }
        }
    }

    private fun pollProvisioningStatus(deviceUuid: String, token: String) {
        viewModelScope.launch {
            var isReady = false
            var attempts = 0
            while (!isReady && attempts < 30) {
                attempts++
                // Now passing the actual device UUID to the status check
                when (val statusResult = userRepository.getProvisioningStatus(deviceUuid, token)) {
                    is AppResult.Success -> {
                        val statusData = statusResult.data.data
                        if (statusData != null) {
                            val status = statusData.status
                            _uiState.update { it.copy(status = "Provisioning status: $status") }
                            
                            if (status == "ready_to_finalize") {
                                isReady = true
                                // If statusResult has a more up-to-date UUID, use it, otherwise use the one we have
                                finalizePairing(statusData.uuid ?: deviceUuid)
                            } else if (status == "expired") {
                                _uiState.update { it.copy(isLoading = false, error = "Pairing session expired") }
                                return@launch
                            }
                        }
                    }
                    is AppResult.Error -> {
                        // Ignore individual poll errors
                    }
                }
                delay(2000)
            }
            if (!isReady) {
                _uiState.update { it.copy(isLoading = false, error = "Provisioning timed out") }
            }
        }
    }

    private fun finalizePairing(deviceUuid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(status = "Finalizing pairing...") }
            deviceRepository.saveDeviceId(deviceUuid)
            
            when (val mqttResult = deviceRepository.connectWithDynamicCredentials()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, status = "Pairing complete!", pairingSuccess = true) }
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to get MQTT credentials: ${mqttResult.message}") }
                }
            }
        }
    }
}
