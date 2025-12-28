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

// -------- CALL SIGNALING --------

sealed class CallEvent {
    data class Incoming(val from: String) : CallEvent()
    data class Accepted(val from: String) : CallEvent()
    data class Rejected(val from: String) : CallEvent()
    data class Ended(val from: String) : CallEvent()
}

// -------- MQTT MANAGER --------

class MqttManager(
    var myId: String,
    private var settings: MqttSettings = MqttSettings()
) {

    private val attachmentServer = "http://10.122.97.180:8090"

    private val gpsTopic = "gps/location"
    private var inboxTopic = "$myId/inbox"

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

    private val _callEvents = MutableStateFlow<CallEvent?>(null)
    val callEvents: StateFlow<CallEvent?> = _callEvents

    // -------- DISCONNECT --------

    fun disconnect() {
        scope.launch {
            try {
                if (::mqttClient.isInitialized && mqttClient.isConnected) {
                    mqttClient.unsubscribe(gpsTopic)
                    mqttClient.unsubscribe(inboxTopic)
                    mqttClient.disconnect()
                    Log.d("MQTT", "Disconnected")
                }
            } catch (e: Exception) {
                Log.e("MQTT", "Error during disconnect", e)
            } finally {
                _connectionState.value = MqttConnectionState.Idle
            }
        }
    }

    // -------- RECONNECT --------

    fun reconnect(newSettings: MqttSettings) {
        scope.launch {
            try {
                disconnect()
                // Wait a bit for disconnect to complete
                delay(500)
                settings = newSettings
                connect()
            } catch (e: Exception) {
                Log.e("MQTT", "Reconnection failed", e)
                _connectionState.value = MqttConnectionState.Error
            }
        }
    }

    // -------- UPDATE DEVICE ID --------

    fun updateDeviceId(newDeviceId: String) {
        scope.launch {
            try {
                val oldDeviceId = myId
                
                // Publish device ID change announcement before disconnecting
                if (::mqttClient.isInitialized && mqttClient.isConnected) {
                    val changeMessage = JSONObject().apply {
                        put("type", "device_id_changed")
                        put("old_id", oldDeviceId)
                        put("new_id", newDeviceId)
                    }
                    
                    mqttClient.publish(
                        gpsTopic,
                        MqttMessage(changeMessage.toString().toByteArray()).apply { qos = 1 }
                    )
                    Log.d("MQTT", "Published device ID change: $oldDeviceId -> $newDeviceId")
                    
                    // Wait a bit to ensure message is sent
                    delay(200)
                }
                
                // Disconnect
                disconnect()
                delay(500)
                
                // Update device ID and inbox topic
                myId = newDeviceId
                inboxTopic = "$myId/inbox"
                
                // Reconnect with new device ID
                connect()
            } catch (e: Exception) {
                Log.e("MQTT", "Device ID update failed", e)
                _connectionState.value = MqttConnectionState.Error
            }
        }
    }

    // -------- CONNECT --------

    fun connect() {
        scope.launch {
            try {
                // Validate broker URI before attempting connection
                if (!MqttSettingsManager.isValidBrokerUri(settings.brokerUri)) {
                    Log.e("MQTT", "Invalid broker URI: ${settings.brokerUri}")
                    _connectionState.value = MqttConnectionState.Error
                    return@launch
                }

                _connectionState.value = MqttConnectionState.Connecting

                mqttClient = MqttClient(settings.brokerUri, myId, null)

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    isAutomaticReconnect = true
                    connectionTimeout = 10
                    keepAliveInterval = 60
                    
                    // Only set username/password if they are not empty
                    if (settings.username.isNotBlank()) {
                        userName = settings.username
                    }
                    if (settings.password.isNotBlank()) {
                        password = settings.password.toCharArray()
                    }
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
            
            // Check if this is a device ID change announcement
            if (json.optString("type") == "device_id_changed") {
                val oldId = json.optString("old_id")
                val newId = json.optString("new_id")
                
                if (oldId.isNotBlank() && newId.isNotBlank()) {
                    Log.d("MQTT", "Device ID changed: $oldId -> $newId")
                    // Remove old device ID from device list immediately
                    _devices.value = _devices.value.toMutableMap().apply {
                        remove(oldId)
                    }
                }
                return
            }
            
            // Regular GPS location message
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

    // -------- INBOX (CHAT + ATTACHMENTS + CALL SIGNALING) --------

    private fun handleInbox(payload: String) {
        try {
            val json = JSONObject(payload)

            // ===============================
            // CALL SIGNALING (MQTT)
            // ===============================
            when (json.optString("type")) {
                "CALL_REQUEST" -> {
                    val from = json.getString("from")
                    if (from != myId) {
                        Log.d("CALL", "Incoming call from $from")
                        _callEvents.value = CallEvent.Incoming(from)
                    }
                    return
                }

                "CALL_ACCEPT" -> {
                    val from = json.getString("from")
                    Log.d("CALL", "Call accepted by $from")
                    _callEvents.value = CallEvent.Accepted(from)
                    return
                }

                "CALL_REJECT" -> {
                    val from = json.getString("from")
                    Log.d("CALL", "Call rejected by $from")
                    _callEvents.value = CallEvent.Rejected(from)
                    return
                }

                "CALL_END" -> {
                    val from = json.getString("from")
                    Log.d("CALL", "Call ended by $from")
                    _callEvents.value = CallEvent.Ended(from)
                    return
                }
            }

            // ===== EXISTING ATTACHMENT LOGIC =====
            if (json.optString("type") == "attachment") {
                val sender = json.getString("sender")
                if (sender == myId) return   // ignore self echo
    
                Log.d("ATTACH", "Received attachment from $sender")
                
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
    
        // ===== EXISTING TEXT LOGIC =====
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
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) return

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
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) return

        val msg = "$myId: $text"
        mqttClient.publish(
            "$peerId/inbox",
            MqttMessage(msg.toByteArray()).apply { qos = 1 }
        )

        _chatItems.value = _chatItems.value + ChatItem.Text("you", text)
    }

    // -------- ATTACHMENT UPLOAD + MQTT --------

    private fun getFileName(uri: Uri, resolver: ContentResolver): String {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                return cursor.getString(nameIndex)
            }
        }
        return "file_${System.currentTimeMillis()}"
    }

    fun sendAttachment(
        peerId: String,
        fileUri: Uri,
        resolver: ContentResolver
    ) {
        scope.launch {
            var conn: HttpURLConnection? = null
            try {
                val uploadUrl = URL("$attachmentServer/upload")
                conn = uploadUrl.openConnection() as HttpURLConnection

                val filename = getFileName(fileUri, resolver)

                // Get file size for Content-Length
                val fileSize = resolver.openInputStream(fileUri)?.available() ?: 0

                conn.requestMethod = "POST"
                conn.setRequestProperty("X-Filename", filename)
                conn.setFixedLengthStreamingMode(fileSize)
                conn.doOutput = true

                val out = DataOutputStream(conn.outputStream)
                resolver.openInputStream(fileUri)?.use { input ->
                    input.copyTo(out)
                }
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
            } finally {
                conn?.disconnect()
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

    // -------- CALL SIGNALING SEND --------

    fun sendCallRequest(peerId: String) {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) return
        sendCallSignal(peerId, "CALL_REQUEST")
    }

    fun sendCallAccept(peerId: String) {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) return
        sendCallSignal(peerId, "CALL_ACCEPT")
    }

    fun sendCallReject(peerId: String) {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) return
        sendCallSignal(peerId, "CALL_REJECT")
    }

    fun sendCallEnd(peerId: String) {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) return
        sendCallSignal(peerId, "CALL_END")
    }

    private fun sendCallSignal(peerId: String, type: String) {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) return
        val payload = JSONObject().apply {
            put("type", type)
            put("from", myId)
            put("to", peerId)
        }
        mqttClient.publish(
            "$peerId/inbox",
            MqttMessage(payload.toString().toByteArray()).apply { qos = 1 }
        )
    }
}
