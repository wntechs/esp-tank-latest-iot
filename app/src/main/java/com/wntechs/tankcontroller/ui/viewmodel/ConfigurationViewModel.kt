package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.model.ConfigResponse
import com.wntechs.tankcontroller.data.model.ConfigUpdateRequest
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfigurationForm(
    val tankShape: Int = 0,
    val tankHeightMm: String = "",
    val tankDiameterMm: String = "",
    val tankLengthMm: String = "",
    val tankBreadthMm: String = "",
    val sensorTopOffsetMm: String = "",
    val sensorDeadZoneMm: String = "",
    val minValidDistanceMm: String = "",
    val maxValidDistanceMm: String = "",
    val startLevelPercent: String = "",
    val stopLevelPercent: String = "",
    val minMotorRunSeconds: String = "",
    val minMotorOffSeconds: String = "",
    val sensorTimeoutSeconds: String = "",
)

data class ConfigurationUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val form: ConfigurationForm = ConfigurationForm(),
    val message: String? = null,
    val error: String? = null,
)

class ConfigurationViewModel(
    private val repository: DeviceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConfigurationUiState())
    val uiState: StateFlow<ConfigurationUiState> = _uiState.asStateFlow()

    init {
        // 1. Listen for Config Updates from MQTT
        viewModelScope.launch {
            repository.config.collect { config ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        saving = false,
                        form = config.toForm(),
                        message = if (it.saving) "Configuration updated" else null
                    )
                }
            }
        }

        // 2. Listen for Errors from MQTT
        viewModelScope.launch {
            repository.errors.collect { errorMsg ->
                _uiState.update { it.copy(loading = false, saving = false, error = errorMsg) }
            }
        }

        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null, message = null) }
        // In MQTT, we send a command to the device to publish its current config
        repository.requestUpdate() // Assuming this triggers 'get_config' via repository
    }

    fun save() {
        val current = _uiState.value.form
        val validation = validate(current)
        if (validation != null) {
            _uiState.update { it.copy(error = validation, message = null) }
            return
        }

        _uiState.update { it.copy(saving = true, error = null, message = null) }
        // Publish the new configuration to the 'cmd/config' topic
        repository.updateConfig(current.toRequest())

        // Note: We don't handle Success/Error here.
        // The UI will update when the device publishes the new config back to the status topic.
    }

    fun update(transform: (ConfigurationForm) -> ConfigurationForm) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }

    private fun validate(form: ConfigurationForm): String? {
        val tankHeight = form.tankHeightMm.toIntOrNull() ?: return "tank_height_mm must be greater than 0"
        val sensorTopOffset = form.sensorTopOffsetMm.toIntOrNull() ?: return "sensor_top_offset_mm is required"
        val minValid = form.minValidDistanceMm.toIntOrNull() ?: return "min_valid_distance_mm is required"
        val maxValid = form.maxValidDistanceMm.toIntOrNull() ?: return "max_valid_distance_mm is required"
        val start = form.startLevelPercent.toIntOrNull() ?: return "start_level_percent is required"
        val stop = form.stopLevelPercent.toIntOrNull() ?: return "stop_level_percent is required"

        if (tankHeight <= 0) return "tank_height_mm must be greater than 0"
        if (sensorTopOffset >= tankHeight) return "sensor_top_offset_mm must be less than tank_height_mm"
        if (minValid >= maxValid) return "min_valid_distance_mm must be less than max_valid_distance_mm"
        if (start >= stop) return "start_level_percent must be less than stop_level_percent"
        if (stop > 100) return "stop_level_percent must be <= 100"
        if (form.tankShape == 0 && (form.tankDiameterMm.toIntOrNull() ?: 0) <= 0) return "tank_diameter_mm must be greater than 0 for cylindrical tank"
        if (form.tankShape == 1) {
            val length = form.tankLengthMm.toIntOrNull() ?: 0
            val breadth = form.tankBreadthMm.toIntOrNull() ?: 0
            if (length <= 0 || breadth <= 0) return "tank_length_mm and tank_breadth_mm must be greater than 0 for rectangular tank"
        }
        return null
    }
}

private fun ConfigResponse.toForm() = ConfigurationForm(
    tankShape = tankShape,
    tankHeightMm = tankHeightMm.toString(),
    tankDiameterMm = tankDiameterMm.toString(),
    tankLengthMm = tankLengthMm.toString(),
    tankBreadthMm = tankBreadthMm.toString(),
    sensorTopOffsetMm = sensorTopOffsetMm.toString(),
    sensorDeadZoneMm = sensorDeadZoneMm.toString(),
    minValidDistanceMm = minValidDistanceMm.toString(),
    maxValidDistanceMm = maxValidDistanceMm.toString(),
    startLevelPercent = startLevelPercent.toString(),
    stopLevelPercent = stopLevelPercent.toString(),
    minMotorRunSeconds = minMotorRunSeconds.toString(),
    minMotorOffSeconds = minMotorOffSeconds.toString(),
    sensorTimeoutSeconds = sensorTimeoutSeconds.toString(),
)

private fun ConfigurationForm.toRequest() = ConfigUpdateRequest(
    tankShape = tankShape,
    tankHeightMm = tankHeightMm.toIntOrNull(),
    tankDiameterMm = tankDiameterMm.toIntOrNull(),
    tankLengthMm = tankLengthMm.toIntOrNull(),
    tankBreadthMm = tankBreadthMm.toIntOrNull(),
    sensorTopOffsetMm = sensorTopOffsetMm.toIntOrNull(),
    sensorDeadZoneMm = sensorDeadZoneMm.toIntOrNull(),
    minValidDistanceMm = minValidDistanceMm.toIntOrNull(),
    maxValidDistanceMm = maxValidDistanceMm.toIntOrNull(),
    startLevelPercent = startLevelPercent.toIntOrNull(),
    stopLevelPercent = stopLevelPercent.toIntOrNull(),
    minMotorRunSeconds = minMotorRunSeconds.toIntOrNull(),
    minMotorOffSeconds = minMotorOffSeconds.toIntOrNull(),
    sensorTimeoutSeconds = sensorTimeoutSeconds.toIntOrNull(),
)
