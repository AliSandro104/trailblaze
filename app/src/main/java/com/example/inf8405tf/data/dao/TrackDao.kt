package com.example.inf8405tf.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.inf8405tf.domain.Track

@Dao
interface TrackDao {
    @Query("SELECT * FROM track WHERE username = :username AND track_status = 'Started' ORDER BY start_datetime DESC LIMIT 1")
    suspend fun getStartedTrackForUser(username: String): Track?

    @Query("SELECT * FROM track WHERE username = :username AND track_status = 'Completed'")
    suspend fun getCompletedTracksForUser(username: String): List<Track>

    @Insert
    suspend fun insertTrack(track: Track): Long

    @Update
    suspend fun updateTrack(track: Track)

    @Delete
    suspend fun deleteTrack(track: Track)

    @Query("DELETE FROM track WHERE username = :username AND track_status = 'Started'")
    suspend fun deleteStartedTrackForUser(username: String): Int
}