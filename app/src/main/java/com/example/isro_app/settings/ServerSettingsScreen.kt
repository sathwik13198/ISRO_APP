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
import com.example.isro_app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

enum class ConnectionStatus {
    Idle,
    Testing,
    Connected,
    Error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsScreen(
    mqttManager: MqttManager,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onTileServerUpdate: (String) -> Unit,
    onAsteriskServerUpdate: (String) -> Unit
) {
    // Load current settings
    val currentSettings = remember { ServerSettingsManager.loadSettings(context) }
    
    // Form state
    var attachmentServerUrl by rememberSaveable { mutableStateOf(currentSettings.attachmentServerUrl) }
    var tileServerUrl by rememberSaveable { mutableStateOf(currentSettings.tileServerUrl) }
    var asteriskServerIp by rememberSaveable { mutableStateOf(currentSettings.asteriskServerIp) }
    
    // Validation state
    var attachmentServerError by remember { mutableStateOf<String?>(null) }
    var tileServerError by remember { mutableStateOf<String?>(null) }
    var asteriskServerError by remember { mutableStateOf<String?>(null) }
    
    // Connection status
    var attachmentStatus by remember { mutableStateOf(ConnectionStatus.Idle) }
    var tileStatus by remember { mutableStateOf(ConnectionStatus.Idle) }
    var asteriskStatus by remember { mutableStateOf(ConnectionStatus.Idle) }
    
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Validate attachment server URL on change
    LaunchedEffect(attachmentServerUrl) {
        attachmentServerError = if (attachmentServerUrl.isNotBlank() && !ServerSettingsManager.isValidAttachmentServerUrl(attachmentServerUrl)) {
            ServerSettingsManager.getAttachmentServerErrorMessage(attachmentServerUrl)
        } else {
            null
        }
    }
    
    // Validate tile server URL on change
    LaunchedEffect(tileServerUrl) {
        tileServerError = if (tileServerUrl.isNotBlank() && !ServerSettingsManager.isValidTileServerUrl(tileServerUrl)) {
            ServerSettingsManager.getTileServerErrorMessage(tileServerUrl)
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
    
    // Test attachment server connection
    fun testAttachmentServer() {
        if (attachmentServerError != null) return
        
        scope.launch {
            attachmentStatus = ConnectionStatus.Testing
            try {
                val success = withContext(Dispatchers.IO) {
                    try {
                        val url = URL(attachmentServerUrl.trim().removeSuffix("/"))
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.requestMethod = "GET"
                        connection.connect()
                        val responseCode = connection.responseCode
                        connection.disconnect()
                        responseCode in 200..499 // Accept any response (even 404 means server is reachable)
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (success) {
                    attachmentStatus = ConnectionStatus.Connected
                    delay(2000)
                    attachmentStatus = ConnectionStatus.Idle
                } else {
                    attachmentStatus = ConnectionStatus.Error
                    delay(2000)
                    attachmentStatus = ConnectionStatus.Idle
                }
            } catch (e: Exception) {
                attachmentStatus = ConnectionStatus.Error
                delay(2000)
                attachmentStatus = ConnectionStatus.Idle
            }
        }
    }
    
    // Test tile server connection
    fun testTileServer() {
        if (tileServerError != null) return
        
        scope.launch {
            tileStatus = ConnectionStatus.Testing
            try {
                val success = withContext(Dispatchers.IO) {
                    try {
                        val testUrl = tileServerUrl.trim().removeSuffix("/") + "/0/0/0.png"
                        val url = URL(testUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.requestMethod = "HEAD"
                        connection.connect()
                        val responseCode = connection.responseCode
                        connection.disconnect()
                        responseCode in 200..499
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (success) {
                    tileStatus = ConnectionStatus.Connected
                    delay(2000)
                    tileStatus = ConnectionStatus.Idle
                } else {
                    tileStatus = ConnectionStatus.Error
                    delay(2000)
                    tileStatus = ConnectionStatus.Idle
                }
            } catch (e: Exception) {
                tileStatus = ConnectionStatus.Error
                delay(2000)
                tileStatus = ConnectionStatus.Idle
            }
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
    
    // Connect to attachment server
    fun connectAttachmentServer() {
        if (attachmentServerError != null) return
        
        scope.launch {
            attachmentStatus = ConnectionStatus.Testing
            try {
                val success = withContext(Dispatchers.IO) {
                    try {
                        val url = URL(attachmentServerUrl.trim().removeSuffix("/"))
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.requestMethod = "GET"
                        connection.connect()
                        val responseCode = connection.responseCode
                        connection.disconnect()
                        responseCode in 200..499
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (success) {
                    // Update MqttManager with new attachment server URL
                    mqttManager.updateAttachmentServer(attachmentServerUrl.trim().removeSuffix("/"))
                    
                    // Save settings
                    val newSettings = ServerSettings(
                        attachmentServerUrl = attachmentServerUrl.trim(),
                        tileServerUrl = currentSettings.tileServerUrl,
                        asteriskServerIp = currentSettings.asteriskServerIp
                    )
                    ServerSettingsManager.saveSettings(context, newSettings)
                    
                    attachmentStatus = ConnectionStatus.Connected
                    delay(2000)
                    attachmentStatus = ConnectionStatus.Idle
                } else {
                    attachmentStatus = ConnectionStatus.Error
                    delay(2000)
                    attachmentStatus = ConnectionStatus.Idle
                }
            } catch (e: Exception) {
                attachmentStatus = ConnectionStatus.Error
                delay(2000)
                attachmentStatus = ConnectionStatus.Idle
            }
        }
    }
    
    // Connect to tile server
    fun connectTileServer() {
        if (tileServerError != null) return
        
        scope.launch {
            tileStatus = ConnectionStatus.Testing
            try {
                val success = withContext(Dispatchers.IO) {
                    try {
                        val testUrl = tileServerUrl.trim().removeSuffix("/") + "/0/0/0.png"
                        val url = URL(testUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.requestMethod = "HEAD"
                        connection.connect()
                        val responseCode = connection.responseCode
                        connection.disconnect()
                        responseCode in 200..499
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (success) {
                    // Update tile server URL
                    onTileServerUpdate(tileServerUrl.trim())
                    
                    // Save settings
                    val newSettings = ServerSettings(
                        attachmentServerUrl = currentSettings.attachmentServerUrl,
                        tileServerUrl = tileServerUrl.trim(),
                        asteriskServerIp = currentSettings.asteriskServerIp
                    )
                    ServerSettingsManager.saveSettings(context, newSettings)
                    
                    tileStatus = ConnectionStatus.Connected
                    delay(2000)
                    tileStatus = ConnectionStatus.Idle
                } else {
                    tileStatus = ConnectionStatus.Error
                    delay(2000)
                    tileStatus = ConnectionStatus.Idle
                }
            } catch (e: Exception) {
                tileStatus = ConnectionStatus.Error
                delay(2000)
                tileStatus = ConnectionStatus.Idle
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
                        attachmentServerUrl = currentSettings.attachmentServerUrl,
                        tileServerUrl = currentSettings.tileServerUrl,
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Server Configuration")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Attachment Server
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
                            text = "Attachment Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = attachmentServerUrl,
                            onValueChange = { attachmentServerUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("http://192.168.1.100:8090") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = attachmentServerError != null,
                            supportingText = {
                                if (attachmentServerError != null) {
                                    Text(
                                        text = attachmentServerError!!,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text("Format: http://IP:PORT")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            trailingIcon = {
                                when (attachmentStatus) {
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
                                onClick = { testAttachmentServer() },
                                modifier = Modifier.weight(1f),
                                enabled = attachmentServerError == null && attachmentStatus != ConnectionStatus.Testing
                            ) {
                                Text("Test")
                            }
                            
                            Button(
                                onClick = { connectAttachmentServer() },
                                modifier = Modifier.weight(1f),
                                enabled = attachmentServerError == null && attachmentStatus != ConnectionStatus.Testing
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                }
                
                // Tile Server
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
                            text = "Tile Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        OutlinedTextField(
                            value = tileServerUrl,
                            onValueChange = { tileServerUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("http://192.168.1.100:8080/tiles/") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = tileServerError != null,
                            supportingText = {
                                if (tileServerError != null) {
                                    Text(
                                        text = tileServerError!!,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text("Format: http://IP:PORT/tiles/")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            trailingIcon = {
                                when (tileStatus) {
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
                                onClick = { testTileServer() },
                                modifier = Modifier.weight(1f),
                                enabled = tileServerError == null && tileStatus != ConnectionStatus.Testing
                            ) {
                                Text("Test")
                            }
                            
                            Button(
                                onClick = { connectTileServer() },
                                modifier = Modifier.weight(1f),
                                enabled = tileServerError == null && tileStatus != ConnectionStatus.Testing
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                }
                
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
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

