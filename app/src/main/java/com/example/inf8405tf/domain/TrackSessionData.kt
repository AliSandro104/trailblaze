package com.example.inf8405tf.domain

import java.util.Date

/**
 * Données associées à une session de parcours.
 */
data class TrackSessionData(
    // Date et heure où ces données ont été enregistrées
    val dateTime: Date? = null,

    // Durée totale de la session en secondes
    val duration: Int? = null,

    // Distance parcourue à un instant donné pendant la session
    val distance: Float? = null,

    // Nombre de pas effectués à un instant donné pendant la session
    val steps: Int? = null,

    // Conditions météo générales à un instant donné pendant la session
    val weatherCondition: String? = null,

    // Vitesse instantanée (m/s)
    val speed: Float? = null,

    // Accélération instantanée (m/s²)
    val acceleration: Float? = null,

    // Température à un instant donné pendant la session (°C)
    val temperature: Float? = null,

    // Humidité à un instant donné pendant la session (%)
    val humidity: Float? = null,

    // Vitesse du vent à un instant donné pendant la session (m/s)
    val windSpeed: Float? = null
)

