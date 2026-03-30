package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.model.StatusResponse
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val loading: Boolean = false,
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
                if (settings.baseUrl.isNotBlank()) {
                    _uiState.update { it.copy(baseUrl = settings.baseUrl) }
                    // CONNECT WITH BOTH URL AND DEVICE ID
                    repository.connect(settings.baseUrl, settings.deviceId)
                }
            }
        }

        // Listen for MQTT Status Updates (REPLACES POLLING)
        viewModelScope.launch {
            repository.status.collect { newStatus ->
                _uiState.update { it.copy(status = newStatus, loading = false) }
            }
        }

        // Listen for Errors from ESP
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
    }

    fun refresh() {
        _uiState.update { it.copy(loading = true) }
        repository.requestUpdate()
    }

    fun turnManual(state: Boolean) {
        _uiState.update { it.copy(loading = true) }
        repository.setManual(state)
        // No need to call refresh()!
        // The ESP will publish the new status to 'tank/relay1/status'
        // and our status.collect block above will update the UI automatically.
    }
    fun returnAuto() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            repository.setAuto()

        }
    }
}
