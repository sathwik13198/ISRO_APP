package com.example.isro_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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

@Composable
fun OfflineMapView(
    latitude: Double,
    longitude: Double,
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

                controller.setZoom(11.0)
                controller.setCenter(GeoPoint(latitude, longitude))

                val marker = Marker(this)
                marker.position = GeoPoint(latitude, longitude)
                marker.title = "Device Location"
                overlays.add(marker)
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose { }
    }
}
