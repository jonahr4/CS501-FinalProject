package com.example.cs501_mealmapproject.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_logs")
data class FoodLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealName: String,
    val calories: Int,
    val protein: Float,      // grams
    val carbs: Float,        // grams
    val fat: Float,          // grams
    val fiber: Float = 0f,   // grams
    val sugar: Float = 0f,   // grams
    val sodium: Float = 0f,  // mg
    val servingSize: String = "1 serving",
    val servings: Float = 1f,
    val mealType: String = "SNACK", // BREAKFAST, LUNCH, DINNER, SNACK
    val source: String,      // "Manual", "Barcode", "USDA Database", "Recipe", etc.
    val timestamp: Long = System.currentTimeMillis(),
    
    // New fields for enhanced logging
    val isFavorite: Boolean = false,      // Mark as favorite for quick re-logging
    val notes: String = "",               // User notes about the meal
    val fromRecipe: String? = null,       // If logged from a recipe, store recipe name
    val loggedTime: String? = null,       // Time of day when eaten (e.g., "08:30 AM")
    val imageUrl: String? = null          // Optional image URL (from recipe or barcode)
)
