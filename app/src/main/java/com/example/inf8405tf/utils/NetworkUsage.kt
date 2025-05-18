package com.example.inf8405tf.utils

import android.net.TrafficStats
import java.text.DecimalFormat

object NetworkUsage {

    // https://developer.android.com/reference/android/net/TrafficStats
    private var previousTxBytes = TrafficStats.getTotalTxBytes() // Retourne le nombre de bytes transmis depuis le début de l'application
    private var previousRxBytes = TrafficStats.getTotalRxBytes() // Retourne le nombre de bytes reçus depuis le début de l'application
    private var previousTime = System.currentTimeMillis()

    fun getFormattedUplinkDownlink(): Pair<String, String> {
        val currentTxBytes = TrafficStats.getTotalTxBytes()
        val currentRxBytes = TrafficStats.getTotalRxBytes()
        val currentTime = System.currentTimeMillis()

        val timeDiff = (currentTime - previousTime) / 1000.0 // On divise par 1000 pour convertir de millisecondes en secondes

        val uplink = (currentTxBytes - previousTxBytes) / timeDiff // Nous donne le taux de variation qui est le uplink ici
        val downlink = (currentRxBytes - previousRxBytes) / timeDiff // Nous donne le taux de variation qui est le downlink ici

        // Mise à jour des variables pour la prochaine itération
        previousTxBytes = currentTxBytes
        previousRxBytes = currentRxBytes
        previousTime = currentTime

        return Pair(
            formatBytesPerSecond(uplink),
            formatBytesPerSecond(downlink)
        )
    }

    // Formatte le string à afficher pour l'uplink/downlink en B/s, KB/s ou MB/s dépendamment de la taille
    private fun formatBytesPerSecond(bytesPerSec: Double): String {
        val df = DecimalFormat("#.##")
        return when {
            bytesPerSec >= 1024 * 1024 -> "${df.format(bytesPerSec / (1024 * 1024))} MB/s"
            bytesPerSec >= 1024 -> "${df.format(bytesPerSec / 1024)} KB/s"
            else -> "${df.format(bytesPerSec)} B/s"
        }
    }
}