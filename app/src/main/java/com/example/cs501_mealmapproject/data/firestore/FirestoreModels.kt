package com.example.cs501_mealmapproject.data.firestore

import com.google.firebase.firestore.PropertyName

/**
 * User profile stored in Firestore
 * Path: /users/{userId}/profile
 */
data class UserProfileFirestore(
    @PropertyName("id") val id: String = "",
    @PropertyName("displayName") val displayName: String = "",
    @PropertyName("email") val email: String? = null,
    @PropertyName("photoUrl") val photoUrl: String? = null,
    @PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("lastUpdatedAt") val lastUpdatedAt: Long = System.currentTimeMillis()
)

/**
 * Onboarding profile (health goals) stored in Firestore
 * Path: /users/{userId}/onboardingProfile
 */
data class OnboardingProfileFirestore(
    @PropertyName("calorieTarget") val calorieTarget: Int = 2000,
    @PropertyName("currentWeightLbs") val currentWeightLbs: Float = 160f,
    @PropertyName("goalWeightLbs") val goalWeightLbs: Float = 150f,
    @PropertyName("activityLevel") val activityLevel: String = "Moderate", // Store enum as string
    @PropertyName("lastUpdatedAt") val lastUpdatedAt: Long = System.currentTimeMillis()
)

/**
 * Food log entry stored in Firestore
 * Path: /users/{userId}/foodLogs/{logId}
 */
data class FoodLogFirestore(
    @PropertyName("id") val id: String = "",
    @PropertyName("mealName") val mealName: String = "",
    @PropertyName("calories") val calories: Int = 0,
    @PropertyName("protein") val protein: Float = 0f,
    @PropertyName("carbs") val carbs: Float = 0f,
    @PropertyName("fat") val fat: Float = 0f,
    @PropertyName("source") val source: String = "",
    @PropertyName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @PropertyName("lastUpdatedAt") val lastUpdatedAt: Long = System.currentTimeMillis(),
    @PropertyName("deleted") val deleted: Boolean = false // Soft delete for sync
)

/**
 * Meal plan entry stored in Firestore
 * Path: /users/{userId}/mealPlans/{planId}
 */
data class MealPlanFirestore(
    @PropertyName("id") val id: String = "",
    @PropertyName("date") val date: String = "", // ISO-8601 format: "2025-12-11"
    @PropertyName("mealType") val mealType: String = "", // "Breakfast", "Lunch", "Dinner"
    @PropertyName("recipeName") val recipeName: String = "",
    @PropertyName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @PropertyName("lastUpdatedAt") val lastUpdatedAt: Long = System.currentTimeMillis(),
    @PropertyName("deleted") val deleted: Boolean = false
)
