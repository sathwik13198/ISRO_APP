package com.example.isro_app

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.media.AudioManager
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.isro_app.location.LocationState
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.iax.IaxManager
import com.example.isro_app.call.CallController
import com.example.isro_app.ui.theme.Divider
import com.example.isro_app.ui.theme.ISRO_APPTheme
import com.example.isro_app.ui.theme.PrimaryBlue
import com.example.isro_app.ui.theme.Surface
import com.example.isro_app.ui.theme.SurfaceMuted
import com.example.isro_app.ui.theme.TextPrimary
import com.example.isro_app.ui.theme.TextSecondary
import com.example.isro_app.mqtt.MqttManager
import com.example.isro_app.mqtt.MqttConnectionState
import com.example.isro_app.mqtt.CallEvent
import com.example.isro_app.settings.MqttSettingsScreen
import com.example.isro_app.settings.ServerSettingsScreen
import com.example.isro_app.settings.ServerSettingsManager
import com.example.isro_app.MapDevice
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {
    private lateinit var iaxManager: IaxManager
    lateinit var callController: CallController
    
    fun reconnectIax(newIp: String) {
        try {
            if (::iaxManager.isInitialized) {
                iaxManager.hangup()
            }
            iaxManager = IaxManager(this, newIp)
            iaxManager.connect()
            callController = CallController(
                context = this,
                mqtt = (application as MyApplication).mqttManager,
                iax = iaxManager
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to reconnect IAX", e)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    
        val config = org.osmdroid.config.Configuration.getInstance()
        config.userAgentValue = packageName
        config.osmdroidBasePath = filesDir
        config.osmdroidTileCache = File(filesDir, "osmdroid")
    
        val mqttManager = (application as MyApplication).mqttManager
        val serverSettings = ServerSettingsManager.loadSettings(this)
        iaxManager = IaxManager(this, serverSettings.asteriskServerIp)
        iaxManager.connect()

        callController = CallController(
            context = this,
            mqtt = mqttManager,
            iax = iaxManager
        )

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            101
        )

    
        enableEdgeToEdge()
        setContent {
            ISRO_APPTheme {
                IsroApp(
                    mqttManager = mqttManager,
                    callController = callController
                )
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted - audio will start only after CALL_ACCEPT
        } else {
            Toast.makeText(
                this,
                "Microphone permission required for calling",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

private enum class WindowSize { Compact, Medium, Expanded }

@Composable
private fun rememberWindowSize(): WindowSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp <= 600 -> WindowSize.Compact
        widthDp <= 840 -> WindowSize.Medium
        else -> WindowSize.Expanded
    }
}

private enum class DeviceStatus { Online, Offline }
private enum class DeviceSort { NAME, STATUS }
private enum class MessageOwner { Local, Remote }
private enum class DeliveryState { Pending, Delivered, Failed }

private data class Device(
    val clientId: String,
    val displayName: String,
    val ip: String,
    val status: DeviceStatus,
    val lastSeen: Long,
    val lat: Double,
    val lon: Double
)

private data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val uri: android.net.Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String
)

private data class Message(
    val id: String = UUID.randomUUID().toString(),
    val from: String,
    val to: String,
    val text: String?,
    val attachment: Attachment? = null,
    val timestamp: Long,
    val owner: MessageOwner,
    val state: DeliveryState = DeliveryState.Delivered
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IsroApp(
    mqttManager: MqttManager,
    callController: CallController
) {
    val locationViewModel: LocationViewModel = viewModel()
    val locationState by locationViewModel.location.collectAsState()
    val context = LocalContext.current
    val windowSize = rememberWindowSize()
    
    // Device IP state
    var deviceIp by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        deviceIp = getLocalIpAddress()
    }
    
    // Server settings state
    var tileServerUrl by remember { mutableStateOf(ServerSettingsManager.loadSettings(context).tileServerUrl) }
    var asteriskServerIp by remember { mutableStateOf(ServerSettingsManager.loadSettings(context).asteriskServerIp) }

    // Get MQTT connection state from app-wide manager
    val mqttState by mqttManager.connectionState.collectAsState()

    // Show toast if MQTT connection fails
    LaunchedEffect(mqttState) {
        if (mqttState == MqttConnectionState.Error) {
            Toast.makeText(
                context,
                "MQTT server unreachable. Running in offline mode.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Get devices from MQTT and convert to Device list (with safe initial value)
    val deviceLocations by mqttManager.devices.collectAsState(initial = emptyMap())
    val devices = remember(deviceLocations) {
        deviceLocations.values.map { location ->
            Device(
                clientId = location.deviceId,
                displayName = location.deviceId, // Use deviceId as display name
                ip = "192.168.10.${location.deviceId.hashCode() % 255}", // Generate IP from deviceId
                status = if (System.currentTimeMillis() - location.lastSeen < 5 * 60 * 1000) {
                    DeviceStatus.Online
                } else {
                    DeviceStatus.Offline
                },
                lastSeen = location.lastSeen,
                lat = location.latitude,
                lon = location.longitude
            )
        }
    }

    val myDeviceId = mqttManager.myId

    var isMapFullscreen by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showServerSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    
    var selectedDeviceId by rememberSaveable { 
        mutableStateOf("")
    }

    // Call event handling
   // Call event handling - BOTH MQTT and IAX
    val callEvent by mqttManager.callEvents.collectAsState()
    var incomingCallFrom by remember { mutableStateOf<String?>(null) }
    var activeCallPeerId by remember { mutableStateOf<String?>(null) }
    var incomingIaxCallInfo by remember { mutableStateOf<Pair<String, Int>?>(null) }

    // Setup IAX incoming call listener
    LaunchedEffect(callController) {
        callController.onIncomingIaxCall = { peerId, callNumber ->
            incomingIaxCallInfo = Pair(peerId, callNumber)
        }
    }

    LaunchedEffect(callEvent) {
        when (val event = callEvent) {
            is CallEvent.Incoming -> incomingCallFrom = event.from
            is CallEvent.Rejected -> {
                incomingCallFrom = null
                // Show toast for rejected call
                Toast.makeText(context, "Call rejected by ${event.from}", Toast.LENGTH_SHORT).show()
            }
            is CallEvent.Ended -> {
                callController.onCallEndedViaMqtt()
                incomingCallFrom = null
                activeCallPeerId = null
            }
            is CallEvent.Accepted -> {
                callController.onCallAcceptedViaMqtt(event.from)
                incomingCallFrom = null
                activeCallPeerId = event.from
            }
            null -> {}
        }
    }

    // Track IAX call state to update UI
    LaunchedEffect(Unit) {
        while (true) {
            delay(500) // Check every 500ms
            val currentState = callController.getCallState()
            if (currentState == com.example.iax.IaxManager.CallState.IDLE && activeCallPeerId != null) {
                // Call ended - clear UI
                activeCallPeerId = null
            }
        }
    }
    
    // Initialize selectedDeviceId when devices become available
    LaunchedEffect(devices) {
        if (selectedDeviceId.isBlank() && devices.isNotEmpty()) {
            selectedDeviceId = devices.first().clientId
        }
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortBy by rememberSaveable { mutableStateOf(DeviceSort.NAME) }
    var isListCollapsed by rememberSaveable { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                locationViewModel.start()
            }
        }

    // ✅ FIX 1: Move attachments BEFORE filePickerLauncher (critical order)
    val attachments = remember { mutableStateMapOf<String, SnapshotStateList<Attachment>>() }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            val resolver = context.contentResolver
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val cursor = resolver.query(uri, null, null, null, null)
            cursor?.moveToFirst()

            val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME) ?: -1
            val sizeIndex = cursor?.getColumnIndex(OpenableColumns.SIZE) ?: -1

            val name = if (nameIndex >= 0) cursor?.getString(nameIndex) ?: "file" else "file"
            val size = if (sizeIndex >= 0) cursor?.getLong(sizeIndex) ?: 0L else 0L
            val type = resolver.getType(uri) ?: "application/octet-stream"

            cursor?.close()

            attachments
                .getOrPut(selectedDeviceId) { mutableStateListOf() }
                .add(
                    Attachment(
                        uri = uri,
                        name = name,
                        sizeBytes = size,
                        mimeType = type
                    )
                )
        }

    // ✅ FIX 2: Create pickFile lambda to pass to child composables
    val pickFile: () -> Unit = {
        filePickerLauncher.launch(arrayOf("*/*"))
    }

    LaunchedEffect(Unit) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationViewModel.start()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Publish GPS when location updates
    LaunchedEffect(locationState.hasFix, locationState.latitude, locationState.longitude, locationState.timestamp) {
        if (locationState.hasFix && locationState.timestamp.isNotBlank()) {
            mqttManager.publishGps(
                locationState.latitude,
                locationState.longitude,
                locationState.timestamp
            )
        }
    }

    // Initialize message lists for all devices
    val messagesPerDevice = remember {
        mutableStateMapOf<String, androidx.compose.runtime.snapshots.SnapshotStateList<Message>>()
    }

    // Initialize message lists for all devices
    LaunchedEffect(devices) {
        devices.forEach { device ->
            if (!messagesPerDevice.containsKey(device.clientId)) {
                messagesPerDevice[device.clientId] = mutableStateListOf()
            }
        }
    }

    // Collect incoming MQTT chat items (text + attachments)
    val incomingChatItems by mqttManager.chatItems.collectAsState()

    // Bridge MQTT chat items into per-device UI message lists
    LaunchedEffect(incomingChatItems) {
        incomingChatItems.forEach { item ->
            when (item) {
                is com.example.isro_app.mqtt.ChatItem.Text -> {
                    val from = item.from
                    val list =
                        messagesPerDevice.getOrPut(from) { mutableStateListOf() }

                    // avoid duplicates
                    if (list.none { it.timestamp == item.timestamp }) {
                        list.add(
                            Message(
                                from = from,
                                to = myDeviceId,
                                text = item.text,
                                timestamp = item.timestamp,
                                owner = MessageOwner.Remote
                            )
                        )
                    }
                }

                is com.example.isro_app.mqtt.ChatItem.Attachment -> {
                    val from = item.from
                    val list =
                        messagesPerDevice.getOrPut(from) { mutableStateListOf() }

                    if (list.none { it.timestamp == item.timestamp }) {
                        list.add(
                            Message(
                                from = from,
                                to = myDeviceId,
                                text = null,
                                attachment = Attachment(
                                    uri = android.net.Uri.parse(item.downloadUrl),
                                    name = item.filename,
                                    sizeBytes = 0,
                                    mimeType = "application/octet-stream"
                                ),
                                timestamp = item.timestamp,
                                owner = MessageOwner.Remote
                            )
                        )
                    }
                }
            }
        }
    }

    val drafts = remember { mutableStateMapOf<String, String>() }

    val filtered = remember(searchQuery, sortBy, devices) {
        devices
            .filter { it.displayName.contains(searchQuery, ignoreCase = true) || it.ip.contains(searchQuery, true) }
            .sortedWith(
                when (sortBy) {
                    DeviceSort.NAME -> compareBy { it.displayName.lowercase() }
                    DeviceSort.STATUS -> compareBy<Device> { it.status == DeviceStatus.Online }.reversed()
                }
            )
    }

    val selectedDevice = filtered.find { it.clientId == selectedDeviceId } ?: filtered.firstOrNull()
    if (selectedDevice != null && selectedDevice.clientId != selectedDeviceId) {
        selectedDeviceId = selectedDevice.clientId
    }

    val sendMessage: (String, String?, Attachment?) -> Unit = sendMessage@{ text, _, attachment ->
        val deviceId = selectedDeviceId

        if (text.isBlank() && attachment == null) {
            return@sendMessage
        }

        val msgList = messagesPerDevice.getOrPut(deviceId) { mutableStateListOf() }

        // Create single message with both text and attachment if present
        val pending = Message(
            from = "you",
            to = deviceId,
            text = text.ifBlank { null },
            attachment = attachment,
            timestamp = System.currentTimeMillis(),
            owner = MessageOwner.Local,
            state = DeliveryState.Pending
        )
        msgList.add(pending)

        // Handle attachment upload
        if (attachment != null) {
            // Send attachment via HTTP + MQTT
            mqttManager.sendAttachment(
                peerId = deviceId,
                fileUri = attachment.uri,
                resolver = context.contentResolver
            )
        }

        // Handle text message
        if (text.isNotBlank()) {
            // Send via MQTT
            mqttManager.sendChat(deviceId, text)
        }
        
        // Update to delivered after a short delay
        scope.launch {
            delay(600)
            val idx = msgList.indexOfFirst { it.id == pending.id }
            if (idx >= 0) {
                msgList[idx] = msgList[idx].copy(state = DeliveryState.Delivered)
            }
        }
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = windowSize == WindowSize.Compact,
        drawerContent = {
            if (windowSize == WindowSize.Compact) {
                DrawerContent(
                    devices = filtered,
                    selectedDeviceId = selectedDeviceId,
                    onSelect = {
                        selectedDeviceId = it
                        scope.launch { drawerState.close() }
                    },
                    searchQuery = searchQuery,
                    onSearch = { searchQuery = it },
                    sortBy = sortBy,
                    onSortChange = { sortBy = it },
                    coordinatesCard = {
                        selectedDevice?.let {
                            CoordinatesCard(device = it)
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ISRO_APP",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                text = deviceIp?.let { "Device IP: $it" } ?: "Network: detecting…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    },
                    navigationIcon = {
                        val isDrawerOpen = drawerState.currentValue == DrawerValue.Open
                        IconButton(
                            onClick = {
                                if (windowSize == WindowSize.Compact) {
                                    scope.launch {
                                        if (isDrawerOpen) {
                                            drawerState.close()
                                        } else {
                                            drawerState.open()
                                        }
                                    }
                                } else {
                                    isListCollapsed = !isListCollapsed
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (windowSize == WindowSize.Compact && isDrawerOpen) {
                                    Icons.Default.Close
                                } else {
                                    Icons.Default.Menu
                                },
                                contentDescription = if (windowSize == WindowSize.Compact && isDrawerOpen) {
                                    "Close menu"
                                } else {
                                    "Open menu"
                                }
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* refresh hook */ }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "MQTT Settings")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("MQTT") },
                                    onClick = {
                                        showMenu = false
                                        showSettingsDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Configure Server") },
                                    onClick = {
                                        showMenu = false
                                        showServerSettingsDialog = true
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (isMapFullscreen) {
                // 🔥 FULLSCREEN MAP ONLY
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                            OfflineMapView(
                        devices = filtered.map {
                            MapDevice(it.clientId, it.lat, it.lon)
                        },
                        currentLocation = locationState,
                        myDeviceId = myDeviceId,
                        tileServerUrl = tileServerUrl,
                        modifier = Modifier.fillMaxSize()
                    )

                    // ⬅ EXIT FULLSCREEN
                    IconButton(
                        onClick = { isMapFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Text("⬅", color = Color.White)
                    }

                    if (mqttState == MqttConnectionState.Connecting) {
                        MqttConnectingOverlay()
                    }
                }
            } else {
                // ✅ EXISTING UI (chat + map)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (windowSize) {
                        WindowSize.Compact -> MobileLayout(
                            devices = filtered,
                            selectedDevice = selectedDevice,
                            messages = messagesPerDevice[selectedDeviceId].orEmpty(),
                            onMessageSend = { text, attachment ->
                                sendMessage(text, null, attachment)
                                drafts[selectedDeviceId] = ""
                                attachments.getOrPut(selectedDeviceId) { mutableStateListOf() }.clear()
                            },
                            draft = drafts[selectedDeviceId].orEmpty(),
                            onDraftChange = { drafts[selectedDeviceId] = it },
                            selectedAttachments = attachments[selectedDeviceId].orEmpty(),
                            onAddAttachment = {
                                val list = attachments.getOrPut(selectedDeviceId) { mutableStateListOf() }
                                list.add(it)
                            },
                            onRemoveAttachment = { att ->
                                attachments[selectedDeviceId]?.remove(att)
                            },
                            onSelectDevice = {
                                selectedDeviceId = it
                                scope.launch { drawerState.close() }
                            },
                            locationState = locationState,
                            myDeviceId = myDeviceId,
                            tileServerUrl = tileServerUrl,
                            onFullMap = { isMapFullscreen = true },
                            onPickFile = pickFile,
                            callController = callController,
                            onStartCall = { peerId ->
                                callController.startOutgoingCall(peerId)
                                activeCallPeerId = peerId
                            }
                        )

                        WindowSize.Medium -> MediumLayout(
                            devices = filtered,
                            selectedDeviceId = selectedDeviceId,
                            onSelectDevice = { selectedDeviceId = it },
                            isCollapsed = isListCollapsed,
                            onCollapseToggle = { isListCollapsed = !isListCollapsed },
                            searchQuery = searchQuery,
                            onSearch = { searchQuery = it },
                            sortBy = sortBy,
                            onSortChange = { sortBy = it },
                            selectedDevice = selectedDevice,
                            messages = messagesPerDevice[selectedDeviceId].orEmpty(),
                            draft = drafts[selectedDeviceId].orEmpty(),
                            onDraftChange = { drafts[selectedDeviceId] = it },
                            attachments = attachments[selectedDeviceId].orEmpty(),
                            onAddAttachment = { att ->
                                val list = attachments.getOrPut(selectedDeviceId) { mutableStateListOf() }
                                list.add(att)
                            },
                            onRemoveAttachment = { att -> attachments[selectedDeviceId]?.remove(att) },
                            onSend = { text, attachment ->
                                sendMessage(text, null, attachment)
                                drafts[selectedDeviceId] = ""
                                attachments.getOrPut(selectedDeviceId) { mutableStateListOf() }.clear()
                            },
                            locationState = locationState,
                            myDeviceId = myDeviceId,
                            tileServerUrl = tileServerUrl,
                            onFullMap = { isMapFullscreen = true },
                            onPickFile = pickFile,
                            callController = callController,
                            onStartCall = { peerId ->
                                callController.startOutgoingCall(peerId)
                                activeCallPeerId = peerId
                            }
                        )

                        WindowSize.Expanded -> ExpandedLayout(
                            devices = filtered,
                            selectedDeviceId = selectedDeviceId,
                            onSelectDevice = { selectedDeviceId = it },
                            searchQuery = searchQuery,
                            onSearch = { searchQuery = it },
                            sortBy = sortBy,
                            onSortChange = { sortBy = it },
                            selectedDevice = selectedDevice,
                            messages = messagesPerDevice[selectedDeviceId].orEmpty(),
                            draft = drafts[selectedDeviceId].orEmpty(),
                            onDraftChange = { drafts[selectedDeviceId] = it },
                            attachments = attachments[selectedDeviceId].orEmpty(),
                            onAddAttachment = { att ->
                                val list = attachments.getOrPut(selectedDeviceId) { mutableStateListOf() }
                                list.add(att)
                            },
                            onRemoveAttachment = { att -> attachments[selectedDeviceId]?.remove(att) },
                            onSend = { text, attachment ->
                                sendMessage(text, null, attachment)
                                drafts[selectedDeviceId] = ""
                                attachments.getOrPut(selectedDeviceId) { mutableStateListOf() }.clear()
                            },
                            locationState = locationState,
                            myDeviceId = myDeviceId,
                            tileServerUrl = tileServerUrl,
                            onFullMap = { isMapFullscreen = true },
                            onPickFile = pickFile,
                            callController = callController,
                            onStartCall = { peerId ->
                                callController.startOutgoingCall(peerId)
                                activeCallPeerId = peerId
                            }
                        )
                    }

                    // Show MQTT connecting overlay
                    if (mqttState == MqttConnectionState.Connecting) {
                        MqttConnectingOverlay()
                    }
                }
            }
        }

        // 🔔 Incoming call dialog
    // 🔔 Incoming call dialog (MQTT signaling)
    incomingCallFrom?.let { caller ->
        AlertDialog(
            onDismissRequest = {
                callController.rejectCall()
                incomingCallFrom = null
            },
            title = { Text("Incoming Call (MQTT)") },
            text = { Text("Call request from $caller") },
            confirmButton = {
                TextButton(onClick = {
                    // For MQTT calls, we need to start the IAX call
                    callController.startOutgoingCall(caller)
                    incomingCallFrom = null
                    activeCallPeerId = caller
                }) { Text("Accept & Call") }
            },
            dismissButton = {
                TextButton(onClick = {
                    callController.rejectCall()
                    incomingCallFrom = null
                }) { Text("Reject") }
            }
        )
    }

    // 🔔 Incoming IAX call dialog (actual voice call)
    incomingIaxCallInfo?.let { (caller, callNumber) ->
        AlertDialog(
            onDismissRequest = {
                callController.rejectCall()
                incomingIaxCallInfo = null
            },
            title = { Text("Incoming Voice Call") },
            text = { Text("Voice call from $caller\nTap accept to answer") },
            confirmButton = {
                TextButton(onClick = {
                    callController.acceptIncomingCall()
                    incomingIaxCallInfo = null
                    activeCallPeerId = caller
                }) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = {
                    callController.rejectCall()
                    incomingIaxCallInfo = null
                }) { Text("Reject") }
            }
        )
    }
        // 📞 Active call interface
        activeCallPeerId?.let { peerId ->
            if (callController.isInCall()) {
                ActiveCallInterface(
                    peerId = peerId,
                    onEndCall = {
                        // End the current call via controller and clear UI state
                        callController.endCall()
                        activeCallPeerId = null
                    }
                )
            }
        }

        // ⚙️ MQTT Settings Dialog
        if (showSettingsDialog) {
            MqttSettingsScreen(
                mqttManager = mqttManager,
                context = context,
                onDismiss = { showSettingsDialog = false }
            )
        }
        
        // ⚙️ Server Settings Dialog
        if (showServerSettingsDialog) {
            ServerSettingsScreen(
                mqttManager = mqttManager,
                context = context,
                onDismiss = { showServerSettingsDialog = false },
                onTileServerUpdate = { newUrl ->
                    tileServerUrl = newUrl
                },
                onAsteriskServerUpdate = { newIp ->
                    asteriskServerIp = newIp
                    // Reconnect IAX with new IP
                    (context as? MainActivity)?.let { activity ->
                        activity.reconnectIax(newIp)
                    }
                }
            )
        }
    }
}

@Composable
private fun DrawerContent(
    devices: List<Device>,
    selectedDeviceId: String,
    onSelect: (String) -> Unit,
    searchQuery: String,
    onSearch: (String) -> Unit,
    sortBy: DeviceSort,
    onSortChange: (DeviceSort) -> Unit,
    coordinatesCard: @Composable () -> Unit
) {
    ModalDrawerSheet {
        DrawerHeader(
            searchQuery = searchQuery,
            onSearch = onSearch,
            sortBy = sortBy,
            onSortChange = onSortChange
        )
        DrawerDeviceList(
            devices = devices,
            selectedDeviceId = selectedDeviceId,
            onSelect = onSelect
        )
        Divider()
        coordinatesCard()
    }
}

@Composable
private fun DrawerHeader(
    searchQuery: String,
    onSearch: (String) -> Unit,
    sortBy: DeviceSort,
    onSortChange: (DeviceSort) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Devices on LAN", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearch,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            placeholder = { Text("Search name or IP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { onSortChange(DeviceSort.NAME) },
                label = { Text("Name") },
                leadingIcon = {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null
                    )
                },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = if (sortBy == DeviceSort.NAME) PrimaryBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                )
            )
            AssistChip(
                onClick = { onSortChange(DeviceSort.STATUS) },
                label = { Text("Status") },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = if (sortBy == DeviceSort.STATUS) PrimaryBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

@Composable
private fun DrawerDeviceList(
    devices: List<Device>,
    selectedDeviceId: String,
    onSelect: (String) -> Unit
) {
    if (devices.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No devices found on this network", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { /* refresh */ }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Refresh")
            }
        }
        return
    }
    LazyColumn {
        items(devices, key = { it.clientId }) { device ->
            NavigationDrawerItem(
                label = {
                    Column {
                        Text(device.displayName, fontWeight = FontWeight.SemiBold)
                        Text("${device.ip} • ${formatRelativeTime(device.lastSeen)}", style = MaterialTheme.typography.bodyMedium)
                    }
                },
                selected = device.clientId == selectedDeviceId,
                onClick = { onSelect(device.clientId) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                badge = {
                    StatusDot(device.status)
                }
            )
        }
    }
}

@Composable
private fun MobileLayout(
    devices: List<Device>,
    selectedDevice: Device?,
    messages: List<Message>,
    onMessageSend: (String, Attachment?) -> Unit,
    draft: String,
    onDraftChange: (String) -> Unit,
    selectedAttachments: List<Attachment>,
    onAddAttachment: (Attachment) -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    onSelectDevice: (String) -> Unit,
    locationState: LocationState,
    myDeviceId: String,
    tileServerUrl: String,
    onFullMap: () -> Unit,
    onPickFile: () -> Unit,
    callController: CallController,
    onStartCall: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceMuted),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MapCard(
            devices = devices,
            locationState = locationState,
            myDeviceId = myDeviceId,
            tileServerUrl = tileServerUrl,
            onFullScreen = onFullMap
        )
        ChatPane(
            device = selectedDevice,
            messages = messages,
            draft = draft,
            onDraftChange = onDraftChange,
            attachments = selectedAttachments,
            onAddAttachment = onAddAttachment,
            onRemoveAttachment = onRemoveAttachment,
            onSend = onMessageSend,
            onPickFile = onPickFile,
            callController = callController,
            onStartCall = onStartCall
        )
    }
}

@Composable
private fun MediumLayout(
    devices: List<Device>,
    selectedDeviceId: String,
    onSelectDevice: (String) -> Unit,
    isCollapsed: Boolean,
    onCollapseToggle: () -> Unit,
    searchQuery: String,
    onSearch: (String) -> Unit,
    sortBy: DeviceSort,
    onSortChange: (DeviceSort) -> Unit,
    selectedDevice: Device?,
    messages: List<Message>,
    draft: String,
    onDraftChange: (String) -> Unit,
    attachments: List<Attachment>,
    onAddAttachment: (Attachment) -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    onSend: (String, Attachment?) -> Unit,
    locationState: LocationState,
    myDeviceId: String,
    tileServerUrl: String,
    onFullMap: () -> Unit,
    onPickFile: () -> Unit,
    callController: CallController,
    onStartCall: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceMuted)
    ) {
        if (!isCollapsed) {
            DevicePanel(
                devices = devices,
                selectedDeviceId = selectedDeviceId,
                onSelect = onSelectDevice,
                searchQuery = searchQuery,
                onSearch = onSearch,
                sortBy = sortBy,
                onSortChange = onSortChange,
                coordinatesCard = {
                    selectedDevice?.let { CoordinatesCard(it) }
                },
                modifier = Modifier.width(320.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MapCard(
                devices = devices,
                locationState = locationState,
                myDeviceId = myDeviceId,
                tileServerUrl = tileServerUrl,
                onFullScreen = onFullMap
            )
            ChatPane(
                device = selectedDevice,
                messages = messages,
                draft = draft,
                onDraftChange = onDraftChange,
                attachments = attachments,
                onAddAttachment = onAddAttachment,
                onRemoveAttachment = onRemoveAttachment,
                onSend = onSend,
                onPickFile = onPickFile,
                callController = callController,
                onStartCall = onStartCall
            )
        }
    }
}

@Composable
private fun ExpandedLayout(
    devices: List<Device>,
    selectedDeviceId: String,
    onSelectDevice: (String) -> Unit,
    searchQuery: String,
    onSearch: (String) -> Unit,
    sortBy: DeviceSort,
    onSortChange: (DeviceSort) -> Unit,
    selectedDevice: Device?,
    messages: List<Message>,
    draft: String,
    onDraftChange: (String) -> Unit,
    attachments: List<Attachment>,
    onAddAttachment: (Attachment) -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    onSend: (String, Attachment?) -> Unit,
    locationState: LocationState,
    myDeviceId: String,
    tileServerUrl: String,
    onFullMap: () -> Unit,
    onPickFile: () -> Unit,
    callController: CallController,
    onStartCall: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceMuted)
    ) {
        DevicePanel(
            devices = devices,
            selectedDeviceId = selectedDeviceId,
            onSelect = onSelectDevice,
            searchQuery = searchQuery,
            onSearch = onSearch,
            sortBy = sortBy,
            onSortChange = onSortChange,
            coordinatesCard = {
                selectedDevice?.let { CoordinatesCard(it) }
            },
            modifier = Modifier.width(320.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MapCard(
                devices = devices,
                locationState = locationState,
                myDeviceId = myDeviceId,
                tileServerUrl = tileServerUrl,
                onFullScreen = onFullMap
            )
        }
        ChatPane(
            device = selectedDevice,
            messages = messages,
            draft = draft,
            onDraftChange = onDraftChange,
            attachments = attachments,
            onAddAttachment = onAddAttachment,
            onRemoveAttachment = onRemoveAttachment,
            onSend = onSend,
            onPickFile = onPickFile,
            callController = callController,
            onStartCall = onStartCall,
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun DevicePanel(
    devices: List<Device>,
    selectedDeviceId: String,
    onSelect: (String) -> Unit,
    searchQuery: String,
    onSearch: (String) -> Unit,
    sortBy: DeviceSort,
    onSortChange: (DeviceSort) -> Unit,
    coordinatesCard: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        DrawerHeader(searchQuery, onSearch, sortBy, onSortChange)
        Divider()
        DrawerDeviceList(
            devices = devices,
            selectedDeviceId = selectedDeviceId,
            onSelect = onSelect
        )
        Divider()
        Box(modifier = Modifier.padding(16.dp)) {
            coordinatesCard()
        }
    }
}

@Composable
private fun CoordinatesCard(device: Device) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Last update: ${formatTime(device.lastSeen)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val clipboard: ClipboardManager = LocalClipboardManager.current
                OutlinedButton(onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString("${device.lat}, ${device.lon}"))
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy coordinates")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }
                OutlinedButton(onClick = { /* open maps */ }) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Open in maps")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open in Maps")
                }
            }
        }
    }
}

