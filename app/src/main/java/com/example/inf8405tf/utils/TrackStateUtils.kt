package com.example.inf8405tf.utils

import android.util.Log
import com.example.inf8405tf.data.AppDatabase
import com.example.inf8405tf.domain.Track
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Singleton

@Singleton
class TrackStateUtils @Inject constructor() {

    suspend fun getStartedTrack(database: AppDatabase): Track {
        return withContext(Dispatchers.IO) {
            val username = UserSession.getUsername()

            if (username == null) {
                Log.e("Parcours", "Nom d'utilisateur indéfini")
                throw Exception("Parcours: Nom d'utilisateur indéfini")
            }

            val startedTrack = database.trackDao().getStartedTrackForUser(username)

            if (startedTrack == null) {
                Log.e("Parcours", "Parcours débuté indéfini")
                throw Exception("Parcours: Parcours débuté indéfini")
            }

            startedTrack
        }
    }
}