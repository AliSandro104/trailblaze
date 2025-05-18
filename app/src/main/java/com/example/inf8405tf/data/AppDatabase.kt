package com.example.inf8405tf.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.inf8405tf.data.converters.DateConverter
import com.example.inf8405tf.data.dao.AppSettingsDao
import com.example.inf8405tf.data.dao.BatteryLogDao
import com.example.inf8405tf.data.dao.LocationPointDao
import com.example.inf8405tf.data.dao.SensorDataDao
import com.example.inf8405tf.data.dao.TrackDao
import com.example.inf8405tf.data.dao.UserDao
import com.example.inf8405tf.data.dao.WeatherDataDao
import com.example.inf8405tf.domain.AppSettings
import com.example.inf8405tf.domain.BatteryLog
import com.example.inf8405tf.domain.LocationPoint
import com.example.inf8405tf.domain.SensorData
import com.example.inf8405tf.domain.Track
import com.example.inf8405tf.domain.User
import com.example.inf8405tf.domain.WeatherData

@Database(
    entities = [
        User::class,
        Track::class,
        LocationPoint::class,
        SensorData::class,
        WeatherData::class,
        AppSettings::class,
        BatteryLog::class
    ],
    version = 19
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun trackDao(): TrackDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun sensorDataDao(): SensorDataDao
    abstract fun weatherDataDao(): WeatherDataDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun batteryLogDao(): BatteryLogDao
}