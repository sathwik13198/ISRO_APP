package com.example.isro_app.location

data class LocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val hasFix: Boolean = false,
    val timestamp: String = ""
)
