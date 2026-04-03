package com.wntechs.tankcontroller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PairingStartRequest(
    val code: String
)

@Serializable
data class PairingClaimRequest(
    val token: String
)

@Serializable
data class ProvisioningStatusResponse(
    val data: ProvisioningData? = null
)

@Serializable
data class ProvisioningData(
    val uuid: String? = null,
    val token: String? = null,
    val status: String,
    val message: String? = null
)

@Serializable
data class PairingResponse(
    val data: PairingResponseData? = null,
    val success: Boolean? = true,
    val message: String? = null,
    val token: String? = null
)

@Serializable
data class PairingResponseData(
    val token: String? = null,
    val code: String? = null,
    val status: String? = null,
    val device: DeviceInfo? = null,
    val message: String? = null,
    val success: Boolean? = true
)

@Serializable
data class DeviceInfo(
    val uuid: String,
    @SerialName("serial_number") val serialNumber: String,
    val status: String
)
