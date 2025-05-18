package com.example.inf8405tf.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "app_settings")
data class AppSettings(
    // Si l'application est initialisé
    var isInitialized: Boolean = false,

    // Date et heure de l'initialisation de l'application
    var initializationTimestamp: Date,

    // Identifiant unique et statique de l'entité
    @PrimaryKey
    val id: Int = 1
)
