package com.wntechs.tankcontroller.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SensorListResponse(
    val success: Boolean,
    val command: String,
    val count: Int = 0,
    val pair_requested: Boolean = false,
    val pair_confirmed: Boolean = false,
    val paired_sensor_id: String = "",
    val sensors: List<SensorItem> = emptyList()
)

@Serializable
data class SensorItem(
    val index: Int,
    val id: String,
    val status: String,
    val paired: Boolean,
    val selected: Boolean,
    val age_ms: Long
)
