package com.example.cs501_mealmapproject.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    @Insert
    suspend fun insertFoodLog(foodLog: FoodLogEntity)
    
    @Update
    suspend fun updateFoodLog(foodLog: FoodLogEntity)

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    fun getAllFoodLogs(): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentFoodLogs(limit: Int = 10): Flow<List<FoodLogEntity>>
    
    // Get logs for a specific date (using timestamp range)
    @Query("SELECT * FROM food_logs WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getLogsForDate(startOfDay: Long, endOfDay: Long): Flow<List<FoodLogEntity>>
    
    // Get logs by meal type for a specific date
    @Query("SELECT * FROM food_logs WHERE mealType = :mealType AND timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getLogsByMealType(mealType: String, startOfDay: Long, endOfDay: Long): Flow<List<FoodLogEntity>>
    
    // Get favorite foods (foods marked as favorite)
    @Query("SELECT * FROM food_logs WHERE isFavorite = 1 GROUP BY mealName ORDER BY MAX(timestamp) DESC")
    fun getFavoriteFoods(): Flow<List<FoodLogEntity>>
    
    // Get frequently logged foods (grouped by name, sorted by frequency)
    @Query("SELECT *, COUNT(*) as count FROM food_logs GROUP BY mealName ORDER BY count DESC LIMIT :limit")
    fun getFrequentFoods(limit: Int = 20): Flow<List<FoodLogEntity>>
    
    // Get recent unique foods (for "Recent" tab - deduplicated)
    @Query("SELECT * FROM food_logs GROUP BY mealName ORDER BY MAX(timestamp) DESC LIMIT :limit")
    fun getRecentUniqueFoods(limit: Int = 30): Flow<List<FoodLogEntity>>
    
    // Toggle favorite status
    @Query("UPDATE food_logs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    
    // Update servings for an existing log
    @Query("UPDATE food_logs SET servings = :servings, calories = :calories, protein = :protein, carbs = :carbs, fat = :fat WHERE id = :id")
    suspend fun updateServings(id: Long, servings: Float, calories: Int, protein: Float, carbs: Float, fat: Float)
    
    // Update meal type for an existing log
    @Query("UPDATE food_logs SET mealType = :mealType WHERE id = :id")
    suspend fun updateMealType(id: Long, mealType: String)
    
    // Add notes to a log
    @Query("UPDATE food_logs SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)
    
    // Get a single food log by ID
    @Query("SELECT * FROM food_logs WHERE id = :id LIMIT 1")
    suspend fun getFoodLogById(id: Long): FoodLogEntity?

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFoodLog(id: Long)

    @Query("DELETE FROM food_logs")
    suspend fun deleteAllFoodLogs()
    
    // Get daily nutrition summary
    @Query("""
        SELECT 
            SUM(calories) as totalCalories,
            SUM(protein) as totalProtein,
            SUM(carbs) as totalCarbs,
            SUM(fat) as totalFat,
            SUM(fiber) as totalFiber,
            SUM(sugar) as totalSugar,
            SUM(sodium) as totalSodium
        FROM food_logs 
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay
    """)
    suspend fun getDailyNutritionSummary(startOfDay: Long, endOfDay: Long): DailyNutritionSummary?
}

data class DailyNutritionSummary(
    val totalCalories: Int?,
    val totalProtein: Float?,
    val totalCarbs: Float?,
    val totalFat: Float?,
    val totalFiber: Float?,
    val totalSugar: Float?,
    val totalSodium: Float?
)
