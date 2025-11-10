package com.example.cs501_mealmapproject.ui.recipes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RecipeDiscoveryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeDiscoveryUiState())
    val uiState: StateFlow<RecipeDiscoveryUiState> = _uiState.asStateFlow()
}

data class RecipeDiscoveryUiState(
    val recipes: List<RecipeSummary> = demoRecipes
)

data class RecipeSummary(
    val title: String,
    val prepTime: String,
    val calories: Int,
    val tags: List<String>
)

private val demoRecipes = listOf(
    RecipeSummary(
        title = "Mediterranean Chickpea Salad",
        prepTime = "15 min",
        calories = 420,
        tags = listOf("High protein", "Veg-friendly", "Budget")
    ),
    RecipeSummary(
        title = "Sheet-Pan Teriyaki Salmon",
        prepTime = "30 min",
        calories = 510,
        tags = listOf("Omega-3", "Meal Prep", "Gluten free")
    )
)
