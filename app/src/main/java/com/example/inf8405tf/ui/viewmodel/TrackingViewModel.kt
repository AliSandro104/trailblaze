package com.example.inf8405tf.ui.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.domain.LocationPoint
import com.example.inf8405tf.service.GPSScannerService
import com.example.inf8405tf.utils.TrackStateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    database: AppDatabase,
    application: Application,
    trackStateUtils: TrackStateUtils
) : ViewModel() {
    // MapsFragment utilise ces points pour tracer le chemin
    private val _pathPoints = MutableLiveData<List<LocationPoint>>(emptyList())
    val pathPoints: LiveData<List<LocationPoint>> = _pathPoints
    // HistoriqueParcoursFragment sélectionne un parcours, puis MapsFragment l'affiche sur la carte
    private val _selectedTrackId = MutableLiveData<String?>()
    val selectedTrackId: LiveData<String?> = _selectedTrackId
    // MapsFragment observe pour effacer le chemin affiché
    private val _clearMapEvent = MutableLiveData<Boolean>()
    val clearMapEvent: LiveData<Boolean> = _clearMapEvent

    private val gpsService = GPSScannerService(application, database, trackStateUtils)
    private var trackingJob: Job? = null

    fun startTracking(trackId: String) {
        // ne rien faire si session est active
        if (trackingJob?.isActive == true) {
            return
        }
        trackingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                gpsService.getCurrentLocation { lat, long, altitude, speed, date ->
                    val updatedList = _pathPoints.value?.toMutableList() ?: mutableListOf()
                    updatedList.add(
                        LocationPoint(
                            trackId = trackId,
                            timestamp = date,
                            latitude = lat,
                            longitude = long,
                            altitude = altitude,
                            speed = speed
                        )
                    )
                    _pathPoints.postValue(updatedList)
                }
                // effectuer une mise a jour de la vue à chaque seconde
                delay(1000)
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        // réinitialise le id stocké dans GPSService
        gpsService.resetTrack()
    }

    fun resetPath() {
        _pathPoints.postValue(emptyList())
    }

    fun selectTrackToDisplay(id: String?) {
        _selectedTrackId.postValue(id)
    }

    fun getSelectTrackToDisplay(): String? {
        return selectedTrackId.value
    }

    fun triggerClearMap() {
        _clearMapEvent.postValue(true)
    }
}