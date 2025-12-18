package com.example.isro_app.mqtt

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

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

sealed class ChatItem {
    data class Text(
        val from: String,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : ChatItem()

    data class Attachment(
        val from: String,
        val filename: String,
        val downloadUrl: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : ChatItem()
}

// -------- MQTT MANAGER --------

class MqttManager(
    val myId: String
) {

    private val brokerUri = "tcp://192.168.29.239:1883"
    private val attachmentServer = "http://192.168.29.239:8090"

    private val gpsTopic = "gps/location"
    private val inboxTopic = "$myId/inbox"

    private lateinit var mqttClient: MqttClient

    private val scope = CoroutineScope(Dispatchers.IO)

    // Ensure UI-related state updates happen on the main thread
    private fun onMain(block: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            block()
        }
    }

    private val _devices =
        MutableStateFlow<Map<String, DeviceLocation>>(emptyMap())
    val devices: StateFlow<Map<String, DeviceLocation>> = _devices

    private val _chatItems =
        MutableStateFlow<List<ChatItem>>(emptyList())
    val chatItems: StateFlow<List<ChatItem>> = _chatItems

    private val _connectionState =
        MutableStateFlow<MqttConnectionState>(MqttConnectionState.Idle)
    val connectionState: StateFlow<MqttConnectionState> = _connectionState

    // -------- CONNECT --------

    fun connect() {
        scope.launch {
            try {
                _connectionState.value = MqttConnectionState.Connecting

                mqttClient = MqttClient(brokerUri, myId, null)

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    isAutomaticReconnect = true
                    connectionTimeout = 10
                    keepAliveInterval = 60
                }

                mqttClient.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        _connectionState.value = MqttConnectionState.Error
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (topic == null || message == null) return
                        val payload = String(message.payload)
                    
                        Log.e("MQTT-RAW", "topic=$topic payload=$payload")
                    
                        when (topic) {
                            gpsTopic -> {
                                Log.e("MQTT-GPS", payload)
                                handleGps(payload)
                            }
                            inboxTopic -> {
                                Log.e("MQTT-INBOX", payload)
                                handleInbox(payload)
                            }
                            else -> {
                                Log.e("MQTT-OTHER", "Unhandled topic: $topic")
                            }
                        }
                    }                    

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                mqttClient.connect(options)
                mqttClient.subscribe(gpsTopic, 1)
                mqttClient.subscribe(inboxTopic, 1)
                // logs
                Log.d("MQTT", "ANDROID SUBSCRIBED")
                Log.d("MQTT", "GPS TOPIC = $gpsTopic")
                Log.d("MQTT", "INBOX TOPIC = $inboxTopic")


                _connectionState.value = MqttConnectionState.Connected
                Log.d("MQTT", "Connected as $myId")

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

    // -------- INBOX (CHAT + ATTACHMENTS) --------

    private fun handleInbox(payload: String) {
        try {
            val json = JSONObject(payload)
    
            // Attachment message
            if (json.optString("type") == "attachment") {
                val sender = json.getString("sender")
                if (sender == myId) return   // ignore self echo
    
                _chatItems.value = _chatItems.value + ChatItem.Attachment(
                    from = sender,
                    filename = json.getString("filename"),
                    downloadUrl = json.getString("download_url")
                )
                return
            }
        } catch (_: Exception) {
            // Not JSON → continue as text
        }
    
        // Text message: "sender: message"
        val split = payload.split(":", limit = 2)
        if (split.size == 2) {
            val sender = split[0].trim()
            val text = split[1].trim()
    
            if (sender == myId) return   // ignore self echo
    
            _chatItems.value = _chatItems.value + ChatItem.Text(
                from = sender,
                text = text
            )
        }
    }
    

    // -------- GPS --------

    fun publishGps(lat: Double, lon: Double, timestamp: String) {
        if (!mqttClient.isConnected) return

        val json = JSONObject().apply {
            put("sender_id", myId)
            put("latitude", lat)
            put("longitude", lon)
            put("timestamp", timestamp)
        }

        mqttClient.publish(
            gpsTopic,
            MqttMessage(json.toString().toByteArray()).apply { qos = 1 }
        )
    }

    // -------- SEND CHAT --------

    fun sendChat(peerId: String, text: String) {
        if (!mqttClient.isConnected) return

        val msg = "$myId: $text"
        mqttClient.publish(
            "$peerId/inbox",
            MqttMessage(msg.toByteArray()).apply { qos = 1 }
        )

        _chatItems.value = _chatItems.value + ChatItem.Text("you", text)
    }

    // -------- ATTACHMENT UPLOAD + MQTT --------

    fun sendAttachment(
        peerId: String,
        fileUri: Uri,
        resolver: ContentResolver
    ) {
        scope.launch {
            try {
                val uploadUrl = URL("$attachmentServer/upload")
                val conn = uploadUrl.openConnection() as HttpURLConnection

                val filename =
                    fileUri.lastPathSegment ?: "file_${System.currentTimeMillis()}"

                conn.requestMethod = "POST"
                conn.setRequestProperty("X-Filename", filename)
                conn.doOutput = true

                val out = DataOutputStream(conn.outputStream)
                resolver.openInputStream(fileUri)?.copyTo(out)
                out.flush()
                out.close()

                if (conn.responseCode == 200) {
                    val response =
                        conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)

                    sendAttachmentMetadata(
                        peerId,
                        json.getString("filename"),
                        json.getString("file_id"),
                        json.getString("download_url")
                    )
                }

            } catch (e: Exception) {
                Log.e("ATTACH", "Upload failed", e)
            }
        }
    }

    private fun sendAttachmentMetadata(
        peerId: String,
        filename: String,
        fileId: String,
        downloadUrl: String
    ) {
        val payload = JSONObject().apply {
            put("type", "attachment")
            put("sender", myId)
            put("filename", filename)
            put("file_id", fileId)
            put("download_url", downloadUrl)
        }

        mqttClient.publish(
            "$peerId/inbox",
            MqttMessage(payload.toString().toByteArray()).apply { qos = 1 }
        )

        onMain {
            _chatItems.value = _chatItems.value + ChatItem.Attachment(
                from = "you",
                filename = filename,
                downloadUrl = downloadUrl
            )
        }
    }
}
