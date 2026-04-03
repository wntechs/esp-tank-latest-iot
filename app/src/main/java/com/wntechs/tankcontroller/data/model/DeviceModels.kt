package com.wntechs.tankcontroller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceListResponse(
    val data: List<OwnedDevice>
)

@Serializable
data class OwnedDevice(
    val uuid: String,
    @SerialName("serial_number") val serialNumber: String,
    val model: String? = null,
    val status: String,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    val ownership: DeviceOwnership
)

@Serializable
data class DeviceOwnership(
    val role: String,
    val status: String,
    @SerialName("started_at") val startedAt: String? = null
)
