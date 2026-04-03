package com.wntechs.tankcontroller.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MqttCredentialsRequest(
    @SerialName("device_uuid") val deviceUuid: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("client_id") val clientId: String? = null
)

@Serializable
data class MqttRefreshRequest(
    @SerialName("device_uuid") val deviceUuid: String,
    @SerialName("client_id") val clientId: String
)

@Serializable
data class MqttCredentialsResponse(
    val data: MqttData
)

@Serializable
data class MqttData(
    val mqtt: MqttCredentials
)

@Serializable
data class MqttCredentials(
    val host: String,
    val port: Int,
    @SerialName("client_id") val clientId: String,
    val username: String,
    val password: String,
    @SerialName("expires_at") val expiresAt: String,
    val topics: MqttTopics
)

@Serializable
data class MqttTopics(
    val subscribe: List<String>,
    val publish: List<String>
)
