package com.example.inf8405tf.utils

import android.content.Context
import android.content.SharedPreferences

object UserSession {
    private const val PREF_NAME = "user_session"
    private const val KEY_USERNAME = "username"
    private lateinit var sharedPreferences: SharedPreferences
    // https://developer.android.com/training/data-storage/shared-preferences
    // L'objet SharedPreferences pointe à un fichier contenant des paires key-value et fournit des méthodes simples pour lire et écrire
    // C'est pratique pour stocker le username de l'utilisateur pour la session actuelle, car c'est accessible facilement depuis n'importe quel contexte

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUsername(username: String) {
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply()
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}
