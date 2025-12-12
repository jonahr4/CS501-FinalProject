package com.example.cs501_mealmapproject.ui.mealplan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.cs501_mealmapproject.data.auth.AuthRepository
import com.example.cs501_mealmapproject.data.database.MealPlanEntity
import com.example.cs501_mealmapproject.data.repository.MealPlanRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MealPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val mealPlanRepository = MealPlanRepository(application)
    private val authRepository = AuthRepository(application)

    private val _uiState = MutableStateFlow(MealPlanUiState(plan = generateWeekPlan()))
    val uiState: StateFlow<MealPlanUiState> = _uiState.asStateFlow()

    init {
        loadMealPlansFromDatabase()
        syncFromFirestore()
    }

    private fun loadMealPlansFromDatabase() {
        viewModelScope.launch {
            mealPlanRepository.getAllMealPlans().collect { entities ->
                // Group entities by date
                val planMap = entities.groupBy { it.date }

                // Update UI state with loaded plans
                _uiState.update { state ->
                    val updatedPlan = state.plan.map { day ->
                        val dateStr = day.date.toString()
                        val plansForDate = planMap[dateStr] ?: emptyList()

                        val updatedMeals = day.meals.map { slot ->
                            val savedPlan = plansForDate.find { it.mealType == slot.mealType }
                            if (savedPlan != null) {
                                slot.copy(recipeName = savedPlan.recipeName)
                            } else {
                                slot
                            }
                        }

                        day.copy(meals = updatedMeals)
                    }
                    state.copy(plan = updatedPlan)
                }
            }
        }
    }

    private fun syncFromFirestore() {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid
            if (userId != null) {
                mealPlanRepository.syncFromFirestore(userId)
                Log.d("MealPlanViewModel", "Synced meal plans from Firestore")
            }
        }
    }

    fun assignMeal(date: LocalDate, mealType: String, recipeName: String) {
        _uiState.update { state ->
            val updatedPlan = state.plan.map { day ->
                if (day.date == date) {
                    val updatedMeals = day.meals.map { slot ->
                        if (slot.mealType == mealType) slot.copy(recipeName = recipeName) else slot
                    }
                    day.copy(meals = updatedMeals)
                } else {
                    day
                }
            }
            state.copy(plan = updatedPlan)
        }

        savePlanToDatabase(date, mealType, recipeName)
    }

    fun removeMeal(date: LocalDate, mealType: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid
            if (userId != null) {
                mealPlanRepository.deleteMealPlan(userId, date.toString(), mealType)
            }
        }
        assignMeal(date, mealType, "Tap to add a recipe")
    }

    private fun savePlanToDatabase(date: LocalDate, mealType: String, recipeName: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid
            if (userId != null) {
                val entity = MealPlanEntity(
                    date = date.toString(),
                    mealType = mealType,
                    recipeName = recipeName
                )
                mealPlanRepository.saveMealPlan(userId, entity)
                Log.d("MealPlanViewModel", "Saved meal plan: $date $mealType")
            }
        }
    }

    private fun generateWeekPlan(startDate: LocalDate = LocalDate.now()): List<DailyMealPlan> {
        return (0 until 7).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            DailyMealPlan(
                date = date,
                meals = defaultMeals.map { it.copy() }
            )
        }
    }

    companion object {
        private val defaultMeals = listOf(
            MealSlot("Breakfast", "Tap to add a recipe"),
            MealSlot("Lunch", "Tap to add a recipe"),
            MealSlot("Dinner", "Tap to add a recipe")
        )
    }
}

data class MealPlanUiState(
    val plan: List<DailyMealPlan> = emptyList()
)

data class DailyMealPlan(
    val date: LocalDate,
    val meals: List<MealSlot>
)

data class MealSlot(
    val mealType: String,
    val recipeName: String
)

val MealPlanDayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE MMM d")
