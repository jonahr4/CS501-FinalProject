package com.example.cs501_mealmapproject.data.firestore

import com.example.cs501_mealmapproject.data.database.FoodLogEntity
import com.example.cs501_mealmapproject.data.database.MealPlanEntity
import com.example.cs501_mealmapproject.ui.model.AppUser
import com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel
import com.example.cs501_mealmapproject.ui.onboarding.OnboardingProfile

/**
 * Extension functions to convert between Room/UI models and Firestore models
 */

// ========== AppUser <-> UserProfileFirestore ==========

fun AppUser.toFirestore(): UserProfileFirestore {
    return UserProfileFirestore(
        id = this.id,
        displayName = this.displayName,
        email = this.email,
        photoUrl = this.photoUrl,
        createdAt = System.currentTimeMillis(),
        lastUpdatedAt = System.currentTimeMillis()
    )
}

fun UserProfileFirestore.toAppUser(): AppUser {
    return AppUser(
        id = this.id,
        displayName = this.displayName,
        email = this.email,
        photoUrl = this.photoUrl
    )
}

// ========== OnboardingProfile <-> OnboardingProfileFirestore ==========

fun OnboardingProfile.toFirestore(): OnboardingProfileFirestore {
    return OnboardingProfileFirestore(
        calorieTarget = this.calorieTarget,
        currentWeightLbs = this.currentWeightLbs,
        goalWeightLbs = this.goalWeightLbs,
        activityLevel = this.activityLevel.name, // Store enum as string
        lastUpdatedAt = System.currentTimeMillis()
    )
}

fun OnboardingProfileFirestore.toOnboardingProfile(): OnboardingProfile {
    return OnboardingProfile(
        calorieTarget = this.calorieTarget,
        currentWeightLbs = this.currentWeightLbs,
        goalWeightLbs = this.goalWeightLbs,
        activityLevel = try {
            ActivityLevel.valueOf(this.activityLevel)
        } catch (e: IllegalArgumentException) {
            ActivityLevel.Moderate // Default fallback
        }
    )
}

// ========== FoodLogEntity <-> FoodLogFirestore ==========

fun FoodLogEntity.toFirestore(): FoodLogFirestore {
    return FoodLogFirestore(
        id = this.id.toString(),
        mealName = this.mealName,
        calories = this.calories,
        protein = this.protein,
        carbs = this.carbs,
        fat = this.fat,
        isFavorite = this.isFavorite,
        source = this.source,
        timestamp = this.timestamp,
        lastUpdatedAt = System.currentTimeMillis(),
        deleted = false
    )
}

fun FoodLogFirestore.toEntity(): FoodLogEntity {
    return FoodLogEntity(
        id = this.id.toLongOrNull() ?: 0L,
        mealName = this.mealName,
        calories = this.calories,
        protein = this.protein,
        carbs = this.carbs,
        fat = this.fat,
        isFavorite = this.isFavorite,
        source = this.source,
        timestamp = this.timestamp
    )
}

// ========== MealPlanEntity <-> MealPlanFirestore ==========

fun MealPlanEntity.toFirestore(planId: String = "${this.date}_${this.mealType}"): MealPlanFirestore {
    return MealPlanFirestore(
        id = planId,
        date = this.date,
        mealType = this.mealType,
        recipeName = this.recipeName,
        timestamp = this.timestamp,
        lastUpdatedAt = System.currentTimeMillis(),
        deleted = false
    )
}

fun MealPlanFirestore.toEntity(): MealPlanEntity {
    return MealPlanEntity(
        id = this.id.toLongOrNull() ?: 0L,
        date = this.date,
        mealType = this.mealType,
        recipeName = this.recipeName,
        timestamp = this.timestamp
    )
}
