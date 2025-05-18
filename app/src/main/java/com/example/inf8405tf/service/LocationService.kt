package com.example.inf8405tf.service

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LocationService @Inject constructor(@ApplicationContext private val context: Context) {

    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getLastLocation(onLocationReceived: (Location?) -> Unit) {
        // Verifie les permissions
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
                onLocationReceived(location)
            }.addOnFailureListener { e ->
                Log.e("LocationService", "Erreur: ${e.message}")
            }
        } else {
            Log.e("LocationService", "Manque de permissions")
            onLocationReceived(null)
        }
    }

    // Retourne location + speed
    fun getLocationData(onLocationDataReceived: (Location?, Float) -> Unit) {
        getLastLocation { location ->
            location?.let {
                onLocationDataReceived(it, it.speed)
            } ?: onLocationDataReceived(null, 0f)
        }
    }
}
