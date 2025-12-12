package com.example.cs501_mealmapproject.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(foodLog: FoodLogEntity): Long

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    fun getAllFoodLogs(): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentFoodLogs(limit: Int = 10): Flow<List<FoodLogEntity>>

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFoodLog(id: Long)

    @Query("DELETE FROM food_logs")
    suspend fun deleteAllFoodLogs()
}
