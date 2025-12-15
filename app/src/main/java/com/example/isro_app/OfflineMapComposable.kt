package com.example.isro_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import android.os.Environment
import android.widget.Toast
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import java.io.File

data class MapDevice(
    val id: String,
    val latitude: Double,
    val longitude: Double
)

@Composable
fun OfflineMapView(
    devices: List<MapDevice>,
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

    val markerMap = remember { mutableStateMapOf<String, Marker>() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->

            // 🔍 DEBUG: check MBTiles file
            val mbtilesFile = File(
                Environment.getExternalStorageDirectory(),
                "osmdroid/india.mbtiles"
            )

            Toast.makeText(
                ctx,
                if (mbtilesFile.exists())
                    "✅ MBTiles detected (${mbtilesFile.length() / (1024 * 1024)} MB)"
                else
                    "❌ MBTiles NOT FOUND",
                Toast.LENGTH_LONG
            ).show()

            MapView(ctx).apply {

                // ✅ REQUIRED: tells OSMDroid to use offline tiles
                setTileSource(TileSourceFactory.MAPNIK)

                // 🔒 FORCE OFFLINE
                setUseDataConnection(false)

                // 🔒 YOU HAVE TILES ONLY 0–12
                minZoomLevel = 0.0
                maxZoomLevel = 12.0

                setMultiTouchControls(true)

                // Initial camera position – center on first device if available
                val first = devices.firstOrNull()
                if (first != null) {
                    controller.setZoom(11.0)
                    controller.setCenter(GeoPoint(first.latitude, first.longitude))

                    // Also ensure at least one marker exists on first render
                    addOrUpdateMarker(
                        id = first.id,
                        lat = first.latitude,
                        lon = first.longitude,
                        map = this,
                        markerMap = markerMap
                    )
                }
            }
        },
        update = { mapView ->
            // Update or create markers for all devices without clearing the map
            devices.forEach { device ->
                addOrUpdateMarker(
                    id = device.id,
                    lat = device.latitude,
                    lon = device.longitude,
                    map = mapView,
                    markerMap = markerMap
                )
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose { }
    }
}

fun addOrUpdateMarker(
    id: String,
    lat: Double,
    lon: Double,
    map: MapView,
    markerMap: MutableMap<String, Marker>
) {
    val position = GeoPoint(lat, lon)

    if (markerMap.containsKey(id)) {
        markerMap[id]?.position = position
    } else {
        val marker = Marker(map).apply {
            this.position = position
            this.title = id
        }
        map.overlays.add(marker)
        markerMap[id] = marker
    }

    map.invalidate()
}
