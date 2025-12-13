package com.example.cs501_mealmapproject.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Caches recipe data from TheMealDB API along with calculated nutrition.
 * This allows quick access to nutrition data when logging meals from the plan.
 */
@Entity(tableName = "recipe_cache")
data class RecipeCacheEntity(
    @PrimaryKey
    val recipeName: String,
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val category: String? = null,
    val area: String? = null,
    @ColumnInfo(name = "ingredientList")
    val ingredients: List<IngredientWithMeasure> = emptyList(),
    val estimatedServings: Int = 4,
    
    // Calculated nutrition (total for entire recipe)
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val totalFiber: Float = 0f,
    val totalSugar: Float = 0f,
    val totalSodium: Float = 0f,
    
    val isNutritionCalculated: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Represents an ingredient with its measurement
 */
data class IngredientWithMeasure(
    val ingredient: String,
    val measure: String
)
