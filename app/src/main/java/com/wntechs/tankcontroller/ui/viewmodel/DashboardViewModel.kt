package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.model.StatusResponse
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.data.repository.MqttConnectionState
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = false,
    val connectionState: MqttConnectionState = MqttConnectionState.Disconnected,
    val baseUrl: String = "",
    val status: StatusResponse = StatusResponse(),
    val message: String? = null,
    val error: String? = null,
)

class DashboardViewModel(private val repository: DeviceRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _uiState.update { it.copy(baseUrl = settings.baseUrl) }
            }
        }

        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                if (state is MqttConnectionState.Error) {
                    _uiState.update { it.copy(error = state.message) }
                }
            }
        }

        // Listen for MQTT Status Updates
        viewModelScope.launch {
            repository.status.collect { newStatus ->
                _uiState.update { it.copy(status = newStatus, loading = false) }
            }
        }

        // Listen for Errors from ESP/MQTT
        viewModelScope.launch {
            repository.errors.collect { errorMsg ->
                _uiState.update { it.copy(error = errorMsg, loading = false) }
            }
        }

        // Listen for Online/Offline status
        viewModelScope.launch {
            repository.isOnline.collect { online ->
                if (!online) _uiState.update { it.copy(error = "Device is Offline") }
            }
        }

        // Attempt to connect using dynamic credentials on start
        connect()
    }

    fun connect() {
        viewModelScope.launch {
            repository.connectWithDynamicCredentials()
        }
    }

    fun refresh() {
        _uiState.update { it.copy(loading = true) }
        repository.requestUpdate()
    }

    fun turnManual(state: Boolean) {
        _uiState.update { it.copy(loading = true) }
        repository.setManual(state)
    }

    fun returnAuto() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            repository.setAuto()
        }
    }
}
