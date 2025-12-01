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
    val source: String,      // "Manual", "Barcode", etc.
    val timestamp: Long = System.currentTimeMillis()
)
