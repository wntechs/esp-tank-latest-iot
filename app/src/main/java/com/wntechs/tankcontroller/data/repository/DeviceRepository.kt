package com.wntechs.tankcontroller.data.repository

import android.os.Build
import android.util.Log
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
        // Monitor both device selection and MQTT credentials
        CoroutineScope(Dispatchers.IO).launch {
            combine(
                settings.map { it.deviceId }.distinctUntilChanged(),
                settingsStore.mqttCredsFlow.distinctUntilChanged(),
                authState.map { it.isLoggedIn }.distinctUntilChanged()
            ) { deviceId: String, creds: MqttCredentials?, isLoggedIn: Boolean ->
                Triple(deviceId, creds, isLoggedIn)
            }.collect { (deviceId, creds, isLoggedIn) ->
                currentDeviceUuid = deviceId
                
                if (!isLoggedIn || deviceId.isBlank()) {
                    mqttManager.disconnect()
                    _connectionState.value = MqttConnectionState.Disconnected
                    return@collect
                }

                if (creds != null) {
                    // We have credentials, connect!
                    mqttManager.connect(creds, deviceId)
                    _connectionState.value = MqttConnectionState.Connected
                } else {
                    // We have a device but NO credentials (likely after a fresh login)
                    // Automatically trigger the credential fetch
                    Log.d("DeviceRepository", "Device selected but no MQTT creds found. Fetching...")
                    connectWithDynamicCredentials()
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
                // SettingsStore.mqttCredsFlow will trigger the connection in the init block's combine observer
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
        if (currentDeviceUuid.isBlank()) return
        mqttManager.publish(getPublishTopic("get_status"))
        mqttManager.publish(getPublishTopic("get_config"))
    }

    fun setManual(turnOn: Boolean) {
        if (currentDeviceUuid.isBlank()) return
        val cmd = if (turnOn) "ON" else "OFF"
        mqttManager.publish(getPublishTopic("manual"), cmd)
    }

    fun setAuto() {
        if (currentDeviceUuid.isBlank()) return
        mqttManager.publish(getPublishTopic("auto"))
    }

    fun updateConfig(request: ConfigUpdateRequest) {
        if (currentDeviceUuid.isBlank()) return
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
