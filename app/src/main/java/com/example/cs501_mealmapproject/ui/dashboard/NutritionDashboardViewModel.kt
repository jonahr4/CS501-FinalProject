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
                val baseCalorieGoal = profile?.calorieTarget ?: 2000
                val activityLevel = profile?.activityLevel ?: com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Moderate
                val activityFactor = when (activityLevel) {
                    com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Sedentary -> 0.9f
                    com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Light -> 1.0f
                    com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Moderate -> 1.05f
                    com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel.Active -> 1.15f
                }
                val calorieGoal = (baseCalorieGoal * activityFactor).toInt()

                // Calculate macro goals from calorie goal (typical: 30% protein, 40% carbs, 30% fat)
                val proteinGoal = (calorieGoal * 0.30 / 4).toFloat()  // 4 calories per gram
                val carbsGoal = (calorieGoal * 0.40 / 4).toFloat()
                val fatGoal = (calorieGoal * 0.30 / 9).toFloat()      // 9 calories per gram

                _uiState.value = NutritionDashboardUiState(
                    caloriesConsumed = totalCalories,
                    caloriesGoal = calorieGoal,
                    proteinConsumed = totalProtein,
                    proteinGoal = proteinGoal,
                    carbsConsumed = totalCarbs,
                    carbsGoal = carbsGoal,
                    fatConsumed = totalFat,
                    fatGoal = fatGoal,
                    currentWeight = profile?.currentWeightLbs ?: 0f,
                    goalWeight = profile?.goalWeightLbs ?: 0f,
                    mealsLoggedToday = todaysLogs.size,
                    activityLevel = activityLevel,
                    baseCalorieGoal = baseCalorieGoal,
                    activityFactor = activityFactor
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
    val baseCalorieGoal: Int = 2000,
    val activityFactor: Float = 1.0f
)
