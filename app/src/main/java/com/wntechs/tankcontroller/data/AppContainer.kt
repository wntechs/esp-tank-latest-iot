package com.wntechs.tankcontroller.data

import android.content.Context
import com.wntechs.tankcontroller.data.discovery.MqttManager
import com.wntechs.tankcontroller.data.local.SettingsStore
import com.wntechs.tankcontroller.data.remote.ApiClientFactory
import com.wntechs.tankcontroller.data.repository.DeviceRepository
import com.wntechs.tankcontroller.data.repository.UserRepository
import com.wntechs.tankcontroller.data.repository.UserSettings
import kotlinx.coroutines.flow.map


class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settingsStore = SettingsStore(appContext)
    val mqttManager = MqttManager()
    val apiClientFactory = ApiClientFactory(settingsStore)

    private val userSettingsFlow = settingsStore.settingsFlow.map { settings ->
        val rawUrl = settings.baseUrl.ifBlank { "192.46.215.185" }
        UserSettings(
            baseUrl = rawUrl
                .replace("http://", "")
                .replace("https://", "")
                .split(":")[0],
            deviceId = settings.deviceId.ifBlank { "relay1" }
        )
    }

    val deviceRepository = DeviceRepository(
        mqttManager = mqttManager,
        settings = userSettingsFlow,
        settingsStore = settingsStore
    )

    val userRepository = UserRepository(
        authApi = apiClientFactory.createAuthApi(),
        settingsStore = settingsStore
    )
}
