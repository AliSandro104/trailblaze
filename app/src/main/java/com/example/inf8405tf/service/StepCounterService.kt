package com.example.inf8405tf.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Service pour compter le nombre de pas effectués par l'utilisateur
 */
class StepCounterService @Inject constructor(@ApplicationContext private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    // valeur de référence du nombre de pas
    private var baseStepCount: Float = -1f
    // Dernière valeur du compteur de pas système enregistrée
    private var lastStepCount: Float = 0f
    // Nombre total de pas comptés pour l'utilisateur pendant les périodes actives
    private var userStepCount: Int = 0
    private var isCountingSteps = false

    // Callback appelée lorsque le nombre de pas change
    var onStepCountChanged: ((Int) -> Unit)? = null

    /**
     * Démarre le comptage des pas.
     */
    fun start() {
        if (stepCounterSensor == null) {
            Log.e("StepCounterService", "Aucun capteur de compteur de pas détecté sur cet appareil")
            Toast.makeText(context, "Aucun capteur de compteur de pas détecté", Toast.LENGTH_SHORT).show()
            return
        }

        isCountingSteps = true

        sensorManager.registerListener(
            this,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        Log.d("StepCounterService", "Compteur de pas démarré")
    }

    /**
     * Arrête le comptage des pas.
     */
    fun stop() {
        isCountingSteps = false
        Log.d("StepCounter", "Compteur de pas arrêté à $userStepCount pas")
    }

    /**
     * Appelé lorsque le capteur détecte un changement
     */
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val systemStepCount = it.values[0]

                // Initialisation à la première lecture du capteur.
                // Permet de définir la valeur initiale du nombre de pas du système
                // quand le comptage niveau applicatif débute
                if (baseStepCount < 0) {
                    baseStepCount = systemStepCount
                    lastStepCount = systemStepCount
                    Log.d("StepCounterService", "Valeur initiale du compteur de pas système : $baseStepCount")
                    return
                }

                // Calcul des pas effectués depuis la dernière lecture
                val stepsDelta = systemStepCount - lastStepCount
                lastStepCount = systemStepCount

                // Incrémenter le nombre de pas de la session de parcours uniquement
                // si la session est active (pas en pause) et qu'il y a eu des nouveaux pas de capturer
                if (isCountingSteps && stepsDelta > 0) {
                    userStepCount += stepsDelta.toInt()
                    onStepCountChanged?.invoke(userStepCount)
                    Log.d("StepCounterService", "Nombre total de pas utilisateur : $userStepCount")
                } else {
                    Log.d("StepCounterService", "Pas ignorés : ${stepsDelta.toInt()} (en pause)")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Pas utilisé
    }

    /**
     * Désenregistre le listener
     */
    fun cleanup() {
        sensorManager.unregisterListener(this)
    }
}