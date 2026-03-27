package com.wntechs.tankcontroller.data.repository

import com.wntechs.tankcontroller.data.discovery.DiscoveryManager
import com.wntechs.tankcontroller.data.local.SettingsStore
import com.wntechs.tankcontroller.data.model.ConfigResponse
import com.wntechs.tankcontroller.data.model.ConfigUpdateRequest
import com.wntechs.tankcontroller.data.model.DiscoveredDevice
import com.wntechs.tankcontroller.data.model.ManualRelayRequest
import com.wntechs.tankcontroller.data.model.StatusResponse
import com.wntechs.tankcontroller.data.remote.ApiClientFactory
import com.wntechs.tankcontroller.util.AppResult
import kotlinx.coroutines.flow.Flow

class DeviceRepository(
    private val settingsStore: SettingsStore,
    private val apiClientFactory: ApiClientFactory,
    val discoveryManager: DiscoveryManager,
) {
    val settings = settingsStore.settingsFlow
    val discoveredDevices = discoveryManager.discoveredDevices
    val isScanning = discoveryManager.isScanning

    fun startDiscovery(serviceType: String = "_http._tcp.") = discoveryManager.startDiscovery(serviceType)
    fun stopDiscovery() = discoveryManager.stopDiscovery()

    suspend fun saveBaseUrl(baseUrl: String, hostName: String = "", ipAddress: String = "") {
        settingsStore.saveConnection(
            baseUrl = apiClientFactory.normalizeBaseUrl(baseUrl),
            hostName = hostName,
            ipAddress = ipAddress,
        )
    }

    suspend fun setPreferMdns(preferMdns: Boolean) = settingsStore.setPreferMdns(preferMdns)

    suspend fun selectDiscoveredDevice(device: DiscoveredDevice, preferMdns: Boolean) {
        val baseUrl = if (preferMdns) device.mdnsBaseUrl else device.httpBaseUrl
        saveBaseUrl(baseUrl, hostName = device.hostName, ipAddress = device.ipAddress)
    }

    suspend fun getStatus(): AppResult<StatusResponse> = call { apiClientFactory.create().getStatus() }
    suspend fun getConfig(): AppResult<ConfigResponse> = call { apiClientFactory.create().getConfig() }
    suspend fun updateConfig(request: ConfigUpdateRequest) = call { apiClientFactory.create().updateConfig(request) }
    suspend fun setManual(state: Boolean) = call { apiClientFactory.create().setManualRelay(ManualRelayRequest(state)) }
    suspend fun setAuto() = call { apiClientFactory.create().setAutoMode() }
    suspend fun pingRoot() = call { apiClientFactory.create().root() }

    private suspend fun <T> call(block: suspend () -> T): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (t: Throwable) {
            AppResult.Error(t.message ?: "Unknown error", t)
        }
}
