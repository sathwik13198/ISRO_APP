package com.example.isro_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LabelOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.views.overlay.infowindow.InfoWindow
import com.example.isro_app.location.LocationState
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.XYTileSource
import com.example.isro_app.settings.ServerSettingsManager
import android.view.View
import android.widget.TextView
import android.graphics.Typeface
import android.view.Gravity

fun createTileSource(tileServerUrl: String): XYTileSource {
    return XYTileSource(
        "LAN-TILES",
        0,          // min zoom
        14,         // max zoom
        256,        // tile size
        ".png",
        arrayOf(tileServerUrl)
    )
}

/**
 * Custom InfoWindow that displays device name below the marker
 */
class DeviceNameInfoWindow(
    layoutResId: Int,
    mapView: MapView,
    private val textSize: Float = 12f
) : InfoWindow(layoutResId, mapView) {

    override fun onClose() {}

    override fun onOpen(item: Any?) {
        if (item is Marker) {
            val textView = mView.findViewById<TextView>(android.R.id.text1)
            textView?.apply {
                text = item.snippet ?: item.title
                textSize = this@DeviceNameInfoWindow.textSize
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.argb(200, 0, 0, 0))
                setPadding(8, 4, 8, 4)
            }
        }
    }
}


data class MapDevice(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val displayName: String
)

