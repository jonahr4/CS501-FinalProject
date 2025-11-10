package com.example.cs501_mealmapproject.ui.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NutritionDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionDashboardUiState())
    val uiState: StateFlow<NutritionDashboardUiState> = _uiState.asStateFlow()
}

data class NutritionDashboardUiState(
    val metrics: List<NutritionMetric> = demoMetrics
)

data class NutritionMetric(
    val label: String,
    val value: String,
    val status: String
)

private val demoMetrics = listOf(
    NutritionMetric(label = "Calories", value = "1,850 / 2,000", status = "92% of goal"),
    NutritionMetric(label = "Protein", value = "110g / 125g", status = "+2 day streak"),
    NutritionMetric(label = "Carbs", value = "210g / 230g", status = "on track"),
    NutritionMetric(label = "Fats", value = "60g / 70g", status = "slightly low")
)
