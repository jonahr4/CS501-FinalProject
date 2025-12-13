package com.example.cs501_mealmapproject.ui.mealplan

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
    private val appContext = application.applicationContext
    private var currentUserId: String? = null
    private var prefs: SharedPreferences = application.getSharedPreferences("meal_plan_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(MealPlanUiState(plan = generateWeekPlan()))
    val uiState: StateFlow<MealPlanUiState> = _uiState.asStateFlow()

    init {
        loadMealPlansFromDatabase()
        syncFromFirestore()
    }

    private fun loadMealPlansFromDatabase() {
        viewModelScope.launch {
            mealPlanRepository.getAllMealPlans().collect { entities ->
                val planMap = entities.groupBy { it.date }

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
            val userId = authRepository.currentUser?.uid ?: return@launch
            mealPlanRepository.syncFromFirestore(userId)
            Log.d("MealPlanViewModel", "Synced meal plans from Firestore")
        }
    }

    /**
     * Set the current user and load their meal plan data.
     * Call this when user signs in or when the ViewModel is first created with a signed-in user.
     */
    fun setCurrentUser(userId: String) {
        if (currentUserId == userId) return

        currentUserId = userId
        prefs = appContext.getSharedPreferences("meal_plan_prefs_$userId", Context.MODE_PRIVATE)

        val savedPlan = loadSavedPlan()
        _uiState.value = MealPlanUiState(plan = savedPlan ?: generateWeekPlan())
        Log.d("MealPlanVM", "Loaded meal plan for user: $userId")

        viewModelScope.launch {
            mealPlanRepository.syncFromFirestore(userId)
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

        savePlanToPrefs()
        savePlanToDatabase(date, mealType, recipeName)
    }

    fun removeMeal(date: LocalDate, mealType: String) {
        _uiState.update { state ->
            val updatedPlan = state.plan.map { day ->
                if (day.date == date) {
                    val updatedMeals = day.meals.map { slot ->
                        if (slot.mealType == mealType) slot.copy(recipeName = "Tap to add a recipe") else slot
                    }
                    day.copy(meals = updatedMeals)
                } else {
                    day
                }
            }
            state.copy(plan = updatedPlan)
        }

        savePlanToPrefs()

        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            mealPlanRepository.deleteMealPlan(userId, date.toString(), mealType)
        }
    }

    fun setPreSelectedSlot(date: LocalDate, mealType: String) {
        _uiState.update { it.copy(preSelectedDate = date, preSelectedMealType = mealType) }
    }

    fun clearPreSelectedSlot() {
        _uiState.update { it.copy(preSelectedDate = null, preSelectedMealType = null) }
    }

    private fun savePlanToPrefs() {
        val lines = _uiState.value.plan.flatMap { day ->
            day.meals.map { slot ->
                val safeRecipe = slot.recipeName.replace("|", "\\|").replace("\n", " ")
                "${day.date}|${slot.mealType}|$safeRecipe"
            }
        }
        prefs.edit().putString(PREF_KEY, lines.joinToString("\n")).apply()
    }

    private fun loadSavedPlan(): List<DailyMealPlan>? {
        val serialized = prefs.getString(PREF_KEY, null) ?: return null
        val lines = serialized.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        val storedSlots = mutableMapOf<LocalDate, MutableList<MealSlot>>()
        lines.forEach { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size == 3) {
                val date = runCatching { LocalDate.parse(parts[0]) }.getOrNull() ?: return@forEach
                val mealType = parts[1]
                val recipe = parts[2].replace("\\|", "|")
                val slots = storedSlots.getOrPut(date) { defaultMeals.map { it.copy() }.toMutableList() }
                val index = slots.indexOfFirst { it.mealType == mealType }
                if (index >= 0) {
                    slots[index] = slots[index].copy(recipeName = recipe)
                } else {
                    slots.add(MealSlot(mealType, recipe))
                }
            }
        }

        val defaultPlan = generateWeekPlan(LocalDate.now())
        return defaultPlan.map { day ->
            val slots = storedSlots[day.date] ?: return@map day
            val mergedSlots = defaultMeals.map { defaultSlot ->
                slots.find { it.mealType == defaultSlot.mealType } ?: defaultSlot
            }
            day.copy(meals = mergedSlots)
        }
    }

    private fun savePlanToDatabase(date: LocalDate, mealType: String, recipeName: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUser?.uid ?: return@launch
            val entity = MealPlanEntity(
                date = date.toString(),
                mealType = mealType,
                recipeName = recipeName
            )
            mealPlanRepository.saveMealPlan(userId, entity)
            Log.d("MealPlanViewModel", "Saved meal plan: $date $mealType")
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
            MealSlot("Dinner", "Tap to add a recipe"),
            MealSlot("Snack", "Tap to add a recipe")
        )
        private const val PREF_KEY = "meal_plan_serialized_v2"
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
