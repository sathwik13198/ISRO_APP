package com.example.isro_app

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import com.example.isro_app.location.LocationState
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _location = MutableStateFlow(LocationState())
    val location: StateFlow<LocationState> = _location

    private val request = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        3000L
    )
        .setMinUpdateDistanceMeters(2f)
        .build()

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc: Location = result.lastLocation ?: return
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(Date(loc.time))
            _location.value = LocationState(
                latitude = loc.latitude,
                longitude = loc.longitude,
                hasFix = true,
                timestamp = timestamp
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        fusedClient.requestLocationUpdates(
            request,
            callback,
            null
        )
    }

    override fun onCleared() {
        fusedClient.removeLocationUpdates(callback)
        super.onCleared()
    }
}
