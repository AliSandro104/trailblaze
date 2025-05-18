package com.example.inf8405tf.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.inf8405tf.domain.User

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM user WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM user WHERE authenticated = 1 LIMIT 1")
    suspend fun getAuthenticatedUser(): User?

    @Query("UPDATE user SET authenticated = 0 WHERE authenticated = 1")
    suspend fun logoutUser()
}