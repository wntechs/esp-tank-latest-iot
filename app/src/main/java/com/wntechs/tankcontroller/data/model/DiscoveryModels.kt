package com.wntechs.tankcontroller.data.model

data class DiscoveredDevice(
    val serviceName: String,
    val hostName: String,
    val ipAddress: String,
    val port: Int,
) {
    val httpBaseUrl: String
        get() = "http://$ipAddress:$port/"

    val mdnsBaseUrl: String
        get() = "http://$hostName:$port/"
}
