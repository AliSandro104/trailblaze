package com.example.inf8405tf.service

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject

class WeatherService @Inject constructor() {
    private val client = OkHttpClient()
    private val apiKey = "25ec20e6b494dd9443c5ceadfc0b6f54"

    fun fetchTemperature(
        lat: Double,
        lon: Double,
        maxRetries: Int = 5,
        retryDelayMillis: Long = 2000L,
        callback: (Float?, String?, Float?, Float?, Boolean) -> Unit
    ) {
        var retries = 0
        val mainHandler = Handler(Looper.getMainLooper())

        fun attemptFetch() {
            val url =
                "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&units=metric&lang=fr&appid=$apiKey"

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.w("API Météo", "Échec de la requête : ${e.stackTraceToString()}")
                    retries++
                    if (retries < maxRetries) {
                        Log.w("API Météo", "Nouvelle tentative dans 2 secondes (Retry: $retries/$maxRetries)...")
                        mainHandler.postDelayed({ attemptFetch() }, retryDelayMillis)
                    } else {
                        Log.e("API Météo", "Échec après $maxRetries tentatives.")
                        // Échec
                        mainHandler.post { callback(null, null, null, null, false) }
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body?.use { body ->
                            val json = JSONObject(body.string())

                            val temp = json.getJSONObject("main").getDouble("temp").toFloat()
                            val humidity = json.getJSONObject("main").getDouble("humidity").toFloat()
                            val windSpeed = json.getJSONObject("wind").getDouble("speed").toFloat()

                            val weatherArray = json.getJSONArray("weather")
                            val description = if (weatherArray.length() > 0) {
                                weatherArray.getJSONObject(0).getString("description")
                            } else {
                                null
                            }

                            Log.d(
                                "API Météo",
                                "Température: $temp, Description: $description, Humidité: $humidity, Vent: $windSpeed"
                            )
                            // Succès
                            mainHandler.post { callback(temp, description, humidity, windSpeed, true) }
                        }
                    } else {
                        Log.w("API Météo", "Réponse non réussie, code: ${response.code}")
                        retries++
                        if (retries < maxRetries) {
                            Log.w("API Météo", "Nouvelle tentative dans 2 secondes (Retry: $retries/$maxRetries)...")
                            // Utiliser le mainHandler pour postDelayed
                            mainHandler.postDelayed({ attemptFetch() }, retryDelayMillis)
                        } else {
                            Log.e("API Météo", "Échec après $maxRetries tentatives.")
                            // Échec
                            mainHandler.post { callback(null, null, null, null, false) }
                        }
                    }
                }
            })
        }

        // Lancer la première tentative
        attemptFetch()
    }
}
