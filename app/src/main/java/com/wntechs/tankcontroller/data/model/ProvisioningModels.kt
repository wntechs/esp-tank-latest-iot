package com.wntechs.tankcontroller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobileProvisioningRequest(
    val uuid: String,
    @SerialName("device_secret") val deviceSecret: String,
    @SerialName("firmware_version") val firmwareVersion: String? = null,
    @SerialName("rotate_credentials") val rotateCredentials: Boolean = false
)

@Serializable
data class MobileProvisioningResponse(
    val data: MobileProvisioningData
)

@Serializable
data class MobileProvisioningData(
    @SerialName("credentials_already_issued") val credentialsAlreadyIssued: Boolean,
    val mqtt: DeviceMqttCredentials
)

@Serializable
data class DeviceMqttCredentials(
    val host: String,
    val port: Int,
    @SerialName("client_id") val clientId: String,
    val username: String,
    val password: String? = null,
    val topics: List<String>
)
