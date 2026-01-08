package com.example.isro_app.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.isro_app.mqtt.MqttManager
import com.example.isro_app.mqtt.MqttConnectionState
import com.example.isro_app.mqtt.MqttSettings
import com.example.isro_app.mqtt.MqttSettingsManager
import com.example.isro_app.ui.theme.TextSecondary
import com.example.isro_app.settings.ServerSettings
import com.example.isro_app.settings.ServerSettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

//enum class ConnectionStatus {
//    Idle,
//    Testing,
//    Connected,
//    Error
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MqttSettingsScreen(
    mqttManager: MqttManager,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onAsteriskServerUpdate: (String) -> Unit
) {
    val connectionState by mqttManager.connectionState.collectAsState()
    
    // Load current settings
    val currentSettings = remember { MqttSettingsManager.loadSettings(context) }
    val currentDeviceId = remember { MqttSettingsManager.loadDeviceId(context) }
    val currentServerSettings = remember { ServerSettingsManager.loadSettings(context) }
    
    // Form state
    var deviceId by rememberSaveable { mutableStateOf(currentDeviceId) }
    var brokerUri by rememberSaveable { mutableStateOf(currentSettings.brokerUri) }
    var asteriskServerIp by rememberSaveable { mutableStateOf(currentServerSettings.asteriskServerIp) }
    
    // Validation state
    var deviceIdError by remember { mutableStateOf<String?>(null) }
    var brokerUriError by remember { mutableStateOf<String?>(null) }
    var asteriskServerError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Connection status for Asterisk
    var asteriskStatus by remember { mutableStateOf(ConnectionStatus.Idle) }
    
    // Validate device ID on change
    LaunchedEffect(deviceId) {
        deviceIdError = if (deviceId.isNotBlank() && !MqttSettingsManager.isValidDeviceId(deviceId)) {
            MqttSettingsManager.getDeviceIdErrorMessage(deviceId)
        } else {
            null
        }
    }
    
    // Validate broker URI on change
    LaunchedEffect(brokerUri) {
        brokerUriError = if (brokerUri.isNotBlank() && !MqttSettingsManager.isValidBrokerUri(brokerUri)) {
            MqttSettingsManager.getBrokerUriErrorMessage(brokerUri)
        } else {
            null
        }
    }
    
    // Validate Asterisk server IP on change
    LaunchedEffect(asteriskServerIp) {
        asteriskServerError = if (asteriskServerIp.isNotBlank() && !ServerSettingsManager.isValidAsteriskServerIp(asteriskServerIp)) {
            ServerSettingsManager.getAsteriskServerErrorMessage(asteriskServerIp)
        } else {
            null
        }
    }
    
    // Test Asterisk server connection
    fun testAsteriskServer() {
        if (asteriskServerError != null) return
        
        scope.launch {
            asteriskStatus = ConnectionStatus.Testing
            try {
                val success = withContext(Dispatchers.IO) {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(asteriskServerIp.trim(), 4569), 5000)
                        socket.close()
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (success) {
                    asteriskStatus = ConnectionStatus.Connected
                    delay(2000)
                    asteriskStatus = ConnectionStatus.Idle
                } else {
                    asteriskStatus = ConnectionStatus.Error
                    delay(2000)
                    asteriskStatus = ConnectionStatus.Idle
                }
            } catch (e: Exception) {
                asteriskStatus = ConnectionStatus.Error
                delay(2000)
                asteriskStatus = ConnectionStatus.Idle
            }
        }
    }
    
    // Connect to Asterisk server
    fun connectAsteriskServer() {
        if (asteriskServerError != null) return
        
        scope.launch {
            asteriskStatus = ConnectionStatus.Testing
            try {
                val success = withContext(Dispatchers.IO) {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(asteriskServerIp.trim(), 4569), 5000)
                        socket.close()
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (success) {
                    // Update Asterisk server IP
                    onAsteriskServerUpdate(asteriskServerIp.trim())
                    
                    // Save settings
                    val newSettings = ServerSettings(
                        attachmentServerUrl = currentServerSettings.attachmentServerUrl,
                        tileServerUrl = currentServerSettings.tileServerUrl,
                        asteriskServerIp = asteriskServerIp.trim()
                    )
                    ServerSettingsManager.saveSettings(context, newSettings)
                    
                    asteriskStatus = ConnectionStatus.Connected
                    delay(2000)
                    asteriskStatus = ConnectionStatus.Idle
                } else {
                    asteriskStatus = ConnectionStatus.Error
                    delay(2000)
                    asteriskStatus = ConnectionStatus.Idle
                }
            } catch (e: Exception) {
                asteriskStatus = ConnectionStatus.Error
                delay(2000)
                asteriskStatus = ConnectionStatus.Idle
            }
        }
    }
    
    val isValid = deviceId.isNotBlank() &&
                  MqttSettingsManager.isValidDeviceId(deviceId) &&
                  brokerUri.isNotBlank() && 
                  MqttSettingsManager.isValidBrokerUri(brokerUri) &&
                  !isSaving
    
    // Connection status text
    val connectionStatusText = when (connectionState) {
        MqttConnectionState.Idle -> "Disconnected"
        MqttConnectionState.Connecting -> "Connecting..."
        MqttConnectionState.Connected -> "Connected"
        MqttConnectionState.Error -> "Connection Error"
    }
    
    val connectionStatusColor = when (connectionState) {
        MqttConnectionState.Connected -> MaterialTheme.colorScheme.primary
        MqttConnectionState.Error -> MaterialTheme.colorScheme.error
        MqttConnectionState.Connecting -> MaterialTheme.colorScheme.tertiary
        MqttConnectionState.Idle -> TextSecondary
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("MQTT & Device Settings")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connection Status:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = connectionStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = connectionStatusColor
                        )
                    }
                }
                
                // Device ID
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Device ID") },
                    placeholder = { Text("e.g., Sathwik, Vivek") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = deviceIdError != null,
                    supportingText = {
                        if (deviceIdError != null) {
                            Text(
                                text = deviceIdError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Your device name (letters, numbers, hyphens, underscores only)")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                
                // Broker URI
                OutlinedTextField(
                    value = brokerUri,
                    onValueChange = { brokerUri = it },
                    label = { Text("Broker URI") },
                    placeholder = { Text("tcp://192.168.1.100:1883") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = brokerUriError != null,
                    supportingText = {
                        if (brokerUriError != null) {
                            Text(
                                text = brokerUriError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Format: tcp://host:port or ssl://host:port")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                
                // Asterisk Server
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Asterisk Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = asteriskServerIp,
                            onValueChange = { asteriskServerIp = it },
                            label = { Text("Server IP") },
                            placeholder = { Text("192.168.1.100") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = asteriskServerError != null,
                            supportingText = {
                                if (asteriskServerError != null) {
                                    Text(
                                        text = asteriskServerError!!,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text("Format: xxx.xxx.xxx.xxx")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                when (asteriskStatus) {
                                    ConnectionStatus.Testing -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    ConnectionStatus.Connected -> {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Connected",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    ConnectionStatus.Error -> {
                                        Icon(
                                            imageVector = Icons.Default.Error,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { testAsteriskServer() },
                                modifier = Modifier.weight(1f),
                                enabled = asteriskServerError == null && asteriskStatus != ConnectionStatus.Testing
                            ) {
                                Text("Test")
                            }
                            
                            Button(
                                onClick = { connectAsteriskServer() },
                                modifier = Modifier.weight(1f),
                                enabled = asteriskServerError == null && asteriskStatus != ConnectionStatus.Testing
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        isSaving = true
                        val newSettings = MqttSettings(
                            brokerUri = brokerUri.trim(),
                            username = "", // No authentication
                            password = "" // No authentication
                        )
                        val newDeviceId = deviceId.trim()
                        val deviceIdChanged = newDeviceId != currentDeviceId
                        
                        // Save settings
                        MqttSettingsManager.saveSettings(context, newSettings)
                        MqttSettingsManager.saveDeviceId(context, newDeviceId)
                        
                        // If device ID changed, update it (this will also reconnect)
                        if (deviceIdChanged) {
                            mqttManager.updateDeviceId(newDeviceId)
                        } else {
                            // Only reconnect if MQTT settings changed
                            mqttManager.reconnect(newSettings)
                        }
                        
                        // Reset saving state after a delay
                        scope.launch {
                            delay(2000)
                            isSaving = false
                            onDismiss()
                        }
                    }
                },
                enabled = isValid && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

