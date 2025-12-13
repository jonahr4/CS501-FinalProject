package com.example.cs501_mealmapproject.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeCacheDao {
    
    @Query("SELECT * FROM recipe_cache WHERE recipeName = :name LIMIT 1")
    suspend fun getRecipe(name: String): RecipeCacheEntity?
    
    @Query("SELECT * FROM recipe_cache WHERE recipeName = :name LIMIT 1")
    fun getRecipeFlow(name: String): Flow<RecipeCacheEntity?>
    
    @Query("SELECT * FROM recipe_cache WHERE isFavorite = 1 ORDER BY recipeName ASC")
    fun getFavoriteRecipes(): Flow<List<RecipeCacheEntity>>
    
    @Query("SELECT * FROM recipe_cache ORDER BY cachedAt DESC")
    fun getAllCachedRecipes(): Flow<List<RecipeCacheEntity>>
    
    @Query("SELECT * FROM recipe_cache ORDER BY cachedAt DESC LIMIT :limit")
    fun getRecentRecipes(limit: Int = 20): Flow<List<RecipeCacheEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeCacheEntity)
    
    @Update
    suspend fun updateRecipe(recipe: RecipeCacheEntity)
    
    @Query("UPDATE recipe_cache SET isFavorite = :isFavorite WHERE recipeName = :name")
    suspend fun setFavorite(name: String, isFavorite: Boolean)
    
    @Query("UPDATE recipe_cache SET totalCalories = :calories, totalProtein = :protein, totalCarbs = :carbs, totalFat = :fat, totalFiber = :fiber, totalSugar = :sugar, totalSodium = :sodium, isNutritionCalculated = 1, nutritionCalculatedAt = :calculatedAt WHERE recipeName = :name")
    suspend fun updateNutrition(
        name: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float,
        sugar: Float,
        sodium: Float,
        calculatedAt: Long = System.currentTimeMillis()
    )
    
    @Query("DELETE FROM recipe_cache WHERE recipeName = :name")
    suspend fun deleteRecipe(name: String)
    
    @Query("DELETE FROM recipe_cache WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)
    
    @Query("SELECT EXISTS(SELECT 1 FROM recipe_cache WHERE recipeName = :name)")
    suspend fun isRecipeCached(name: String): Boolean
}
