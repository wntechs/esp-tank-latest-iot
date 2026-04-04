package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.model.TankFamily
import com.wntechs.tankcontroller.data.model.TankMeasurements
import com.wntechs.tankcontroller.data.model.TankModel
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.data.repository.UserRepository
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class SettingsUiState(
    val loading: Boolean = false,
    val savedMessage: String? = null,
    val error: String? = null,
    val tankMeasurements: TankMeasurements? = null,
    val selectedFamily: TankFamily? = null,
    val selectedModel: TankModel? = null,
    val currentDeviceId: String = ""
)

class SettingsViewModel(
    private val repository: DeviceRepository,
    private val userRepository: UserRepository,
    private val jsonString: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        val measurements = try {
            json.decodeFromString<TankMeasurements>(jsonString)
        } catch (e: Exception) {
            null
        }
        _uiState.update { it.copy(tankMeasurements = measurements) }

        viewModelScope.launch {
            repository.settings.collect { settings ->
                _uiState.update { it.copy(currentDeviceId = settings.deviceId) }
            }
        }
    }

    fun selectFamily(family: TankFamily?) {
        _uiState.update { it.copy(selectedFamily = family, selectedModel = null) }
    }

    fun selectModel(model: TankModel?) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun applySelectedPreset() {
        val state = _uiState.value
        val model = state.selectedModel ?: return
        val shape = state.selectedFamily?.shape ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, savedMessage = null, error = null) }
            val tankShape = if (shape == "rectangular") 1 else 0
            repository.updateConfig(
                com.wntechs.tankcontroller.data.model.ConfigUpdateRequest(
                    tankShape = tankShape,
                    tankHeightMm = model.dimensions.height_mm,
                    tankDiameterMm = model.dimensions.diameter_mm,
                    tankLengthMm = model.dimensions.length_mm,
                    tankBreadthMm = model.dimensions.breadth_mm
                )
            )
            _uiState.update { it.copy(loading = false, savedMessage = "Tank preset '${model.code}' applied to device") }
        }
    }

    fun resetDevice() {
        val deviceUuid = _uiState.value.currentDeviceId
        if (deviceUuid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, savedMessage = null) }
            when (val result = userRepository.resetDevice(deviceUuid)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(loading = false, savedMessage = "Device reset request accepted. You can now re-provision the device.") }
                    repository.disconnect()
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(loading = false, error = result.message) }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(savedMessage = null, error = null) }
    }
}
