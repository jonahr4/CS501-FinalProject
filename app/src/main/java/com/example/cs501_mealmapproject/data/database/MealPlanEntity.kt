package com.example.cs501_mealmapproject.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing meal plan assignments
 * Each row represents one meal slot (e.g., Monday Breakfast = Oatmeal)
 */
@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,          // ISO-8601 format: "2025-12-11"
    val mealType: String,      // "Breakfast", "Lunch", "Dinner"
    val recipeName: String,     // Name of the recipe/meal
    val timestamp: Long = System.currentTimeMillis()
)
