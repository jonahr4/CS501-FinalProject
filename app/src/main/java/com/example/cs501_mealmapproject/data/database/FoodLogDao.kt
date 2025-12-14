package com.example.cs501_mealmapproject.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
    
    // Get favorite foods
    @Query("SELECT * FROM food_logs WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteFoods(): Flow<List<FoodLogEntity>>
    
    // Get unique recent foods (for quick re-logging)
    @Query("SELECT * FROM food_logs GROUP BY mealName ORDER BY MAX(timestamp) DESC LIMIT :limit")
    fun getRecentUniqueFoods(limit: Int = 30): Flow<List<FoodLogEntity>>
    
    // Get logs by date range
    @Query("SELECT * FROM food_logs WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    fun getLogsForDate(startTime: Long, endTime: Long): Flow<List<FoodLogEntity>>
    
    // Get logs by meal type within date range
    @Query("SELECT * FROM food_logs WHERE mealType = :mealType AND timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    fun getLogsByMealType(mealType: String, startTime: Long, endTime: Long): Flow<List<FoodLogEntity>>
    
    // Set favorite status
    @Query("UPDATE food_logs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    
    // Update servings and recalculate nutrition
    @Query("UPDATE food_logs SET servings = :servings, calories = :calories, protein = :protein, carbs = :carbs, fat = :fat WHERE id = :id")
    suspend fun updateServings(id: Long, servings: Float, calories: Int, protein: Float, carbs: Float, fat: Float)
    
    // Update meal type
    @Query("UPDATE food_logs SET mealType = :mealType WHERE id = :id")
    suspend fun updateMealType(id: Long, mealType: String)
    
    // Update notes
    @Query("UPDATE food_logs SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)
    
    // Get frequently logged foods (by count)
    @Query("""
        SELECT *, COUNT(*) as logCount 
        FROM food_logs 
        GROUP BY mealName 
        ORDER BY logCount DESC 
        LIMIT :limit
    """)
    fun getFrequentFoods(limit: Int = 20): Flow<List<FoodLogEntity>>
    
    // Delete food logs that match a specific recipe, meal type, and date range
    // Used when removing a meal from the meal plan - also deletes the logged entries
    @Query("""
        DELETE FROM food_logs 
        WHERE fromRecipe = :recipeName 
        AND mealType = :mealType 
        AND timestamp >= :startTime 
        AND timestamp < :endTime
    """)
    suspend fun deleteByRecipeAndMealType(
        recipeName: String, 
        mealType: String, 
        startTime: Long, 
        endTime: Long
    ): Int
    
    // Delete all food logs for a specific recipe on a specific date (any meal type)
    @Query("""
        DELETE FROM food_logs 
        WHERE fromRecipe = :recipeName 
        AND timestamp >= :startTime 
        AND timestamp < :endTime
    """)
    suspend fun deleteByRecipeOnDate(
        recipeName: String, 
        startTime: Long, 
        endTime: Long
    ): Int
}
