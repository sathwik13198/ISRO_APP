package com.example.isro_app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.isro_app.location.LocationState
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.XYTileSource

private val LanTileSource = XYTileSource(
    "LAN-TILES",
    0,          // min zoom
    14,         // max zoom
    256,        // tile size
    ".png",
    arrayOf("http://192.168.29.242:8080/tiles/")
)

data class MapDevice(
    val id: String,
    val latitude: Double,
    val longitude: Double
)

@Composable
fun OfflineMapView(
    devices: List<MapDevice>,
    currentLocation: LocationState,
    myDeviceId: String,
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

    val markerMap = remember { mutableStateMapOf<String, Marker>() }
    val selfMarker = remember { mutableStateOf<Marker?>(null) }
    val mapViewState = remember { mutableStateOf<MapView?>(null) }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    mapViewState.value = this

                    // ✅ Use LAN tile server
                    setTileSource(LanTileSource)

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
                            map = this,
                            markerMap = markerMap,
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
                        map = mapView,
                        markerMap = markerMap,
                        isSelf = isSelfDevice
                    )
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
            // 🖐️ PAN MODE (disable follow)
            IconButton(
                onClick = { followMode.value = false },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Text("🖐️", color = Color.White)
            }

            // 🎯 FOLLOW MODE
            IconButton(
                onClick = { followMode.value = true },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Text("🎯", color = Color.White)
            }

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
    map: MapView,
    markerMap: MutableMap<String, Marker>,
    isSelf: Boolean
) {
    val position = GeoPoint(lat, lon)

    if (markerMap.containsKey(id)) {
        markerMap[id]?.position = position
    } else {
        val marker = Marker(map).apply {
            this.position = position
            this.title = if (isSelf) "You" else id
            this.icon = map.context.getDrawable(
                if (isSelf) {
                    com.example.isro_app.R.drawable.ic_marker_self
                } else {
                    com.example.isro_app.R.drawable.ic_marker_other
                }
            )
        }
        map.overlays.add(marker)
        markerMap[id] = marker
    }

    map.invalidate()
}
