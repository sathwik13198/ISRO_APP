package com.example.isro_app.mqtt

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject

// -------- CONNECTION STATE --------

enum class MqttConnectionState {
    Idle,
    Connecting,
    Connected,
    Error
}

// -------- DATA MODELS --------

data class DeviceLocation(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val lastSeen: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val from: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

// -------- MQTT MANAGER --------

class MqttManager {

    private val brokerUri = "tcp://192.168.10.100:1883"
    private val clientId = "android_${System.currentTimeMillis()}"
    private val username = "piuser"
    private val password = "1234"

    private val gpsTopic = "gps/location"
    private val inboxTopic = "android1/inbox" // Keep original inbox topic format

    private lateinit var mqttClient: MqttClient

    private val _devices =
        MutableStateFlow<Map<String, DeviceLocation>>(emptyMap())
    val devices: StateFlow<Map<String, DeviceLocation>> = _devices

    private val _chatMessages =
        MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _connectionState =
        MutableStateFlow<MqttConnectionState>(MqttConnectionState.Idle)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState

    // -------- CONNECT --------

    fun connect() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _connectionState.value = MqttConnectionState.Connecting

                // Create MqttClient (NOT MqttAndroidClient - avoids LocalBroadcastManager crash)
                if (!::mqttClient.isInitialized) {
                    mqttClient = MqttClient(brokerUri, clientId, null)
                }

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    userName = username
                    password = this@MqttManager.password.toCharArray()
                    connectionTimeout = 10
                    keepAliveInterval = 60
                    isAutomaticReconnect = true
                }

                // Set callback before connecting
                mqttClient.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        cause?.printStackTrace()
                        _connectionState.value = MqttConnectionState.Error
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        message ?: return
                        val payload = String(message.payload)

                        when (topic) {
                            gpsTopic -> handleGps(payload)
                            inboxTopic -> handleChat(payload)
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // Message delivery complete
                    }
                })

                // Connect synchronously (runs in background coroutine)
                if (!mqttClient.isConnected) {
                    mqttClient.connect(options)
                }

                // Subscribe after successful connection
                mqttClient.subscribe(gpsTopic, 1)
                mqttClient.subscribe(inboxTopic, 1)

                _connectionState.value = MqttConnectionState.Connected
                Log.d("MQTT", "Connected")
            } catch (e: Exception) {
                Log.e("MQTT", "Connection failed", e)
                _connectionState.value = MqttConnectionState.Error
            }
        }
    }

    // -------- GPS --------

    private fun handleGps(payload: String) {
        try {
            val json = JSONObject(payload)
            val id = json.getString("sender_id")

            val location = DeviceLocation(
                deviceId = id,
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                timestamp = json.getString("timestamp")
            )

            _devices.value = _devices.value.toMutableMap().apply {
                put(id, location)
            }
        } catch (_: Exception) {}
    }

    fun publishGps(lat: Double, lon: Double, timestamp: String) {
        try {
            if (::mqttClient.isInitialized && mqttClient.isConnected) {
                val json = JSONObject().apply {
                    put("sender_id", "android1") // Use original client ID for sender
                    put("latitude", lat)
                    put("longitude", lon)
                    put("timestamp", timestamp)
                }

                val message = MqttMessage(json.toString().toByteArray())
                message.qos = 1
                message.isRetained = false

                mqttClient.publish(gpsTopic, message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // -------- CHAT --------

    private fun handleChat(payload: String) {
        val split = payload.split(":", limit = 2)
        if (split.size != 2) return

        _chatMessages.value =
            _chatMessages.value + ChatMessage(
                from = split[0],
                text = split[1].trim()
            )
    }

    fun sendChat(peerId: String, text: String) {
        try {
            if (::mqttClient.isInitialized && mqttClient.isConnected) {
                val msg = "android1: $text" // Use original client ID format
                val message = MqttMessage(msg.toByteArray())
                message.qos = 1
                message.isRetained = false

                mqttClient.publish("$peerId/inbox", message)

                _chatMessages.value =
                    _chatMessages.value + ChatMessage("you", text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
