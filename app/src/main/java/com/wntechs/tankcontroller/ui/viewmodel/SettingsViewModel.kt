package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.model.TankFamily
import com.wntechs.tankcontroller.data.model.TankMeasurements
import com.wntechs.tankcontroller.data.model.TankModel
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class SettingsUiState(
    val baseUrl: String = "",
    val deviceId: String = "relay1",
    val savedMessage: String? = null,
    val tankMeasurements: TankMeasurements? = null,
    val selectedFamily: TankFamily? = null,
    val selectedModel: TankModel? = null,
)

class SettingsViewModel(
    private val repository: DeviceRepository,
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
            _uiState.update { it.copy(savedMessage = "Tank preset '${model.code}' applied to device") }
        }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            repository.saveBaseUrl(state.baseUrl)
            repository.saveDeviceId(state.deviceId)
            _uiState.update { it.copy(savedMessage = "Settings saved successfully") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(savedMessage = null) }
    }
}
