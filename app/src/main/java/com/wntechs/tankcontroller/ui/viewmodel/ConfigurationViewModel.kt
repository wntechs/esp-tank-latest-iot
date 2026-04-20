package com.wntechs.tankcontroller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wntechs.tankcontroller.data.model.ConfigResponse
import com.wntechs.tankcontroller.data.model.ConfigUpdateRequest
import com.wntechs.tankcontroller.data.repository.DeviceRepository
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
    val startLevelPercent: Int = 0,
    val stopLevelPercent: Int = 0,
    val minMotorRunSeconds: String = "",
    val minMotorOffSeconds: String = "",
    val sensorTimeoutSeconds: String = "",
    val maxMotorRunSeconds: String = "", // max_motor_run_seconds
)

data class ConfigurationUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val initialForm: ConfigurationForm? = null,
    val form: ConfigurationForm = ConfigurationForm(),
    val message: String? = null,
    val error: String? = null,
) {
    val isDirty: Boolean get() = initialForm != null && form != initialForm
}

class ConfigurationViewModel(
    private val repository: DeviceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ConfigurationUiState())
    val uiState: StateFlow<ConfigurationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.config.collect { config ->
                val newForm = config.toForm()
                _uiState.update {
                    it.copy(
                        loading = false,
                        saving = false,
                        initialForm = if (it.initialForm == null) newForm else it.initialForm,
                        form = if (it.initialForm == null) newForm else it.form,
                        message = if (it.saving) "Configuration updated" else it.message
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.errors.collect { errorMsg ->
                _uiState.update { it.copy(loading = false, saving = false, error = errorMsg) }
            }
        }

        load()
    }

    fun load() {
        _uiState.update { it.copy(loading = true, error = null, message = null, initialForm = null) }
        repository.requestUpdate()
    }

    fun save() {
        val current = _uiState.value.form
        val validation = validate(current)
        if (validation != null) {
            _uiState.update { it.copy(error = validation, message = null) }
            return
        }

        _uiState.update { it.copy(saving = true, error = null, message = null) }
        repository.updateConfig(current.toRequest())
        // Reset initialForm after save so button disables until next edit
        _uiState.update { it.copy(initialForm = current) }
    }

    fun updateField(transform: (ConfigurationForm) -> ConfigurationForm) {
        _uiState.update { it.copy(form = transform(it.form), message = null, error = null) }
    }

    private fun validate(form: ConfigurationForm): String? {
        val tankHeight = form.tankHeightMm.toIntOrNull() ?: return "Tank height must be a number"
        if (tankHeight <= 0) return "Tank height must be greater than 0"
        
        val sensorTopOffset = form.sensorTopOffsetMm.toIntOrNull() ?: return "Sensor top offset is required"
        if (sensorTopOffset >= tankHeight) return "Sensor top offset must be less than tank height"

        if (form.startLevelPercent >= form.stopLevelPercent) return "Start level must be less than stop level"
        
        if (form.tankShape == 0 && (form.tankDiameterMm.toIntOrNull() ?: 0) <= 0) return "Diameter must be greater than 0"
        if (form.tankShape == 1) {
            val length = form.tankLengthMm.toIntOrNull() ?: 0
            val breadth = form.tankBreadthMm.toIntOrNull() ?: 0
            if (length <= 0 || breadth <= 0) return "Length and breadth must be greater than 0"
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
    startLevelPercent = startLevelPercent,
    stopLevelPercent = stopLevelPercent,
    minMotorRunSeconds = minMotorRunSeconds.toString(),
    minMotorOffSeconds = minMotorOffSeconds.toString(),
    sensorTimeoutSeconds = sensorTimeoutSeconds.toString(),
    maxMotorRunSeconds = maxMotorRunSeconds.toString(),
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
    startLevelPercent = startLevelPercent,
    stopLevelPercent = stopLevelPercent,
    minMotorRunSeconds = minMotorRunSeconds.toIntOrNull(),
    minMotorOffSeconds = minMotorOffSeconds.toIntOrNull(),
    sensorTimeoutSeconds = sensorTimeoutSeconds.toIntOrNull(),
    maxMotorRunSeconds = maxMotorRunSeconds.toIntOrNull(),
)