@Composable
private fun MapCard(
    devices: List<Device>,
    locationState: LocationState,
    myDeviceId: String,
    tileServerUrl: String,
    onFullScreen: () -> Unit
){
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)   // ⬅ less padding
            .height(360.dp),              // ⬅ bigger map
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 🔥 REAL OFFLINE MAP
            OfflineMapView(
                devices = devices.map { device ->
                    MapDevice(
                        id = device.clientId,
                        latitude = device.lat,
                        longitude = device.lon
                    )
                },
                currentLocation = locationState,
                myDeviceId = myDeviceId,
                tileServerUrl = tileServerUrl,
                modifier = Modifier.fillMaxSize()
            )

            // 📍 Coordinate overlay (top-left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (locationState.hasFix)
                        "Lat: ${"%.5f".format(locationState.latitude)}\n" +
                                "Lon: ${"%.5f".format(locationState.longitude)}"
                    else
                        "Waiting for GPS…",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )

            }

            // ⛶ Fullscreen button
            IconButton(
                onClick = onFullScreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen map"
                )
            }
        }
    }
}



@Composable
private fun ChatPane(
    device: Device?,
    messages: List<Message>,
    draft: String,
    onDraftChange: (String) -> Unit,
    attachments: List<Attachment>,
    onAddAttachment: (Attachment) -> Unit,
    onRemoveAttachment: (Attachment) -> Unit,
    onSend: (String, Attachment?) -> Unit,
    onPickFile: () -> Unit,
    callController: CallController,
    onStartCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column {
                    Text(device?.displayName ?: "No device selected", style = MaterialTheme.typography.headlineSmall)
                    if (device != null) {
                        Text(
                            "Last seen ${formatRelativeTime(device.lastSeen)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                device?.let {
                    IconButton(
                        onClick = {
                            // Delegate call handling to parent so it can update shared call UI state
                            onStartCall(it.clientId)
                        },
                        enabled = !callController.isInCall() && !callController.hasIncomingCall()
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call")
                    }
                }
            }
            device?.let { StatusDot(it.status) }
        }
        Divider()
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth(),
            state = listState
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (attachments.isNotEmpty()) {
            AttachmentPreviewRow(attachments = attachments, onRemove = onRemoveAttachment)
        }
        ChatInput(
            value = draft,
            onValueChange = onDraftChange,
            onAttach = onPickFile,
            onSend = {
                onSend(draft, attachments.firstOrNull())
            },
            canSend = draft.isNotBlank() || attachments.isNotEmpty()
        )
    }
}

