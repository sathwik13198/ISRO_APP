package com.example.isro_app.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * Data class representing server configuration settings
 */
data class ServerSettings(
    val attachmentServerUrl: String = "http://192.168.29.242:8090",
    val tileServerUrl: String = "http://192.168.29.242:8080/tiles/",
    val asteriskServerIp: String = "192.168.29.242"
)

/**
 * Manager for persisting and loading server settings from SharedPreferences
 */
object ServerSettingsManager {
    private const val PREFS_NAME = "server_settings"
    private const val KEY_ATTACHMENT_SERVER = "attachment_server_url"
    private const val KEY_TILE_SERVER = "tile_server_url"
    private const val KEY_ASTERISK_SERVER = "asterisk_server_ip"

    /**
     * Save server settings to SharedPreferences
     */
    fun saveSettings(context: Context, settings: ServerSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_ATTACHMENT_SERVER, settings.attachmentServerUrl)
            putString(KEY_TILE_SERVER, settings.tileServerUrl)
            putString(KEY_ASTERISK_SERVER, settings.asteriskServerIp)
            apply()
        }
    }

    /**
     * Load server settings from SharedPreferences with defaults
     */
    fun loadSettings(context: Context): ServerSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            ServerSettings(
                attachmentServerUrl = prefs.getString(KEY_ATTACHMENT_SERVER, "http://192.168.29.242:8090") ?: "http://192.168.29.242:8090",
                tileServerUrl = prefs.getString(KEY_TILE_SERVER, "http://192.168.29.242:8080/tiles/") ?: "http://192.168.29.242:8080/tiles/",
                asteriskServerIp = prefs.getString(KEY_ASTERISK_SERVER, "192.168.29.242") ?: "192.168.29.242"
            )
        } catch (e: Exception) {
            // If corrupted data, return defaults
            ServerSettings()
        }
    }

    /**
     * Validate attachment server URL format
     */
    fun isValidAttachmentServerUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate tile server URL format
     */
    fun isValidTileServerUrl(url: String): Boolean {
        if (url.isBlank()) return false
        return try {
            (url.startsWith("http://") || url.startsWith("https://")) && url.endsWith("/tiles/")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Validate Asterisk server IP format (basic IP validation)
     */
    fun isValidAsteriskServerIp(ip: String): Boolean {
        if (ip.isBlank()) return false
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) return false
            parts.all { part ->
                val num = part.toIntOrNull()
                num != null && num in 0..255
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get user-friendly error message for invalid attachment server URL
     */
    fun getAttachmentServerErrorMessage(url: String): String {
        if (url.isBlank()) {
            return "Attachment server URL cannot be empty"
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "URL must start with http:// or https://"
        }
        return "Invalid attachment server URL"
    }

    /**
     * Get user-friendly error message for invalid tile server URL
     */
    fun getTileServerErrorMessage(url: String): String {
        if (url.isBlank()) {
            return "Tile server URL cannot be empty"
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "URL must start with http:// or https://"
        }
        if (!url.endsWith("/tiles/")) {
            return "URL must end with /tiles/"
        }
        return "Invalid tile server URL"
    }

    /**
     * Get user-friendly error message for invalid Asterisk server IP
     */
    fun getAsteriskServerErrorMessage(ip: String): String {
        if (ip.isBlank()) {
            return "Asterisk server IP cannot be empty"
        }
        val parts = ip.split(".")
        if (parts.size != 4) {
            return "IP must be in format: xxx.xxx.xxx.xxx"
        }
        return "Invalid IP address"
    }
}

