package com.example.isro_app.mqtt

import android.content.Context
import android.content.SharedPreferences

/**
 * Data class representing MQTT broker settings
 */
data class MqttSettings(
    val brokerUri: String = "tcp://192.168.29.239:1883",
    val username: String = "",
    val password: String = ""
)

/**
 * Manager for persisting and loading MQTT settings from SharedPreferences
 */
object MqttSettingsManager {
    private const val PREFS_NAME = "mqtt_settings"
    private const val KEY_BROKER_URI = "mqtt_broker_uri"
    private const val KEY_USERNAME = "mqtt_username"
    private const val KEY_PASSWORD = "mqtt_password"

    /**
     * Save MQTT settings to SharedPreferences
     */
    fun saveSettings(context: Context, settings: MqttSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_BROKER_URI, settings.brokerUri)
            putString(KEY_USERNAME, settings.username)
            putString(KEY_PASSWORD, settings.password)
            apply()
        }
    }

    /**
     * Load MQTT settings from SharedPreferences with defaults
     */
    fun loadSettings(context: Context): MqttSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            MqttSettings(
                brokerUri = prefs.getString(KEY_BROKER_URI, "tcp://192.168.29.239:1883") ?: "tcp://192.168.29.239:1883",
                username = prefs.getString(KEY_USERNAME, "") ?: "",
                password = prefs.getString(KEY_PASSWORD, "") ?: ""
            )
        } catch (e: Exception) {
            // If corrupted data, return defaults
            MqttSettings()
        }
    }

    /**
     * Validate broker URI format
     * Returns true if URI is valid (starts with tcp:// or ssl://, has valid host and port)
     */
    fun isValidBrokerUri(uri: String): Boolean {
        if (uri.isBlank()) return false
        
        return try {
            val (_, port) = parseBrokerUri(uri) ?: return false
            port in 1..65535
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Parse broker URI to extract host and port
     * Returns Pair(host, port) or null if invalid
     */
    fun parseBrokerUri(uri: String): Pair<String, Int>? {
        if (uri.isBlank()) return null
        
        // Must start with tcp:// or ssl://
        if (!uri.startsWith("tcp://") && !uri.startsWith("ssl://")) {
            return null
        }
        
        try {
            val protocol = if (uri.startsWith("tcp://")) "tcp://" else "ssl://"
            val withoutProtocol = uri.removePrefix(protocol)
            
            // Split host:port
            val parts = withoutProtocol.split(":")
            if (parts.size != 2) return null
            
            val host = parts[0].trim()
            val port = parts[1].trim().toInt()
            
            if (host.isBlank() || port !in 1..65535) {
                return null
            }
            
            return Pair(host, port)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Get user-friendly error message for invalid broker URI
     */
    fun getBrokerUriErrorMessage(uri: String): String {
        if (uri.isBlank()) {
            return "Broker URI cannot be empty"
        }
        
        if (!uri.startsWith("tcp://") && !uri.startsWith("ssl://")) {
            return "Broker URI must start with tcp:// or ssl://"
        }
        
        val parsed = parseBrokerUri(uri)
        if (parsed == null) {
            return "Invalid format. Expected: tcp://host:port or ssl://host:port"
        }
        
        val (_, port) = parsed
        if (port !in 1..65535) {
            return "Port must be between 1 and 65535"
        }
        
        return "Invalid broker URI"
    }
}

