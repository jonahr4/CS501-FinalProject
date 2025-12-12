package com.example.cs501_mealmapproject.ui.mealplan

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MealPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("meal_plan_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MealPlanUiState(plan = loadSavedPlan() ?: generateWeekPlan()))
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
        
        savePlanToPrefs()
    }

    fun removeMeal(date: LocalDate, mealType: String) {
        assignMeal(date, mealType, "Tap to add a recipe")
    }

    fun setPreSelectedSlot(date: LocalDate, mealType: String) {
        _uiState.update { it.copy(preSelectedDate = date, preSelectedMealType = mealType) }
    }

    fun clearPreSelectedSlot() {
        _uiState.update { it.copy(preSelectedDate = null, preSelectedMealType = null) }
    }

    private fun savePlanToPrefs() {
        try {
            val lines = _uiState.value.plan.flatMap { day ->
                day.meals.map { slot ->
                    // date|mealType|recipeName (recipeName may contain pipes/newlines so escape by replacing)
                    val safeRecipe = slot.recipeName.replace("|", "\\|").replace("\n", " ")
                    "${day.date}|${slot.mealType}|$safeRecipe"
                }
            }
            prefs.edit().putString(PREF_KEY, lines.joinToString("\n")).apply()
        } catch (e: Exception) {
            Log.w("MealPlanVM", "Failed to save plan: ${e.message}")
        }
    }

    private fun loadSavedPlan(): List<DailyMealPlan>? {
        val raw = prefs.getString(PREF_KEY, null) ?: return null
        return try {
            // parse lines into map(date -> map(mealType -> recipeName))
            val map = mutableMapOf<LocalDate, MutableMap<String, String>>()
            raw.lines().forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = line.split('|')
                if (parts.size < 3) return@forEach
                val date = LocalDate.parse(parts[0])
                val mealType = parts[1]
                val recipe = parts.subList(2, parts.size).joinToString("|").replace("\\|", "|")
                map.getOrPut(date) { mutableMapOf() }[mealType] = recipe
            }
            val start = LocalDate.now()
            (0 until 7).map { offset ->
                val date = start.plusDays(offset.toLong())
                val meals = defaultMeals.map { slot ->
                    val recipe = map[date]?.get(slot.mealType) ?: slot.recipeName
                    slot.copy(recipeName = recipe)
                }
                DailyMealPlan(date = date, meals = meals)
            }
        } catch (e: Exception) {
            Log.w("MealPlanVM", "Failed to load saved plan: ${e.message}")
            null
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
        private const val PREF_KEY = "meal_plan_serialized_v1"
    }
}

data class MealPlanUiState(
    val plan: List<DailyMealPlan> = emptyList(),
    val preSelectedDate: LocalDate? = null,
    val preSelectedMealType: String? = null
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