@Composable
private fun ChatBubble(message: Message) {
    val alignEnd = message.owner == MessageOwner.Local
    val bubbleColor = if (alignEnd) PrimaryBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(bubbleColor, shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            message.text?.let { Text(it, color = TextPrimary) }
            message.attachment?.let {
                Spacer(modifier = Modifier.height(6.dp))
                AttachmentChip(it)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(message.timestamp), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                when (message.state) {
                    DeliveryState.Pending -> Text("Sending…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    DeliveryState.Delivered -> Text("✓", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    DeliveryState.Failed -> Text("Retry", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun AttachmentChip(attachment: Attachment) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable {
                // Only download if URI is HTTP/HTTPS (received attachments)
                val uriString = attachment.uri.toString()
                if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                    com.example.isro_app.mqtt.downloadAttachment(
                        context = context,
                        url = uriString,
                        filename = attachment.name
                    )
                }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Attachment, contentDescription = null, tint = PrimaryBlue)
        Text("${attachment.name} • ${formatSize(attachment.sizeBytes)}", color = TextPrimary)
    }
}

@Composable
private fun AttachmentPreviewRow(
    attachments: List<Attachment>,
    onRemove: (Attachment) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        attachments.forEach { att ->
            OutlinedCard {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Attachment, contentDescription = null)
                    Column {
                        Text(att.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatSize(att.sizeBytes), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    IconButton(onClick = { onRemove(att) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove attachment")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit,
    canSend: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onAttach) {
            Icon(Icons.Default.Attachment, contentDescription = "Attach file")
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Message…") },
            modifier = Modifier.weight(1f),
            maxLines = 5,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send, keyboardType = KeyboardType.Text),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() })
        )
        IconButton(onClick = onSend, enabled = canSend) {
            Icon(Icons.Default.Send, contentDescription = "Send")
        }
    }
}

@Composable
private fun StatusDot(status: DeviceStatus) {
    val color = when (status) {
        DeviceStatus.Online -> Color(0xFF2E7D32)
        DeviceStatus.Offline -> Color(0xFFE65100)
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, shape = CircleShape)
    )
}

