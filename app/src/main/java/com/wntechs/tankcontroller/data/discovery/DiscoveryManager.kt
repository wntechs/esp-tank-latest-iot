package com.wntechs.tankcontroller.data.discovery

import com.wntechs.tankcontroller.data.model.DiscoveredDevice
import kotlinx.coroutines.flow.StateFlow

interface DiscoveryManager {
    val discoveredDevices: StateFlow<List<DiscoveredDevice>>
    val isScanning: StateFlow<Boolean>
    fun startDiscovery(serviceType: String = "_http._tcp.")
    fun stopDiscovery()
}
