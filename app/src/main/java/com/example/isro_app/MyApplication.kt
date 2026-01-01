package com.example.isro_app

import android.app.Application
import android.os.Environment
import androidx.preference.PreferenceManager
import com.example.isro_app.mqtt.MqttManager
import com.example.isro_app.mqtt.MqttSettingsManager
import com.example.isro_app.settings.ServerSettingsManager
import org.osmdroid.config.Configuration
import java.io.File

class MyApplication : Application() {

    /**
     * Single, app-wide MQTT manager instance.
     *
     * Created once when the app process starts and reused everywhere.
     * UI/composables must not create their own MQTT clients.
     */
    lateinit var mqttManager: MqttManager
        private set

    override fun onCreate() {
        super.onCreate()

        // ---- Load MQTT settings from SharedPreferences ----
        val mqttSettings = MqttSettingsManager.loadSettings(applicationContext)
        
        // ---- Load device ID from SharedPreferences ----
        val deviceId = MqttSettingsManager.loadDeviceId(applicationContext)
        
        // ---- Load server settings from SharedPreferences ----
        val serverSettings = ServerSettingsManager.loadSettings(applicationContext)

        // ---- Initialize MQTT (global singleton) ----
        mqttManager = MqttManager(
            myId = deviceId,
            settings = mqttSettings,
            attachmentServer = serverSettings.attachmentServerUrl
        )
        mqttManager.connect()

        // ---- Initialize OSMDroid base paths ----
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
            // osmdroidTileCache = File(osmBasePath, "tiles")  <-- KEEP DISABLED

            userAgentValue = packageName
        }
    }
}
