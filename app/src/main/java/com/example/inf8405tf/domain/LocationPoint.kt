package com.example.inf8405tf.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Données collectées toutes les 5 secondes
 */
@Entity(tableName = "location_point")
data class LocationPoint(
    // Identifiant unique pour ce point de localisation
    @PrimaryKey(autoGenerate = true) val pointId: Long = 0,

    // Identifiant du trajet auquel ce point est associé
    @ColumnInfo(name = "track_id") val trackId: String,

    // Date et heure où ces données ont été enregistrées
    @ColumnInfo(name = "timestamp") val timestamp: Date,

    // Latitude de la position
    @ColumnInfo(name = "latitude") val latitude: Double,

    // Longitude de la position
    @ColumnInfo(name = "longitude") val longitude: Double,

    // Altitude de la position
    @ColumnInfo(name = "altitude") val altitude: Double,

    // Vitesse mesurée à un instant donné (m/s)
    @ColumnInfo(name = "speed") val speed: Float,
)
