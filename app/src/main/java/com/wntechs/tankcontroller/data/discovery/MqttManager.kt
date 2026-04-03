package com.wntechs.tankcontroller.data.discovery

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.wntechs.tankcontroller.data.model.ConfigResponse
import com.wntechs.tankcontroller.data.model.MqttCredentials
import com.wntechs.tankcontroller.data.model.StatusResponse
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json

class MqttManager {
    private var client: Mqtt3AsyncClient? = null
    private val json = Json { ignoreUnknownKeys = true }

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

    fun connect(creds: MqttCredentials, deviceUuid: String) {
        Log.d("MqttManager", "Connecting to ${creds.host}:${creds.port} with TLS")
        Log.d("MqttManager", "Step 1: connect() called for $deviceUuid at ${creds.host}")
        if (client != null) {
            Log.d("MqttManager", "Existing client found, disconnecting...")
            disconnect()
        }

        client = MqttClient.builder()
            .useMqttVersion3()
            .identifier(creds.clientId)
            .serverHost(creds.host)
            .serverPort(creds.port)
            .sslWithDefaultConfig()
            .automaticReconnectWithDefaultConfig()
            .simpleAuth()
            .username(creds.username)
            .password(creds.password.toByteArray())
            .applySimpleAuth()
            .buildAsync()

        Log.d("MqttManager", "Step 2: Sending connection request...")
        client?.connectWith()?.send()?.whenComplete { _, throwable ->
            if (throwable == null) {
                Log.d("MqttManager", "Step 3: Connection SUCCESS. Proceeding to subscribe...")
                subscribeToTopics(creds, deviceUuid)
            } else {
                Log.e("MqttManager", "Step 3: Connection FAILED: ${throwable.message}")
                _errorFlow.tryEmit("MQTT Connection failed: ${throwable.message}")
            }
        }
    }

    private fun subscribeToTopics(creds: MqttCredentials, deviceUuid: String) {
        val c = client ?: run {
            Log.e("MqttManager", "Subscribe aborted: Client is null")
            return
        }

        Log.d("MqttManager", "Step 4: Initiating Subscriptions. Topic count: ${creds.topics.subscribe.size}")
        creds.topics.subscribe.forEach { topicFilter ->
            val cleanTopic = topicFilter.replace("{uuid}", deviceUuid)
            Log.d("MqttManager", "Attempting to subscribe to: $cleanTopic")
            c.subscribeWith().topicFilter(cleanTopic).callback { p ->
                val payload = p.payloadAsBytes.decodeToString()
                Log.v("MqttManager", "Incoming message on [$cleanTopic]: $payload")
                try {
                    when {
                        cleanTopic.contains("/telemetry") || cleanTopic.contains("/state") -> {
                            if (payload.contains("water_level_percent")) {
                                val status = json.decodeFromString<StatusResponse>(payload)
                                _statusFlow.tryEmit(status)
                            } else if (payload.contains("tank_height_mm")) {
                                val config = json.decodeFromString<ConfigResponse>(payload)
                                _configFlow.tryEmit(config)
                            }
                        }
                        cleanTopic.contains("/availability") -> {
                            _availabilityFlow.tryEmit(payload.lowercase() == "online")
                        }
                        cleanTopic.contains("/error") -> {
                            _errorFlow.tryEmit(payload)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MqttManager", "Error parsing MQTT payload: ${e.message}")
                }
            }.send().whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MqttManager", "Failed to subscribe to $cleanTopic: ${throwable.message}")
                } else {
                    Log.d("MqttManager", "Successfully subscribed to $cleanTopic")
                }
            }
        }
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
