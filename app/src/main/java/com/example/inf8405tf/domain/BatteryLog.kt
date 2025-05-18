package com.example.inf8405tf.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "battery_logs")
data class BatteryLog(
    // Identifiant unique pour ce journal de niveau de batterie
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // Date et heure où ces données ont été enregistrées
    val timestamp: Date,

    // Niveau de la batterie (%)
    val level: Int
)
