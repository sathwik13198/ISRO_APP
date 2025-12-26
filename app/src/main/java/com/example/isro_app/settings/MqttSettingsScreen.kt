package com.example.isro_app.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.isro_app.mqtt.MqttManager
import com.example.isro_app.mqtt.MqttConnectionState
import com.example.isro_app.mqtt.MqttSettings
import com.example.isro_app.mqtt.MqttSettingsManager
import com.example.isro_app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MqttSettingsScreen(
    mqttManager: MqttManager,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    val connectionState by mqttManager.connectionState.collectAsState()
    
    // Load current settings
    val currentSettings = remember { MqttSettingsManager.loadSettings(context) }
    
    // Form state
    var brokerUri by rememberSaveable { mutableStateOf(currentSettings.brokerUri) }
    
    // Validation state
    var brokerUriError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Validate broker URI on change
    LaunchedEffect(brokerUri) {
        brokerUriError = if (brokerUri.isNotBlank() && !MqttSettingsManager.isValidBrokerUri(brokerUri)) {
            MqttSettingsManager.getBrokerUriErrorMessage(brokerUri)
        } else {
            null
        }
    }
    
    val isValid = brokerUri.isNotBlank() && 
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
            Text("MQTT Broker Settings")
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
                        MqttSettingsManager.saveSettings(context, newSettings)
                        mqttManager.reconnect(newSettings)
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

