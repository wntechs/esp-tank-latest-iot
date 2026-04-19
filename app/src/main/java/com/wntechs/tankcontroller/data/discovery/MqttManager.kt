package com.wntechs.tankcontroller.data.discovery

import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.wntechs.tankcontroller.data.model.ConfigResponse
import com.wntechs.tankcontroller.data.model.MqttCredentials
import com.wntechs.tankcontroller.data.model.SensorListResponse
import com.wntechs.tankcontroller.data.model.StatusResponse
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class MqttManager {
    private var client: Mqtt3AsyncClient? = null
    private val json = Json { ignoreUnknownKeys = true }

    private val _sensorListFlow = MutableSharedFlow<SensorListResponse>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sensorListFlow = _sensorListFlow.asSharedFlow()

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

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    suspend fun connect(creds: MqttCredentials, deviceUuid: String): Boolean {
        Log.d("MqttManager", "Attempting to connect to ${creds.host}:${creds.port}...")
        
        // Use a coroutine-level timeout to ensure we don't hang forever
        val result = withTimeoutOrNull(20000) { 
            performConnect(creds, deviceUuid)
        }
        
        if (result == null) {
            Log.e("MqttManager", "Connection to ${creds.host} timed out after 20s")
            _isConnected.value = false
            return false
        }
        
        return result
    }

    private suspend fun performConnect(creds: MqttCredentials, deviceUuid: String): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            // Disconnect any existing client before creating a new one
            client?.disconnect()
            client = null

            val builder = MqttClient.builder()
                .useMqttVersion3()
                .identifier(creds.clientId)
                .serverHost(creds.host)
                .serverPort(creds.port)
                .transportConfig()
                    .mqttConnectTimeout(15, TimeUnit.SECONDS)
                    .applyTransportConfig()
                .automaticReconnectWithDefaultConfig()
                .simpleAuth()
                    .username(creds.username)
                    .password(creds.password.toByteArray())
                    .applySimpleAuth()

            // Enable SSL/TLS for port 8883 (standard MQTTS port)
            if (creds.port == 8883 || creds.port == 443) {
                Log.d("MqttManager", "Enabling SSL for port ${creds.port}")
                builder.sslWithDefaultConfig()
            }

            val newClient = builder.buildAsync()
            client = newClient

            Log.d("MqttManager", "Sending connect request for client ${creds.clientId}...")
            newClient.connectWith()
                .cleanSession(true)
                .keepAlive(30)
                .send()
                .whenComplete { ack, throwable ->
                    if (throwable == null) {
                        Log.d("MqttManager", "Connected successfully to ${creds.host}. Ack: $ack")
                        _isConnected.value = true
                        subscribeToTopics(creds, deviceUuid)
                        if (continuation.isActive) continuation.resume(true)
                    } else {
                        Log.e("MqttManager", "Connection to ${creds.host} failed: ${throwable.message}")
                        _isConnected.value = false
                        _errorFlow.tryEmit("MQTT Connection failed: ${throwable.message}")
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
        } catch (e: Exception) {
            Log.e("MqttManager", "Error during MQTT setup: ${e.message}")
            _isConnected.value = false
            if (continuation.isActive) continuation.resume(false)
        }

        continuation.invokeOnCancellation {
            Log.d("MqttManager", "Connect task cancelled.")
            client?.disconnect()
            client = null
        }
    }

    private fun subscribeToTopics(creds: MqttCredentials, deviceUuid: String) {
        val c = client ?: return
        Log.d("MqttManager", "Setting up subscriptions for $deviceUuid...")
        
        creds.topics.subscribe.forEach { topicFilter ->
            val cleanTopic = topicFilter.replace("{uuid}", deviceUuid)
            Log.d("MqttManager", "Subscribing to: $cleanTopic")
            c.subscribeWith().topicFilter(cleanTopic).callback { p ->
                val actualTopic = p.topic.toString() // Get the actual topic of the message
                val payload = p.payloadAsBytes.decodeToString()

                Log.d("MqttManager", "Raw message received on $actualTopic: $payload")

                try {
                    when {
                        actualTopic.contains("/telemetry/result") -> {
                            val jsonElement = json.parseToJsonElement(payload).jsonObject
                            val command = jsonElement["command"]?.jsonPrimitive?.content
                            if (command == "pairing/list") {
                                val response = json.decodeFromString<SensorListResponse>(payload)
                                _sensorListFlow.tryEmit(response)
                                Log.d("MqttManager", "Emitted ${response.sensors.size} sensors to flow")
                            }
                        }
                        actualTopic.contains("/telemetry") || actualTopic.contains("/state") -> {
                            if (payload.contains("water_level_percent")) {
                                val status = json.decodeFromString<StatusResponse>(payload)
                                _statusFlow.tryEmit(status)
                            } else if (payload.contains("tank_height_mm")) {
                                val config = json.decodeFromString<ConfigResponse>(payload)
                                _configFlow.tryEmit(config)
                            }
                        }
                        actualTopic.contains("/availability") -> {
                            _availabilityFlow.tryEmit(payload.lowercase() == "online")
                        }
                        actualTopic.contains("/error") -> {
                            _errorFlow.tryEmit(payload)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MqttManager", "Parse error on $actualTopic: ${e.message}")
                }
            }.send().whenComplete { subAck, subError ->
                if (subError != null) {
                    Log.e("MqttManager", "Subscription failed for $cleanTopic: ${subError.message}")
                } else {
                    Log.d("MqttManager", "Subscribed to $cleanTopic. Ack: $subAck")
                }
            }
        }
    }

    fun publish(topic: String, payload: String = "") {
        if (!_isConnected.value) {
            Log.w("MqttManager", "Skipping publish to $topic - not connected")
            return
        }
        client?.publishWith()
            ?.topic(topic)
            ?.payload(payload.toByteArray())
            ?.send()
    }

    fun disconnect() {
        Log.d("MqttManager", "Disconnecting MQTT...")
        client?.disconnect()
        client = null
        _isConnected.value = false
    }
}