@Composable
fun OfflineMapView(
    devices: List<MapDevice>,
    currentLocation: LocationState,
    myDeviceId: String,
    tileServerUrl: String = ServerSettingsManager.loadSettings(androidx.compose.ui.platform.LocalContext.current).tileServerUrl,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current

    if (isPreview) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Offline map preview not available")
        }
        return
    }

    // 🎯 Follow GPS vs free pan
    val followMode = remember { mutableStateOf(true) }
    val userInteracting = remember { mutableStateOf(false) }
    val showDeviceNames = remember { mutableStateOf(false) }

    val markerMap = remember { mutableStateMapOf<String, Marker>() }
    val markerDisplayNames = remember { mutableStateMapOf<String, String>() }
    val selfMarker = remember { mutableStateOf<Marker?>(null) }
    val mapViewState = remember { mutableStateOf<MapView?>(null) }
    val currentTileServerUrl = remember { mutableStateOf(tileServerUrl) }
    val deviceNameInfoWindow = remember { mutableStateOf<DeviceNameInfoWindow?>(null) }
    
    // Update tile source when URL changes
    LaunchedEffect(tileServerUrl) {
        if (currentTileServerUrl.value != tileServerUrl) {
            currentTileServerUrl.value = tileServerUrl
            mapViewState.value?.let { mapView ->
                val newTileSource = createTileSource(tileServerUrl)
                mapView.setTileSource(newTileSource)
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewState.value = this

                    // ✅ Use LAN tile server (dynamic)
                    val tileSource = createTileSource(currentTileServerUrl.value)
                    setTileSource(tileSource)

                    // ✅ Allow LAN HTTP (not general internet)
                    setUseDataConnection(true)

                    // zoom limits must match tiles
                    minZoomLevel = 0.0
                    maxZoomLevel = 14.0

                    setMultiTouchControls(true)

                    // ❌ REMOVE default OSMDroid zoom buttons
                    zoomController.setVisibility(
                        org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                    )
                    
                    // Create custom InfoWindow for device names
                    val infoWindow = DeviceNameInfoWindow(
                        android.R.layout.simple_list_item_1,
                        this,
                        textSize = 12f
                    )
                    deviceNameInfoWindow.value = infoWindow

                    // 🔹 Detect user drag / touch to disable follow
                    // setOnTouchListener { _, _ ->
                    //     if (followMode.value) {
                    //         followMode.value = false   // 🖐️ disable follow when user drags
                    //     }
                    //     false
                    // }

                    // Initial position
                    val first = devices.firstOrNull()
                    if (first != null) {
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(first.latitude, first.longitude))

                        addOrUpdateMarker(
                            id = first.id,
                            lat = first.latitude,
                            lon = first.longitude,
                            displayName = first.displayName,
                            map = this,
                            markerMap = markerMap,
                            markerDisplayNames = markerDisplayNames,
                            isSelf = first.id == myDeviceId
                        )
                    }
                }
            },
            update = { mapView ->
                // 🔴 MQTT / other devices (and self when present)
                devices.forEach { device ->
                    val isSelfDevice = device.id == myDeviceId
                    addOrUpdateMarker(
                        id = device.id,
                        lat = device.latitude,
                        lon = device.longitude,
                        displayName = device.displayName,
                        map = mapView,
                        markerMap = markerMap,
                        markerDisplayNames = markerDisplayNames,
                        isSelf = isSelfDevice
                    )
                }
                
                // Update device name visibility
                deviceNameInfoWindow.value?.let { infoWindow ->
                    markerMap.forEach { (deviceId, marker) ->
                        val displayName = markerDisplayNames[deviceId]
                        if (displayName != null) {
                            if (showDeviceNames.value) {
                                // Set custom InfoWindow and show device name
                                marker.infoWindow = infoWindow
                                marker.snippet = displayName
                                marker.showInfoWindow()
                            } else {
                                // Hide InfoWindow
                                marker.closeInfoWindow()
                            }
                        }
                    }
                }

                // 🔵 CURRENT DEVICE LOCATION (GPS fallback when MQTT doesn't have self)
                val isMyDeviceInMqtt = devices.any { it.id == myDeviceId }

                if (currentLocation.hasFix && !isMyDeviceInMqtt) {
                    val position = GeoPoint(
                        currentLocation.latitude,
                        currentLocation.longitude
                    )

                    if (selfMarker.value == null) {
                        val marker = Marker(mapView).apply {
                            this.position = position
                            this.title = "You"
                            this.icon = mapView.context.getDrawable(
                                com.example.isro_app.R.drawable.ic_marker_self
                            )
                        }
                        mapView.overlays.add(marker)
                        selfMarker.value = marker
                    } else {
                        selfMarker.value?.position = position
                    }

                    // 🎯 AUTO-FOLLOW ONLY IF ENABLED
                    if (followMode.value) {
                        mapView.controller.animateTo(position)
                    }
                }
            }
        )

        // 🎛 MAP CONTROLS
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // // 🖐️ PAN MODE (disable follow)
            // IconButton(
            //     onClick = { followMode.value = false },
            //     modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            // ) {
            //     Text("🖐️", color = Color.White)
            // }

            // // 🎯 FOLLOW MODE
            // IconButton(
            //     onClick = { followMode.value = true },
            //     modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            // ) {
            //     Text("🎯", color = Color.White)
            // }

            // ➕ ZOOM IN
            IconButton(
                onClick = { mapViewState.value?.controller?.zoomIn() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Text("+", color = Color.White)
            }

            // ➖ ZOOM OUT
            IconButton(
                onClick = { mapViewState.value?.controller?.zoomOut() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Text("-", color = Color.White)
            }
            
            // 🏷️ TOGGLE DEVICE NAMES
            IconButton(
                onClick = { 
                    showDeviceNames.value = !showDeviceNames.value
                    // Update all markers
                    deviceNameInfoWindow.value?.let { infoWindow ->
                        mapViewState.value?.let { mapView ->
                            markerMap.forEach { (deviceId, marker) ->
                                val displayName = markerDisplayNames[deviceId]
                                if (displayName != null) {
                                    if (showDeviceNames.value) {
                                        marker.infoWindow = infoWindow
                                        marker.snippet = displayName
                                        marker.showInfoWindow()
                                    } else {
                                        marker.closeInfoWindow()
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = if (showDeviceNames.value) Icons.Default.LabelOff else Icons.Default.Label,
                    contentDescription = if (showDeviceNames.value) "Hide device names" else "Show device names",
                    tint = Color.White,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}

fun addOrUpdateMarker(
    id: String,
    lat: Double,
    lon: Double,
    displayName: String,
    map: MapView,
    markerMap: MutableMap<String, Marker>,
    markerDisplayNames: MutableMap<String, String>,
    isSelf: Boolean
) {
    val position = GeoPoint(lat, lon)
    
    // Store display name
    markerDisplayNames[id] = displayName

    if (markerMap.containsKey(id)) {
        markerMap[id]?.position = position
    } else {
        val marker = Marker(map).apply {
            this.position = position
            this.title = if (isSelf) "You" else displayName
            this.snippet = displayName
            this.icon = map.context.getDrawable(
                if (isSelf) {
                    com.example.isro_app.R.drawable.ic_marker_self
                } else {
                    com.example.isro_app.R.drawable.ic_marker_other
                }
            )
            // Set anchor point to bottom center so InfoWindow appears below
            this.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(marker)
        markerMap[id] = marker
    }

    map.invalidate()
}
