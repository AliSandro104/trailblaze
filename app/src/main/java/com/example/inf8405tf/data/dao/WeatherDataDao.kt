package com.example.inf8405tf.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.inf8405tf.domain.WeatherData

@Dao
interface WeatherDataDao {
    @Query("SELECT * FROM weather_data WHERE track_id = :trackId")
    suspend fun getWeatherDataForTrack(trackId: String): List<WeatherData>

    @Insert
    suspend fun insertWeatherData(weatherData: WeatherData): Long

    @Insert
    suspend fun insertWeatherDataList(weatherDataList: List<WeatherData>)
}