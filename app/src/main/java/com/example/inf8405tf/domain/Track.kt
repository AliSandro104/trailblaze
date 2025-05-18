package com.example.inf8405tf.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Données collectées une fois que le parcours est débuté et terminé
 */
@Entity(tableName = "track")
data class Track(
    // Identifiant unique pour chaque session de parcours
    @PrimaryKey(autoGenerate = false) val trackId: String,

    // Nom de l'utilisateur qui a effectué cette activité
    @ColumnInfo(name = "username") val username: String,

    // Statut du parcours : "Commencé" | "Terminé"
    @ColumnInfo(name = "track_status") var trackStatus: String,

    // Date et heure de début de la session de suivi
    @ColumnInfo(name = "start_datetime") val startTimestamp: Date,

    // Date et heure de fin de la session de suivi (null si la session est encore en cours)
    @ColumnInfo(name = "end_datetime") var endTimestamp: Date? = null,

    // Durée totale du parcours en secondes
    @ColumnInfo(name = "duration") var duration: Int? = null,

    // Distance totale parcourue en mètres
    @ColumnInfo(name = "distance") var distance: Float? = null,

    // Nombre total de pas effectués pendant l'activité
    @ColumnInfo(name = "steps_count") var stepsCount: Int? = null,

    // Vitesse moyenne durant l'activité (m/s)
    @ColumnInfo(name = "average_speed") var averageSpeed: Float? = null,

    // Accélération moyenne durant l'activité (m/s²)
    @ColumnInfo(name = "average_acceleration") var averageAcceleration: Float? = null,

    // Nom personnalisé du parcours défini par l'utilisateur
    @ColumnInfo(name = "track_name") var trackName: String? = null,

    // Conditions météorologiques générales pendant l'activité (P. ex.,  "Ensoleillé", "Pluvieux")
    @ColumnInfo(name = "weather_condition") var weatherCondition: String? = null,

    // Température moyenne durant l'activité (°C)
    @ColumnInfo(name = "temperature") var temperature: Float? = null,

    // Humidité moyenne durant l'activité (%)
    @ColumnInfo(name = "average_humidity") var averageHumidity: Float? = null,

    // Vitesse moyenne du vent durant l'activité (m/s)
    @ColumnInfo(name = "average_wind_speed") var averageWindSpeed: Float? = null,

    // État d'expansion pour le UI d'hhistorique du parcours
    var isExpanded: Boolean = false
)
