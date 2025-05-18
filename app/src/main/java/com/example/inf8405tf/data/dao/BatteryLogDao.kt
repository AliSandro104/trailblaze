package com.example.inf8405tf.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.inf8405tf.domain.BatteryLog

@Dao
interface BatteryLogDao {
    @Insert
    suspend fun insertLog(log: BatteryLog)

    @Query("DELETE FROM battery_logs")
    suspend fun clearLogs()

    @Query("SELECT * FROM battery_logs ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstLog(): BatteryLog?

    @Query("SELECT * FROM battery_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLog(): BatteryLog?
}
