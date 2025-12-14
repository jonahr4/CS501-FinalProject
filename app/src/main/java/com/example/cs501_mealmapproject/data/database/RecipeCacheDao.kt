package com.example.cs501_mealmapproject.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecipeCacheDao {
    
    @Query("SELECT * FROM recipe_cache WHERE recipeName = :name LIMIT 1")
    suspend fun getRecipe(name: String): RecipeCacheEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM recipe_cache WHERE recipeName = :name)")
    suspend fun isRecipeCached(name: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeCacheEntity)
    
    @Update
    suspend fun updateRecipe(recipe: RecipeCacheEntity)
    
    @Query("""
        UPDATE recipe_cache SET 
            totalCalories = :calories,
            totalProtein = :protein,
            totalCarbs = :carbs,
            totalFat = :fat,
            totalFiber = :fiber,
            totalSugar = :sugar,
            totalSodium = :sodium,
            isNutritionCalculated = 1
        WHERE recipeName = :name
    """)
    suspend fun updateNutrition(
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float,
        sugar: Float,
        sodium: Float
    )
    
    @Query("UPDATE recipe_cache SET isNutritionCalculated = 0 WHERE recipeName = :name")
    suspend fun resetNutritionStatus(name: String)
    
    @Query("UPDATE recipe_cache SET isNutritionCalculated = 0 WHERE totalCalories = 0")
    suspend fun resetAllZeroCalorieRecipes()
    
    @Query("DELETE FROM recipe_cache WHERE recipeName = :name")
    suspend fun deleteRecipe(name: String)
    
    @Query("DELETE FROM recipe_cache")
    suspend fun deleteAllRecipes()
    
    @Query("DELETE FROM recipe_cache WHERE totalCalories > :maxCalories")
    suspend fun deleteRecipesWithHighCalories(maxCalories: Int)
    
    @Query("DELETE FROM recipe_cache WHERE cachedAt < :beforeTime")
    suspend fun deleteOldCache(beforeTime: Long)
    
    @Query("SELECT * FROM recipe_cache ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getRecentlyViewedRecipes(limit: Int = 20): List<RecipeCacheEntity>
}
