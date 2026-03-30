package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class SettingsUiState(
    val baseUrl: String = "",
    val deviceId: String = "relay1",
    val savedMessage: String? = null,
)

class SettingsViewModel(
    private val repository: DeviceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe the repository settings flow (mapped from DataStore in AppContainer)
            repository.settings.collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        baseUrl = settings.baseUrl,
                        deviceId = settings.deviceId,
                    )
                }
            }
        }
    }

    fun updateBaseUrl(value: String) {
        _uiState.update { it.copy(baseUrl = value, savedMessage = null) }
    }

    fun updateDeviceId(value: String) {
        _uiState.update { it.copy(deviceId = value, savedMessage = null) }
        //_uiState.update { it.copy(deviceId = value, savedMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            // Note: These methods must be implemented in your DeviceRepository
            // to update the underlying SettingsStore/DataStore
            repository.saveBaseUrl(state.baseUrl)
            repository.saveDeviceId(state.deviceId)

            _uiState.update { it.copy(savedMessage = "Settings saved successfully") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }
}
