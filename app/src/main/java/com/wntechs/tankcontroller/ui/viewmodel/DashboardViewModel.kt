package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.model.StatusResponse
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

class DashboardViewModel(
    private val repository: DeviceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collect { settings ->
                _uiState.update { it.copy(baseUrl = settings.baseUrl) }
            }
        }
        
        // Initial refresh
        refresh()

        // Automatic polling every 8 seconds
        viewModelScope.launch {
            while (true) {
                delay(8000)
                if (_uiState.value.baseUrl.isNotBlank() && !_uiState.value.loading) {
                    refresh(showLoading = false)
                }
            }
        }
    }

    fun refresh(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(loading = true, error = null, message = null) }
            }
            when (val result = repository.getStatus()) {
                is AppResult.Success -> _uiState.update { it.copy(loading = false, status = result.data) }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun turnManual(state: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            when (val result = repository.setManual(state)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(loading = false, message = if (state) "Motor turned ON" else "Motor turned OFF") }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun returnAuto() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, message = null) }
            when (val result = repository.setAuto()) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(loading = false, message = "Returned to auto mode") }
                    refresh()
                }
                is AppResult.Error -> _uiState.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
