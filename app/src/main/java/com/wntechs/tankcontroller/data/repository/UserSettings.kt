package com.wntechs.tankcontroller.data.repository

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val baseUrl: String = "", // This will be your MQTT Broker IP/Address
    val deviceId: String = "relay1"
)