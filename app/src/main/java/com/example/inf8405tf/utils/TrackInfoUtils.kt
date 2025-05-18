package com.example.inf8405tf.utils

import jakarta.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Singleton

@Singleton
class TrackInfoUtils @Inject constructor() {

    fun formatShortDateTime(dateTime: Date?): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        return dateTime.let { dateFormat.format(it) } ?: "Date inconnue"
    }

    fun formatLongDateTime(dateTime: Date?): String {
        return dateTime.toString()
    }

    fun formatTime(label: String, dateTime: Date?): String {
        val simpleDateFormat = SimpleDateFormat("HH:mm")

        return dateTime?.time?.let { Date(it) }?.let {
            "$label: ${simpleDateFormat.format(it)}"
        }
            ?: "$label début inconnue"
    }

    fun formatStepsCountLabel(stepsCount: Int?): String {
        return stepsCount?.let {
            "Pas: $stepsCount"
        } ?: "Nombre de pas inconnu"
    }

    fun formatDuration(duration: Int?): String {
        return duration?.let {
            val minutes = it / 60
            val seconds = it % 60
            String.format("%02d:%02d", minutes, seconds)
        } ?: "Durée inconnue"
    }

    fun formatDistance(distance: Float?): String {
        return distance?.let {
            String.format("%.1f km", it / 1000)  // Convert meters to kilometers
        } ?: "Distance inconnue"
    }

    fun formatStepsCount(steps: Int?): String {
        return steps?.let { "$it pas" } ?: "Pas inconnus"
    }

    fun formatWeatherCondition(label: String, weatherCondition: String?): String {
        return weatherCondition?.let { "$label: $it" } ?: "Météo inconnue"
    }

    fun formatSpeed(label: String, speed: Float?): String {
        return speed?.let {
            "$label: ${String.format("%.1f km/h", it * 3.6)}"  // Convert m/s to km/h
        } ?: "Vitesse inconnue"
    }

    fun formatSpeedNoLabel(speed: Float?): String {
        return speed?.let {
            "${String.format("%.1f km/h", it * 3.6)}"  // Convert m/s to km/h
        } ?: "?"
    }

    fun formatAcceleration(label: String, acceleration: Float?): String {
        return acceleration?.let {
            "$label: ${String.format("%.1f m/s²", it)}"
        } ?: "Accélération inconnue"
    }

    fun formatTemperature(label: String, temperature: Float?): String {
        return temperature?.let {
            "$label: ${String.format("%.1f°C", it)}"
        } ?: "Température inconnue"
    }

    fun formatHumidity(label: String, humidity: Float?): String {
        return humidity?.let {
            "$label: ${String.format("%.1f", it)}%"
        } ?: "Humidité inconnue"
    }

    fun formatWindSpeed(label: String, windSpeed: Float?): String {
        return windSpeed?.let {
            "$label: ${String.format("%.1f m/s", it)}"
        } ?: "Vent inconnu"
    }
}