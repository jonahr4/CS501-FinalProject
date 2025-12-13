package com.example.cs501_mealmapproject.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store planned meals with their full recipe data including estimated nutrition.
 * This allows us to log planned meals directly without re-fetching from API.
 */
@Entity(tableName = "planned_meals")
data class PlannedMealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // ISO date string (e.g., "2025-12-12")
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snack"
    val recipeName: String,
    val recipeImageUrl: String? = null,
    val ingredients: String, // JSON list of ingredients
    val instructions: String = "",
    val sourceUrl: String? = null,
    
    // Estimated nutrition (calculated from ingredients)
    val estimatedCalories: Int = 0,
    val estimatedProtein: Float = 0f,
    val estimatedCarbs: Float = 0f,
    val estimatedFat: Float = 0f,
    val estimatedFiber: Float = 0f,
    val estimatedSugar: Float = 0f,
    val estimatedSodium: Float = 0f,
    
    // Serving info
    val servings: Int = 1, // Recipe usually serves X people
    
    // User-specific
    val userId: String? = null,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
