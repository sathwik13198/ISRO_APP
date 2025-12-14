package com.example.isro_app

import android.app.Application
import android.os.Environment
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import java.io.File

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val osmBasePath = File(
            Environment.getExternalStorageDirectory(),
            "osmdroid"
        )

        // Ensure directory exists
        if (!osmBasePath.exists()) {
            osmBasePath.mkdirs()
        }

        Configuration.getInstance().apply {
            load(
                applicationContext,
                PreferenceManager.getDefaultSharedPreferences(applicationContext)
            )

            // ✅ MUST point directly to /storage/emulated/0/osmdroid
            osmdroidBasePath = osmBasePath

            // ❌ DO NOT set osmdroidTileCache for MBTiles
            // osmdroidTileCache = File(osmBasePath, "tiles")  <-- REMOVE

            userAgentValue = packageName
        }
    }
}
