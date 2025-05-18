package com.example.inf8405tf.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.inf8405tf.domain.LocationPoint

@Dao
interface LocationPointDao {
    @Query("SELECT * FROM location_point WHERE track_id = :trackId")
    suspend fun getLocationPointsForTrack(trackId: String): List<LocationPoint>

    @Insert
    suspend fun insertLocationPoint(locationPoint: LocationPoint): Long

    @Insert
    suspend fun insertLocationPoints(locationPoints: List<LocationPoint>)
}