private fun sampleDevices(): List<Device> = listOf(
    Device("PiA", "Pi A", "192.168.1.10", DeviceStatus.Online, System.currentTimeMillis() - 2 * 60 * 1000, 12.9715987, 77.594566),
    Device("PiB", "Pi B", "192.168.1.11", DeviceStatus.Online, System.currentTimeMillis() - 5 * 60 * 1000, 12.975, 77.595),
    Device("Jetson", "Jetson TX2", "192.168.1.12", DeviceStatus.Offline, System.currentTimeMillis() - 45 * 60 * 1000, 12.97, 77.59),
    Device("Truck", "Rover Truck", "192.168.1.14", DeviceStatus.Online, System.currentTimeMillis() - 10 * 60 * 1000, 12.965, 77.6),
    Device("Relay", "Relay Node", "192.168.1.15", DeviceStatus.Online, System.currentTimeMillis() - 90 * 1000, 12.969, 77.59)
)

private fun sampleChat(deviceId: String): List<Message> = listOf(
    Message(from = deviceId, to = "you", text = "We are online", timestamp = System.currentTimeMillis() - 60 * 60 * 1000, owner = MessageOwner.Remote),
    Message(from = "you", to = deviceId, text = "Share coords", timestamp = System.currentTimeMillis() - 55 * 60 * 1000, owner = MessageOwner.Local),
    Message(from = deviceId, to = "you", text = "Lat/Lon updated", timestamp = System.currentTimeMillis() - 50 * 60 * 1000, owner = MessageOwner.Remote)
)

