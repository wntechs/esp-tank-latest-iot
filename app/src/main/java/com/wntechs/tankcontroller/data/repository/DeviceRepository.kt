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
        // Monitor both device selection and user session
        CoroutineScope(Dispatchers.IO).launch {
            combine(
                settings.map { it.deviceId }.distinctUntilChanged(),
                authState.map { it.isLoggedIn }.distinctUntilChanged()
            ) { deviceId: String, isLoggedIn: Boolean ->
                Pair(deviceId, isLoggedIn)
            }.collect { (deviceId, isLoggedIn) ->
                currentDeviceUuid = deviceId
                
                if (!isLoggedIn || deviceId.isBlank()) {
                    mqttManager.disconnect()
                    _connectionState.value = MqttConnectionState.Disconnected
                    return@collect
                }

                // Optimization: Try to connect using whatever we have first
                connectWithDynamicCredentials()
            }
        }
    }

    val status = mqttManager.statusFlow
    val config = mqttManager.configFlow
    val errors = mqttManager.errorFlow
    val isOnline = mqttManager.isDeviceOnline
    // Add this line to expose the sensor list from MqttManager
    val sensorListFlow = mqttManager.sensorListFlow

    suspend fun connectWithDynamicCredentials(): AppResult<Unit> {
        val auth = authState.first()
        if (!auth.isLoggedIn) return AppResult.Error("User is not logged in")

        val deviceUuid = settings.first().deviceId
        if (deviceUuid.isBlank()) return AppResult.Error("Device UUID is missing")
        
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val appKey = settingsStore.appDeviceKeyFlow.first()
        
        _connectionState.value = MqttConnectionState.Connecting

        // 1. Try connecting with stored credentials first
        val existingCreds = settingsStore.mqttCredsFlow.first()
        if (existingCreds != null) {
            Log.d("DeviceRepository", "Found stored MQTT creds. Attempting direct connection...")
            if (mqttManager.connect(existingCreds, deviceUuid)) {
                _connectionState.value = MqttConnectionState.Connected
                return AppResult.Success(Unit)
            }
            Log.d("DeviceRepository", "Stored credentials failed (likely expired).")
        }

        // 2. Direct connection failed or no creds found -> Try REFRESH
        Log.d("DeviceRepository", "Attempting MQTT credential refresh...")
        val refreshResult = userRepository.refreshMqttCredentials(deviceUuid, appKey)
        if (refreshResult is AppResult.Success) {
            if (mqttManager.connect(refreshResult.data, deviceUuid)) {
                _connectionState.value = MqttConnectionState.Connected
                return AppResult.Success(Unit)
            }
        }

        // 3. Refresh failed -> Call ISSUE (Full rotation)
        Log.d("DeviceRepository", "Refresh failed. Issuing fresh MQTT credentials...")
        return when (val issueResult = userRepository.getMqttCredentials(deviceUuid, deviceName, appKey)) {
            is AppResult.Success -> {
                if (mqttManager.connect(issueResult.data, deviceUuid)) {
                    _connectionState.value = MqttConnectionState.Connected
                    AppResult.Success(Unit)
                } else {
                    val msg = "MQTT connection failed even with fresh credentials"
                    _connectionState.value = MqttConnectionState.Error(msg)
                    AppResult.Error(msg)
                }
            }
            is AppResult.Error -> {
                _connectionState.value = MqttConnectionState.Error(issueResult.message)
                AppResult.Error(issueResult.message)
            }
        }
    }

    suspend fun refreshCredentials(): AppResult<Unit> {
        val deviceUuid = settings.first().deviceId
        val appKey = settingsStore.appDeviceKeyFlow.first()
        
        return when (val result = userRepository.refreshMqttCredentials(deviceUuid, appKey)) {
            is AppResult.Success -> {
                mqttManager.connect(result.data, deviceUuid)
                AppResult.Success(Unit)
            }
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

    fun rebootDevice() {
        if (currentDeviceUuid.isBlank()) return

        mqttManager.publish(getPublishTopic("reboot"))
    }

    fun setAuto() {
        if (currentDeviceUuid.isBlank()) return
        mqttManager.publish(getPublishTopic("auto"))
    }

    fun getSensors() {
        if (currentDeviceUuid.isBlank()) return
        mqttManager.publish(getPublishTopic("pairing/list"))
    }

    fun selectSensor(index: Int) {
        if (currentDeviceUuid.isBlank()) return
        mqttManager.publish(getPublishTopic("pairing/select"), index.toString())
    }

    fun unSelectSensor() {
        if (currentDeviceUuid.isBlank()) return
        mqttManager.publish(getPublishTopic("pairing/unselect"))
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

    suspend fun fetchMqttProvisioningCredentials(): AppResult<MqttCredentials> {
        val auth = authState.first()
        if (!auth.isLoggedIn) return AppResult.Error("User is not logged in")

        val deviceUuid = settings.first().deviceId
        if (deviceUuid.isBlank()) return AppResult.Error("Device UUID is missing")

        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val appKey = settingsStore.appDeviceKeyFlow.first()

        val existingCreds = settingsStore.mqttCredsFlow.first()
        if (existingCreds != null) {
            Log.d("DeviceRepository", "Found stored MQTT creds. Using them for provisioning...")
            return AppResult.Success(existingCreds)
        }

        Log.d("DeviceRepository", "Attempting MQTT credential refresh...")
        when (val refreshResult = userRepository.refreshMqttCredentials(deviceUuid, appKey)) {
            is AppResult.Success -> return AppResult.Success(refreshResult.data)
            is AppResult.Error -> Log.d("DeviceRepository", "Refresh failed, issuing fresh creds: ${refreshResult.message}")
        }

        return when (val issueResult = userRepository.getMqttCredentials(deviceUuid, deviceName, appKey)) {
            is AppResult.Success -> AppResult.Success(issueResult.data)
            is AppResult.Error -> AppResult.Error(issueResult.message)
        }
    }
}

sealed class MqttConnectionState {
    data object Disconnected : MqttConnectionState()
    data object Connecting : MqttConnectionState()
    data object Connected : MqttConnectionState()
    data class Error(val message: String) : MqttConnectionState()
}
