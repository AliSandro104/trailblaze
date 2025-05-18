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
import kotlin.math.sqrt

class AccelerationService @Inject constructor(@ApplicationContext private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var accelerationListener: ((Float) -> Unit)? = null

    fun start(listener: (Float) -> Unit) {
        if (accelerometer == null) {
            Log.e("AccelerationService", "Aucun capteur d'accélération détecté sur cet appareil")
            Toast.makeText(context, "Aucun capteur d'accélération détecté", Toast.LENGTH_SHORT).show()
            return
        }

        accelerationListener = listener
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val x = it.values[0]
            val y = it.values[1]
            val z = it.values[2]

            // Calcul la norme de l'acceleration
            val acceleration = sqrt(x * x + y * y + z * z)

            accelerationListener?.invoke(acceleration)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
