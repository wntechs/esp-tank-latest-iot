package com.wntechs.tankcontroller.data.repository

import androidx.activity.result.launch
import com.wntechs.tankcontroller.data.discovery.MqttManager
import com.wntechs.tankcontroller.data.model.ConfigUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class DeviceRepository(
    private val mqttManager: MqttManager,
    val settings: Flow<UserSettings>
) {
    // Helper to get current deviceId from the flow safely for one-off commands
    // In a real app, you might want to store the last known ID in a variable
    private var currentDeviceId: String = "relay1"

    init {
        // Keep the local deviceId updated whenever settings change
        CoroutineScope(Dispatchers.IO).launch {
            settings.collect { currentDeviceId = it.deviceId }
        }
    }

    val status = mqttManager.statusFlow
    val config = mqttManager.configFlow
    val errors = mqttManager.errorFlow
    val isOnline = mqttManager.isDeviceOnline

    fun connect(url: String, deviceId: String) = mqttManager.connect(url, deviceId)

    fun requestUpdate() = mqttManager.publish("tank/$currentDeviceId/cmd/get_status")

    fun setManual(turnOn: Boolean) {
        val cmd = if (turnOn) "ON" else "OFF"
        mqttManager.publish("tank/$currentDeviceId/cmd/manual", cmd)
    }

    fun setAuto() = mqttManager.publish("tank/$currentDeviceId/cmd/auto")

    fun updateConfig(request: ConfigUpdateRequest) {
        val payload = Json.encodeToString(ConfigUpdateRequest.serializer(), request)
        mqttManager.publish("tank/$currentDeviceId/cmd/config", payload)
    }

    // Add these to DeviceRepository.kt if not present
    suspend fun saveBaseUrl(url: String) {
        // This should call your settingsStore.updateBaseUrl(url)
    }

    suspend fun saveDeviceId(id: String) {
        // This should call your settingsStore.updateDeviceId(id)
    }
}