private fun formatRelativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp) / 1000
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86_400 -> "${diff / 3600}h ago"
        else -> "${diff / 86_400}d ago"
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

private fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(kb)
}

@Composable
private fun ActiveCallInterface(
    peerId: String,
    onEndCall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = Surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Call icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = PrimaryBlue.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        modifier = Modifier.size(64.dp),
                        tint = PrimaryBlue
                    )
                }

                // Peer ID
                Text(
                    text = "Call with",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
                Text(
                    text = peerId,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Call status
                Text(
                    text = "Call in progress...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // End call button
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = Color.Red.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneDisabled,
                        contentDescription = "End Call",
                        modifier = Modifier.size(32.dp),
                        tint = Color.Red
                    )
                }
                Text(
                    text = "End Call",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red
                )
            }
        }
    }
}

@Composable
private fun MqttConnectingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text("Connecting to MQTT server…")
                Text(
                    "Please wait",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

fun getLocalIpAddress(): String? {
    return try {
        NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress
    } catch (e: Exception) {
        null
    }
}

// Preview disabled - requires CallController which needs real Context
// @Preview(showBackground = true, widthDp = 420)
// @Composable
// private fun MobilePreview() {
//     ISRO_APPTheme {
//         // Preview requires CallController which needs real Context and dependencies
//         // val previewMqttManager = MqttManager(myId = "preview")
//         // IsroApp(mqttManager = previewMqttManager, callController = ...)
//     }
// }