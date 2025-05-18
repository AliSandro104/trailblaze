package com.example.inf8405tf.data

import android.content.Context
import androidx.room.Room
import com.example.inf8405tf.data.dao.AppSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "trailblaze_database"
        )
            .build()
    }

    @Singleton
    @Provides
    fun provideAppSettings(database: AppDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }
}