package com.wntechs.tankcontroller.data.repository

import android.os.Build
import com.wntechs.tankcontroller.data.discovery.MqttManager
import com.wntechs.tankcontroller.data.local.SettingsStore
import com.wntechs.tankcontroller.data.model.ConfigUpdateRequest
import com.wntechs.tankcontroller.data.model.MqttCredentials
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DeviceRepository(
    private val mqttManager: MqttManager,
    private val userRepository: UserRepository,
    private val settingsStore: SettingsStore,
    val settings: Flow<UserSettings>
) {
    private var currentDeviceUuid: String = ""
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    val authState = userRepository.authState

    init {
        CoroutineScope(Dispatchers.IO).launch {
            settings.collect {
                currentDeviceUuid = it.deviceId
            }
        }
        
        // Monitor saved credentials and auto-connect if available
        CoroutineScope(Dispatchers.IO).launch {
            settingsStore.mqttCredsFlow.collect { creds ->
                if (creds != null && currentDeviceUuid.isNotBlank()) {
                    mqttManager.connect(creds, currentDeviceUuid)
                    _connectionState.value = MqttConnectionState.Connected
                } else {
                    mqttManager.disconnect()
                    _connectionState.value = MqttConnectionState.Disconnected
                }
            }
        }
    }

    val status = mqttManager.statusFlow
    val config = mqttManager.configFlow
    val errors = mqttManager.errorFlow
    val isOnline = mqttManager.isDeviceOnline

    suspend fun connectWithDynamicCredentials(): AppResult<Unit> {
        // Ensure user is logged in
        val auth = authState.first()
        if (!auth.isLoggedIn) {
            return AppResult.Error("User is not logged in")
        }

        val deviceUuid = settings.first().deviceId
        if (deviceUuid.isBlank()) return AppResult.Error("Device UUID is missing")
        
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        
        _connectionState.value = MqttConnectionState.Connecting
        
        return when (val result = userRepository.getMqttCredentials(deviceUuid, deviceName)) {
            is AppResult.Success -> {
                // SettingsStore.mqttCredsFlow will trigger the connection in init block
                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                _connectionState.value = MqttConnectionState.Error(result.message)
                AppResult.Error(result.message)
            }
        }
    }

    suspend fun refreshCredentials(): AppResult<Unit> {
        val creds = settingsStore.mqttCredsFlow.first() ?: return AppResult.Error("No existing credentials")
        val deviceUuid = settings.first().deviceId
        
        return when (val result = userRepository.refreshMqttCredentials(deviceUuid, creds.clientId)) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Error -> AppResult.Error(result.message)
        }
    }

    private fun getPublishTopic(subPath: String): String {
        return "devices/$currentDeviceUuid/commands/mobile/$subPath"
    }

    fun requestUpdate() {
        mqttManager.publish(getPublishTopic("get_status"))
        mqttManager.publish(getPublishTopic("get_config"))
    }

    fun setManual(turnOn: Boolean) {
        val cmd = if (turnOn) "ON" else "OFF"
        mqttManager.publish(getPublishTopic("manual"), cmd)
    }

    fun setAuto() = mqttManager.publish(getPublishTopic("auto"))

    fun updateConfig(request: ConfigUpdateRequest) {
        val payload = Json.encodeToString(request)
        mqttManager.publish(getPublishTopic("config"), payload)
    }

    fun disconnect() {
        mqttManager.disconnect()
        _connectionState.value = MqttConnectionState.Disconnected
    }

    suspend fun saveDeviceId(id: String) {
        settingsStore.saveDeviceId(id)
    }
}

sealed class MqttConnectionState {
    data object Disconnected : MqttConnectionState()
    data object Connecting : MqttConnectionState()
    data object Connected : MqttConnectionState()
    data class Error(val message: String) : MqttConnectionState()
}
