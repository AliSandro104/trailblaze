package com.example.inf8405tf.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.domain.LocationPoint
import com.example.inf8405tf.domain.Track
import com.example.inf8405tf.utils.TrackStateUtils
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class GPSScannerService(
    context: Context,
    private val database: AppDatabase,
    private val trackStateUtils: TrackStateUtils
) {

    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var lastKnownLocation: Location? = null
    private var lastLocationTime = 0L
    private var startedTrack: Track? = null

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 10000 // Intervalle 10 sec
    ).setMinUpdateIntervalMillis(5000) // 5 sec min entre les mises à jour
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                lastKnownLocation = location

                Log.d(
                    "GPSScannerUtils",
                    "Localisation reçue: ${location.latitude}, ${location.longitude}"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(callback: (Double, Double, Double, Float, Date) -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLocationTime < 5000) { // 5 sec entre les requêtes
            lastKnownLocation?.let {
                callback(it.latitude, it.longitude, it.altitude, it.speed, Date())
                Log.d("GPSScannerUtils", "Utilise la localisation dans le cache")
            }
            return
        }
        lastLocationTime = currentTime

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                callback(
                    location.latitude,
                    location.longitude,
                    location.altitude,
                    location.speed,
                    Date()
                )
                lastKnownLocation = location

                // Persists new location data point
                CoroutineScope(Dispatchers.Main).launch {
                    insertLocationPoint(location)
                }
            } else {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
                Log.d("GPSScannerUtils", "Demande nouvelle localisation")
            }
        }.addOnFailureListener { e ->
            Log.e("GPSScannerUtils", "Échec dans la recherche de nouvelle localisation", e)
        }
    }

    private suspend fun insertLocationPoint(location: Location) {
        if (startedTrack == null) {
            startedTrack = trackStateUtils.getStartedTrack(database)

            database.locationPointDao().insertLocationPoint(
                LocationPoint(
                    trackId = startedTrack!!.trackId,
                    timestamp = Date(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    speed = location.speed
                )
            )
        }
    }

    fun resetTrack() {
        startedTrack = null
    }
}
