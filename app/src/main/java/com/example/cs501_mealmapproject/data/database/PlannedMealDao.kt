package com.example.cs501_mealmapproject.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedMealDao {
    
    @Query("SELECT * FROM planned_meals WHERE userId = :userId ORDER BY date ASC, mealType ASC")
    fun getPlannedMealsForUser(userId: String): Flow<List<PlannedMealEntity>>
    
    @Query("SELECT * FROM planned_meals WHERE userId = :userId AND date = :date")
    fun getPlannedMealsForDate(userId: String, date: String): Flow<List<PlannedMealEntity>>
    
    @Query("SELECT * FROM planned_meals WHERE userId = :userId AND date = :date AND mealType = :mealType LIMIT 1")
    suspend fun getPlannedMeal(userId: String, date: String, mealType: String): PlannedMealEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedMeal(meal: PlannedMealEntity): Long
    
    @Update
    suspend fun updatePlannedMeal(meal: PlannedMealEntity)
    
    @Delete
    suspend fun deletePlannedMeal(meal: PlannedMealEntity)
    
    @Query("DELETE FROM planned_meals WHERE userId = :userId AND date = :date AND mealType = :mealType")
    suspend fun removePlannedMeal(userId: String, date: String, mealType: String)
    
    @Query("DELETE FROM planned_meals WHERE userId = :userId AND date < :date")
    suspend fun deleteOldMeals(userId: String, date: String)
    
    // Get all planned meals for a date range (useful for weekly view)
    @Query("SELECT * FROM planned_meals WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getPlannedMealsForDateRange(userId: String, startDate: String, endDate: String): Flow<List<PlannedMealEntity>>
}
