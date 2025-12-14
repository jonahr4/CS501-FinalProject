package com.example.cs501_mealmapproject.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_mealmapproject.data.database.AppDatabase
import com.example.cs501_mealmapproject.ui.onboarding.OnboardingProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class NutritionDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val foodLogDao = database.foodLogDao()

    private val _uiState = MutableStateFlow(NutritionDashboardUiState())
    val uiState: StateFlow<NutritionDashboardUiState> = _uiState.asStateFlow()

    private var currentProfile: OnboardingProfile? = null

    fun setOnboardingProfile(profile: OnboardingProfile?) {
        currentProfile = profile
        loadTodaysData()
    }

    private fun loadTodaysData() {
        viewModelScope.launch {
            // Get start and end of today in milliseconds
            val startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            val endOfDay = LocalDate.now()
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            foodLogDao.getAllFoodLogs().collect { allLogs ->
                val todaysLogs = allLogs.filter { it.timestamp in startOfDay until endOfDay }

                val totalCalories = todaysLogs.sumOf { it.calories }
                val totalProtein = todaysLogs.sumOf { it.protein.toDouble() }.toFloat()
                val totalCarbs = todaysLogs.sumOf { it.carbs.toDouble() }.toFloat()
                val totalFat = todaysLogs.sumOf { it.fat.toDouble() }.toFloat()

                val profile = currentProfile
                val activityLevel = profile?.activityLevel ?: com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Moderate
                
                // Calculate calorie goal based on weight goal (same logic as OnboardingScreen)
                val currentLbs = profile?.currentWeightLbs ?: 160f
                val goalLbs = profile?.goalWeightLbs ?: currentLbs
                
                // More realistic BMR estimate: ~10-11 cal per lb body weight
                val baseBMR = currentLbs * 10.5f
                
                // Activity multiplier for TDEE
                val activityMultiplier = when (activityLevel) {
                    com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Sedentary -> 1.2f
                    com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Light -> 1.375f
                    com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Moderate -> 1.55f
                    com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Active -> 1.725f
                }
                
                val tdee = baseBMR * activityMultiplier
                
                // Safe deficit/surplus: max 500-750 cal/day
                val weightDiff = goalLbs - currentLbs
                val calorieAdjustment = when {
                    weightDiff < -30 -> -750   // Larger deficit for significant weight loss
                    weightDiff < 0 -> -500     // Moderate deficit for weight loss
                    weightDiff > 30 -> 500     // Moderate surplus for significant weight gain
                    weightDiff > 0 -> 300      // Small surplus for lean gain
                    else -> 0                  // Maintenance
                }
                
                // Never go below 1500 calories
                val calorieGoal = (tdee + calorieAdjustment).toInt().coerceIn(1500, 4000)

                // Macro goals: protein scaled to weight (~0.8g per lb), fat 30% of calories, carbs fill the remainder
                val proteinGoal = (currentLbs * 0.8f).coerceAtLeast(0f)
                val fatGoal = (calorieGoal * 0.30f / 9f).toFloat()
                val remainingCalories = (calorieGoal - (proteinGoal * 4f + fatGoal * 9f)).coerceAtLeast(0f)
                val carbsGoal = (remainingCalories / 4f)

                _uiState.value = NutritionDashboardUiState(
                    caloriesConsumed = totalCalories,
                    caloriesGoal = calorieGoal,
                    proteinConsumed = totalProtein,
                    proteinGoal = proteinGoal,
                    carbsConsumed = totalCarbs,
                    carbsGoal = carbsGoal,
                    fatConsumed = totalFat,
                    fatGoal = fatGoal,
                    currentWeight = currentLbs,
                    goalWeight = goalLbs,
                    mealsLoggedToday = todaysLogs.size,
                    activityLevel = activityLevel,
                    tdee = tdee.toInt(),
                    calorieAdjustment = calorieAdjustment
                )
            }
        }
    }
}

data class NutritionDashboardUiState(
    val caloriesConsumed: Int = 0,
    val caloriesGoal: Int = 2000,
    val proteinConsumed: Float = 0f,
    val proteinGoal: Float = 150f,
    val carbsConsumed: Float = 0f,
    val carbsGoal: Float = 200f,
    val fatConsumed: Float = 0f,
    val fatGoal: Float = 67f,
    val currentWeight: Float = 0f,
    val goalWeight: Float = 0f,
    val mealsLoggedToday: Int = 0,
    val activityLevel: com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel = com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Moderate,
    val tdee: Int = 2000,
    val calorieAdjustment: Int = 0
)
