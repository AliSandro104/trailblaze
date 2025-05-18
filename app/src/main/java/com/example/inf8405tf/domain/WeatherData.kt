package com.example.inf8405tf.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Données collectées toutes les 20 minutes
 */
@Entity(tableName = "weather_data")
data class WeatherData(
    // Identifiant unique pour chaque relevé météo
    @PrimaryKey(autoGenerate = true) val weatherDataId: Long = 0,

    // Identifiant du parcours auquel ces données météo sont associées
    @ColumnInfo(name = "track_id") val trackId: String,

    // Date et heure où ces données ont été enregistrées
    @ColumnInfo(name = "timestamp") val timestamp: Date,

    // Température ambiante à un instant donné en degrés Celsius (°C)
    @ColumnInfo(name = "temperature") val temperature: Float?,

    // Humidité ambiante à un instant donné en pourcentage (%)
    @ColumnInfo(name = "humidity") val humidity: Float?,

    // Vitesse du vent ambiant à un instant donné en mètres par seconde (m/s)
    @ColumnInfo(name = "wind_speed") val windSpeed: Float?,

    // Conditions météorologiques générales à un instant donné pendant l'activité (P. ex., "Ensoleillé", "Pluvieux")
    @ColumnInfo(name = "weather_condition") var weatherCondition: String? = null,
)
