package com.wntechs.tankcontroller.data.discovery

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.wntechs.tankcontroller.data.model.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.InetAddress

@SuppressLint("MissingPermission")
class AndroidNsdDiscoveryManager(context: Context) : DiscoveryManager {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    override val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning

    private var listener: NsdManager.DiscoveryListener? = null

    override fun startDiscovery(serviceType: String) {
        stopDiscovery()
        _discoveredDevices.value = emptyList()
        listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                _isScanning.value = false
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                _isScanning.value = true
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                _isScanning.value = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) = Unit

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val host = resolved.host ?: return
                        val hostName = buildHostName(host)
                        val item = DiscoveredDevice(
                            serviceName = resolved.serviceName ?: hostName,
                            hostName = hostName,
                            ipAddress = host.hostAddress.orEmpty(),
                            port = resolved.port,
                        )
                        _discoveredDevices.value = (_discoveredDevices.value + item)
                            .distinctBy { it.ipAddress + ":" + it.port }
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _discoveredDevices.value = _discoveredDevices.value.filterNot { it.serviceName == serviceInfo.serviceName }
            }
        }
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    override fun stopDiscovery() {
        listener?.let {
            runCatching { nsdManager.stopServiceDiscovery(it) }
        }
        listener = null
        _isScanning.value = false
    }

    private fun buildHostName(address: InetAddress): String {
        val host = address.hostName ?: address.hostAddress.orEmpty()

        return if (host == address.hostAddress) {
            // it's an IP
            host
        } else {
            // it's a hostname
            if (host.endsWith(".local")) host else "$host.local"
        }
    }
}
