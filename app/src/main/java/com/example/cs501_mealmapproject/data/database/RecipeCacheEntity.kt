package com.example.cs501_mealmapproject.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Cached recipe data for quick access when logging meals.
 * This stores full recipe info including calculated nutrition.
 */
@Entity(tableName = "recipe_cache")
@TypeConverters(RecipeConverters::class)
data class RecipeCacheEntity(
    @PrimaryKey
    val recipeName: String,
    
    // Recipe metadata
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val category: String? = null,
    val area: String? = null,
    
    // Ingredients with measures (JSON serialized)
    val ingredients: List<IngredientWithMeasure> = emptyList(),
    
    // Calculated nutrition for the entire recipe
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val totalFiber: Float = 0f,
    val totalSugar: Float = 0f,
    val totalSodium: Float = 0f,
    
    // Servings info (estimated)
    val estimatedServings: Int = 4,
    
    // Timestamps
    val cachedAt: Long = System.currentTimeMillis(),
    val nutritionCalculatedAt: Long? = null,
    
    // Flags
    val isNutritionCalculated: Boolean = false,
    val isFavorite: Boolean = false
)

data class IngredientWithMeasure(
    val ingredient: String,
    val measure: String,
    // Nutrition per this ingredient amount (if calculated)
    val calories: Int? = null,
    val protein: Float? = null,
    val carbs: Float? = null,
    val fat: Float? = null
)

class RecipeConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromIngredientList(ingredients: List<IngredientWithMeasure>): String {
        return gson.toJson(ingredients)
    }
    
    @TypeConverter
    fun toIngredientList(json: String): List<IngredientWithMeasure> {
        val type = object : TypeToken<List<IngredientWithMeasure>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
