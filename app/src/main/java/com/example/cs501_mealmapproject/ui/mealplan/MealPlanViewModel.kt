package com.example.cs501_mealmapproject.ui.mealplan

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MealPlanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlanUiState())
    val uiState: StateFlow<MealPlanUiState> = _uiState.asStateFlow()
}

data class MealPlanUiState(
    val plan: List<DailyMealPlan> = demoMealPlan
)

data class DailyMealPlan(
    val day: String,
    val meals: List<MealSlot>
)

data class MealSlot(
    val mealType: String,
    val recipeName: String
)

private val demoMealPlan = listOf(
    DailyMealPlan(
        day = "Monday",
        meals = listOf(
            MealSlot("Breakfast", "Greek yogurt parfait"),
            MealSlot("Lunch", "Quinoa bowl with roasted veggies"),
            MealSlot("Dinner", "Lemon herb salmon")
        )
    ),
    DailyMealPlan(
        day = "Tuesday",
        meals = listOf(
            MealSlot("Breakfast", "Overnight oats"),
            MealSlot("Lunch", "Spicy chickpea wrap"),
            MealSlot("Dinner", "Sheet-pan chicken fajitas")
        )
    )
)
