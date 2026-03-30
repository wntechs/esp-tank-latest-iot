package com.wntechs.tankcontroller.data.discovery

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.wntechs.tankcontroller.data.model.ConfigResponse
import com.wntechs.tankcontroller.data.model.StatusResponse
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import java.util.UUID

class MqttManager {
    private var client: Mqtt3AsyncClient? = null
    private val json = Json { ignoreUnknownKeys = true }

    // /app/src/main/java/com/wntechs/tankcontroller/data/discovery/MqttManager.kt

    // Use replay = 1 so the UI gets the last known state immediately upon subscription
    private val _statusFlow =
        MutableSharedFlow<StatusResponse>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val statusFlow = _statusFlow.asSharedFlow()

    private val _configFlow =
        MutableSharedFlow<ConfigResponse>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val configFlow = _configFlow.asSharedFlow()

    private val _errorFlow =
        MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val errorFlow = _errorFlow.asSharedFlow()

    private val _availabilityFlow =
        MutableSharedFlow<Boolean>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val isDeviceOnline = _availabilityFlow.asSharedFlow()

    fun connect(brokerUrl: String, deviceId: String) {
        if (client != null) return

        client = MqttClient.builder()
            .useMqttVersion3()
            .identifier("android_${UUID.randomUUID().toString().take(5)}")
            .serverHost(brokerUrl)
            .serverPort(1883)
            .automaticReconnectWithDefaultConfig()
            .simpleAuth()
            .username("esp_wnt")
            .password("Wntechs@2026".toByteArray())
            .applySimpleAuth()
            .buildAsync()

        client?.connectWith()?.send()?.whenComplete { _, throwable ->
            if (throwable == null) {
                subscribeToTopics(deviceId) // Pass the ID here
                publish("tank/$deviceId/cmd/get_status")
                publish("tank/$deviceId/cmd/get_config")
            }
        }
    }

    private fun subscribeToTopics(deviceId: String) {
        val c = client ?: return
        val base = "tank/$deviceId"

        c.subscribeWith().topicFilter("$base/status").callback { p ->
            val status = json.decodeFromString<StatusResponse>(p.payloadAsBytes.decodeToString())
            _statusFlow.tryEmit(status)
        }.send()

        c.subscribeWith().topicFilter("$base/config").callback { p ->
            val config = json.decodeFromString<ConfigResponse>(p.payloadAsBytes.decodeToString())
            _configFlow.tryEmit(config)
        }.send()

        c.subscribeWith().topicFilter("$base/availability").callback { p ->
            _availabilityFlow.tryEmit(p.payloadAsBytes.decodeToString().lowercase() == "online")
        }.send()

        c.subscribeWith().topicFilter("$base/error").callback { p ->
            _errorFlow.tryEmit(p.payloadAsBytes.decodeToString())
        }.send()
    }

    fun publish(topic: String, payload: String = "") {
        client?.publishWith()
            ?.topic(topic)
            ?.payload(payload.toByteArray())
            ?.send()
    }

    fun disconnect() {
        client?.disconnect()
        client = null
    }
}