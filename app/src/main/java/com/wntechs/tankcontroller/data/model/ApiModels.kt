package com.wntechs.tankcontroller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatusResponse(
    @SerialName("motor_on") val motorOn: Boolean = false,
    @SerialName("auto_mode_enabled") val autoModeEnabled: Boolean = true,
    @SerialName("sensor_connected") val sensorConnected: Boolean = false,
    @SerialName("reading_valid") val readingValid: Boolean = false,
    @SerialName("filtered_distance_mm") val filteredDistanceMm: Int = 0,
    @SerialName("water_height_mm") val waterHeightMm: Int = 0,
    @SerialName("water_level_percent") val waterLevelPercent: Int = 0,
    @SerialName("litres") val litres: Int = 0,
    @SerialName("last_sensor_packet_ms") val lastSensorPacketMs: Long = 0,
    @SerialName("fault_sensor_timeout") val faultSensorTimeout: Boolean = false,
    @SerialName("fault_invalid_reading") val faultInvalidReading: Boolean = false,
    @SerialName("manual_override") val manualOverride: Boolean = false,
    @SerialName("auto_mode") val autoMode: Boolean = true,
)

@Serializable
data class ConfigResponse(
    @SerialName("tank_shape") val tankShape: Int = 0,
    @SerialName("tank_height_mm") val tankHeightMm: Int = 0,
    @SerialName("tank_diameter_mm") val tankDiameterMm: Int = 0,
    @SerialName("tank_length_mm") val tankLengthMm: Int = 0,
    @SerialName("tank_breadth_mm") val tankBreadthMm: Int = 0,
    @SerialName("sensor_top_offset_mm") val sensorTopOffsetMm: Int = 0,
    @SerialName("sensor_dead_zone_mm") val sensorDeadZoneMm: Int = 0,
    @SerialName("min_valid_distance_mm") val minValidDistanceMm: Int = 0,
    @SerialName("max_valid_distance_mm") val maxValidDistanceMm: Int = 0,
    @SerialName("start_level_percent") val startLevelPercent: Int = 0,
    @SerialName("stop_level_percent") val stopLevelPercent: Int = 0,
    @SerialName("min_motor_run_seconds") val minMotorRunSeconds: Int = 0,
    @SerialName("min_motor_off_seconds") val minMotorOffSeconds: Int = 0,
    @SerialName("sensor_timeout_seconds") val sensorTimeoutSeconds: Int = 0,
)

@Serializable
data class ConfigUpdateRequest(
    @SerialName("tank_shape") val tankShape: Int? = null,
    @SerialName("tank_height_mm") val tankHeightMm: Int? = null,
    @SerialName("tank_diameter_mm") val tankDiameterMm: Int? = null,
    @SerialName("tank_length_mm") val tankLengthMm: Int? = null,
    @SerialName("tank_breadth_mm") val tankBreadthMm: Int? = null,
    @SerialName("sensor_top_offset_mm") val sensorTopOffsetMm: Int? = null,
    @SerialName("sensor_dead_zone_mm") val sensorDeadZoneMm: Int? = null,
    @SerialName("min_valid_distance_mm") val minValidDistanceMm: Int? = null,
    @SerialName("max_valid_distance_mm") val maxValidDistanceMm: Int? = null,
    @SerialName("start_level_percent") val startLevelPercent: Int? = null,
    @SerialName("stop_level_percent") val stopLevelPercent: Int? = null,
    @SerialName("min_motor_run_seconds") val minMotorRunSeconds: Int? = null,
    @SerialName("min_motor_off_seconds") val minMotorOffSeconds: Int? = null,
    @SerialName("sensor_timeout_seconds") val sensorTimeoutSeconds: Int? = null,
)

@Serializable
data class ManualRelayRequest(
    val state: Boolean,
)

@Serializable
data class RelayModeResponse(
    val success: Boolean = false,
    val manual: Boolean? = null,
    @SerialName("motor_on") val motorOn: Boolean? = null,
    @SerialName("auto_mode") val autoMode: Boolean? = null,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class ApiErrorResponse(
    val success: Boolean? = null,
    val error: String? = null,
    val message: String? = null,
)
