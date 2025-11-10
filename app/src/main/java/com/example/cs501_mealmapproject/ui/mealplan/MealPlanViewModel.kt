package com.example.cs501_mealmapproject.ui.mealplan

import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MealPlanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanUiState(plan = generateWeekPlan()))
    val uiState: StateFlow<MealPlanUiState> = _uiState.asStateFlow()

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
