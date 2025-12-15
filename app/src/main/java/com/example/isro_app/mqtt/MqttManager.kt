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

class MqttManager(
    private val myId: String // <-- VERY IMPORTANT (android1 / android2)
) {

    private val brokerUri = "tcp://192.168.29.239:1883"

    private val clientId = myId
    private val gpsTopic = "gps/location"
    private val inboxTopic = "$myId/inbox"

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

                if (!::mqttClient.isInitialized) {
                    mqttClient = MqttClient(brokerUri, clientId, null)
                }

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 60
                    isAutomaticReconnect = true
                }

                mqttClient.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
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

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                mqttClient.connect(options)

                mqttClient.subscribe(gpsTopic, 1)
                mqttClient.subscribe(inboxTopic, 1)

                _connectionState.value = MqttConnectionState.Connected
                Log.d("MQTT", "Connected as $clientId")

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

            Log.d("MQTT", "GPS from ${location.deviceId}")

            _devices.value = _devices.value.toMutableMap().apply {
                put(id, location)
            }
        } catch (_: Exception) {}
    }

    fun publishGps(lat: Double, lon: Double, timestamp: String) {
        try {
            if (mqttClient.isConnected) {
                val json = JSONObject().apply {
                    put("sender_id", myId)
                    put("latitude", lat)
                    put("longitude", lon)
                    put("timestamp", timestamp)
                }

                mqttClient.publish(
                    gpsTopic,
                    MqttMessage(json.toString().toByteArray()).apply {
                        qos = 1
                    }
                )
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
            if (mqttClient.isConnected) {
                val msg = "$myId: $text"

                mqttClient.publish(
                    "$peerId/inbox",
                    MqttMessage(msg.toByteArray()).apply {
                        qos = 1
                    }
                )

                _chatMessages.value =
                    _chatMessages.value + ChatMessage("you", text)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
