package com.wntechs.tankcontroller.data

import android.content.Context
import com.wntechs.tankcontroller.data.discovery.AndroidNsdDiscoveryManager
import com.wntechs.tankcontroller.data.discovery.DiscoveryManager
import com.wntechs.tankcontroller.data.local.SettingsStore
import com.wntechs.tankcontroller.data.remote.ApiClientFactory
import com.wntechs.tankcontroller.data.repository.DeviceRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settingsStore = SettingsStore(appContext)
    val discoveryManager: DiscoveryManager = AndroidNsdDiscoveryManager(appContext)
    val apiClientFactory = ApiClientFactory(settingsStore)
    val deviceRepository = DeviceRepository(settingsStore, apiClientFactory, discoveryManager)
}
