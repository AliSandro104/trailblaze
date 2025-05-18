package com.example.inf8405tf.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Classe pour observer les changements d'état du réseau (mode avion)
 * et déclencher les actions appropriées en fonction de l'état de la connectivité
 */
class NetworkStateObserver @Inject constructor(@ApplicationContext private val context: Context) {

    private var isRegistered = false
    private var callback: NetworkStateCallback? = null

    /**
     * Interface pour gérer les changements d'état du réseau.
     */
    interface NetworkStateCallback {
        fun onNetworkAvailable()
        fun onNetworkUnavailable()
    }

    /**
     * BroadcastReceiver pour écouter les changements d'état du réseau
     */
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            checkNetworkState()
        }
    }

    /**
     * Enregistre l'observateur avec un callback pour recevoir les notifications
     */
    fun register(callback: NetworkStateCallback) {
        this.callback = callback
        if (!isRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            }
            context.registerReceiver(networkReceiver, filter)
            isRegistered = true

            // Vérification initiale de l'état du réseau
            checkNetworkState()
        }
    }

    /**
     * Désenregistre l'observateur pour ne plus recevoir de notifications
     */
    fun unregister() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(networkReceiver)
            } catch (e: Exception) {
                Log.e("NetworkStateObserver", "Erreur lors de la désinscription : ${e.message}")
            }
            isRegistered = false
        }
        callback = null
    }

    /**
     * Vérifie l'état actuel du réseau et notifie le callback
     */
    fun checkNetworkState() {
        if (!isAirplaneModeOn()) {
            callback?.onNetworkAvailable()
        } else {
            callback?.onNetworkUnavailable()
        }
    }

    /**
     * Vérifie si le mode avion est activé
     */
    private fun isAirplaneModeOn(): Boolean {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
    }

    /**
     * Affiche un message pour indiquer l'état actuel du réseau
     */
    fun showNetworkStateToast(isAvailable: Boolean) {
        val message = if (isAvailable) {
            "Suivi repris - Réseau disponible"
        } else {
            "Suivi suspendu - Réseau indisponible"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
