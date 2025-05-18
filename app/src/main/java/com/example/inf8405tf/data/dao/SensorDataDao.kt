package com.example.inf8405tf.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.inf8405tf.domain.SensorData

@Dao
interface SensorDataDao {
    @Query("SELECT * FROM sensor_data WHERE track_id = :trackId")
    suspend fun getSensorDataForTrack(trackId: String): List<SensorData>

    @Insert
    suspend fun insertSensorData(sensorData: SensorData): Long

    @Insert
    suspend fun insertSensorDataList(sensorDataList: List<SensorData>)
}