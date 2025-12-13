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
    val source: String,      // "Manual", "Barcode", "USDA Database", etc.
    val timestamp: Long = System.currentTimeMillis(),
    
    // Additional fields for enhanced logging
    val isFavorite: Boolean = false,
    val notes: String = "",
    val fromRecipe: String? = null,  // If logged from a recipe
    val loggedTime: String? = null,  // Human-readable time (e.g., "10:30 AM")
    val imageUrl: String? = null     // Product/recipe image
)
