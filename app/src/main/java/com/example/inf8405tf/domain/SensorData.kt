package com.example.inf8405tf.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Données collectées toutes les secondes
 */
@Entity(tableName = "sensor_data")
data class SensorData(
    // Identifiant unique pour chaque relevé de capteur
    @PrimaryKey(autoGenerate = true) val sensorDataId: Long = 0,

    // Identifiant du trajet auquel ce relevé de capteur est associé
    @ColumnInfo(name = "track_id") val trackId: String,

    // Date et heure où ces données ont été enregistrées
    @ColumnInfo(name = "timestamp") val timestamp: Date,

    // Valeur mesurée par l'accéléromètre (m/s²)
    @ColumnInfo(name = "accelerometer") val accelerometer: Float?,
)
