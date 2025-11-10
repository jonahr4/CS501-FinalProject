package com.example.cs501_mealmapproject.ui.mealplan

import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MealPlanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanUiState(plan = generateWeekPlan()))
    val uiState: StateFlow<MealPlanUiState> = _uiState.asStateFlow()

    private fun generateWeekPlan(startDate: LocalDate = LocalDate.now()): List<DailyMealPlan> {
        val formatter = DateTimeFormatter.ofPattern("EEEE MMM d")
        return (0 until 7).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            DailyMealPlan(
                day = date.format(formatter),
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
    val day: String,
    val meals: List<MealSlot>
)

data class MealSlot(
    val mealType: String,
    val recipeName: String
)
