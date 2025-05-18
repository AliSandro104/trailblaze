package com.example.inf8405tf.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user")
data class User(
    // Nom d'utilisateur choisi par l'utilisateur
    @PrimaryKey(autoGenerate = false) val username: String,

    // Adresse e-mail de l'utilisateur
    @ColumnInfo(name = "email") val email: String,

    // Version hashed du mot de passe de l'utilisateur
    @ColumnInfo(name = "password_hash") val passwordHash: String,

    // Chemin vers la photo de profil de l'utilisateur
    @ColumnInfo(name = "profile_picture_path") val profilePicturePath: String?,

    // Date et heure de la création du compte utilisateur
    @ColumnInfo(name = "creation_date") val creationTimestamp: Date,

    // Indique si l'utilisateur est authentifié dans l'application
    @ColumnInfo(name = "authenticated") val authenticated: Boolean = false,

    // Genre de l'utilisateur (P. ex., : "Homme", "Femme", ou null)
    @ColumnInfo(name = "gender") val gender: String?
)

