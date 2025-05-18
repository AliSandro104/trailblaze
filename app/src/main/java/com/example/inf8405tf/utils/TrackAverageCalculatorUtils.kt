package com.example.inf8405tf.utils

import com.example.inf8405tf.domain.LocationPoint
import com.example.inf8405tf.domain.SensorData
import com.example.inf8405tf.domain.WeatherData

object TrackAverageCalculatorUtils {
    fun calculateAverageSpeed(locationPoints: List<LocationPoint>): Float {
        if (locationPoints.isEmpty()) {
            return 0.0f
        }

        val totalSpeed = locationPoints.sumOf { it.speed.toDouble() }
        return (totalSpeed / locationPoints.size).toFloat()
    }

    fun calculateAverageAccelerometer(sensorDataList: List<SensorData>): Float {
        val accelerometerValues = sensorDataList.mapNotNull { it.accelerometer }
        return if (accelerometerValues.isNotEmpty()) accelerometerValues.average().toFloat() else 0f
    }

    fun calculateAverageTemperature(weatherDataList: List<WeatherData>): Float {
        val temperatureValues = weatherDataList.mapNotNull { it.temperature }
        return if (temperatureValues.isNotEmpty()) temperatureValues.average().toFloat() else 0f
    }

    fun calculateAverageHumidity(weatherDataList: List<WeatherData>): Float {
        val humidityValues = weatherDataList.mapNotNull { it.humidity }
        return if (humidityValues.isNotEmpty()) humidityValues.average().toFloat() else 0f
    }

    fun calculateAverageWindSpeed(weatherDataList: List<WeatherData>): Float {
        val windSpeedValues = weatherDataList.mapNotNull { it.windSpeed }
        return if (windSpeedValues.isNotEmpty()) windSpeedValues.average().toFloat() else 0f
    }

    fun calculateMostFrequentWeatherCondition(weatherDataList: List<WeatherData>): String? {
        return weatherDataList
            .mapNotNull { it.weatherCondition }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }
}