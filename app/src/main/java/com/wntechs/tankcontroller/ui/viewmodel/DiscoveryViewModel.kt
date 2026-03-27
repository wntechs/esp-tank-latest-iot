package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.model.DiscoveredDevice
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val discoveredDevices: List<DiscoveredDevice> = emptyList(),
    val isScanning: Boolean = false,
    val selectedIndex: Int = 0,
    val manualHost: String = "",
    val message: String? = null,
)

class DiscoveryViewModel(
    private val repository: DeviceRepository,
) : ViewModel() {
    private val local = MutableStateFlow(DiscoveryUiState())

    val uiState: StateFlow<DiscoveryUiState> = combine(
        local,
        repository.discoveredDevices,
        repository.isScanning,
    ) { state, devices, scanning ->
        state.copy(discoveredDevices = devices, isScanning = scanning)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiscoveryUiState())

    fun updateManualHost(value: String) = local.update { it.copy(manualHost = value) }
    fun selectIndex(index: Int) = local.update { it.copy(selectedIndex = index) }
    fun startScan() = repository.startDiscovery()

    fun connectSelected() {
        val state = uiState.value
        val selected = state.discoveredDevices.getOrNull(state.selectedIndex) ?: return
        viewModelScope.launch {
            repository.selectDiscoveredDevice(selected, preferMdns = true)
        }
    }

    fun connectManual() {
        val value = uiState.value.manualHost.trim()
        if (value.isBlank()) {
            local.update { it.copy(message = "Enter hostname or IP first") }
            return
        }
        val normalized = when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            else -> "http://$value/"
        }
        viewModelScope.launch {
            repository.saveBaseUrl(normalized)
        }
    }

    override fun onCleared() {
        repository.stopDiscovery()
        super.onCleared()
    }
}
