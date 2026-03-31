package com.wntechs.tankcontroller.data.repository

import com.wntechs.tankcontroller.data.discovery.MqttManager
import com.wntechs.tankcontroller.data.local.SettingsStore
import com.wntechs.tankcontroller.data.model.ConfigUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DeviceRepository(
    private val mqttManager: MqttManager,
    private val settingsStore: SettingsStore,
    val settings: Flow<UserSettings>
) {
    private var currentDeviceId: String = "relay1"

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settings.collect {
                currentDeviceId = it.deviceId
            }
        }
    }

    val status = mqttManager.statusFlow
    val config = mqttManager.configFlow
    val errors = mqttManager.errorFlow
    val isOnline = mqttManager.isDeviceOnline

    fun connect(url: String, deviceId: String) = mqttManager.connect(url, deviceId)

    /**
     * Forces a disconnection and reconnects using the latest settings stored in DataStore.
     * Call this after saving new connection parameters in the Settings screen.
     */
    fun reconnect() {
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Get the latest settings from the Flow (first() takes current value and completes)
            val currentSettings = settings.first()

            // 2. Disconnect existing client
            mqttManager.disconnect()

            // 3. Connect with new parameters
            mqttManager.connect(currentSettings.baseUrl, currentSettings.deviceId)
        }
    }

    fun requestUpdate() = mqttManager.publish("tank/$currentDeviceId/cmd/get_status")

    fun setManual(turnOn: Boolean) {
        val cmd = if (turnOn) "ON" else "OFF"
        mqttManager.publish("tank/$currentDeviceId/cmd/manual", cmd)
    }

    fun setAuto() = mqttManager.publish("tank/$currentDeviceId/cmd/auto")

    fun updateConfig(request: ConfigUpdateRequest) {
        val payload = Json.encodeToString(request)
        mqttManager.publish("tank/$currentDeviceId/cmd/config", payload)
    }

    suspend fun saveBaseUrl(url: String) {
        settingsStore.saveBaseUrl(url)
    }

    suspend fun saveDeviceId(id: String) {
        settingsStore.saveDeviceId(id)
    }
}
