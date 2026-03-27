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
    val preferMdns: Boolean = true,
    val savedMessage: String? = null,
)

class SettingsViewModel(
    private val repository: DeviceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settings.collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        baseUrl = settings.baseUrl,
                        preferMdns = settings.preferMdns,
                    )
                }
            }
        }
    }

    fun updateBaseUrl(value: String) {
        _uiState.update { it.copy(baseUrl = value) }
    }

    fun setPreferMdns(value: Boolean) {
        _uiState.update { it.copy(preferMdns = value) }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.setPreferMdns(state.preferMdns)
            repository.saveBaseUrl(state.baseUrl)
            _uiState.update { it.copy(savedMessage = "Settings saved") }
        }
    }
}
