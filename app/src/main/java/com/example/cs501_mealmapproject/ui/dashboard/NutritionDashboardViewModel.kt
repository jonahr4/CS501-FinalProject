package com.example.cs501_mealmapproject.ui.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NutritionDashboardViewModel : ViewModel() {

    // A private MutableStateFlow to hold the list of metrics. This is the internal state.
    private val _metrics = MutableStateFlow<List<NutritionMetric>>(emptyList())

    // A public, read-only StateFlow that UI can observe for changes.
    val metrics: StateFlow<List<NutritionMetric>> = _metrics.asStateFlow()

    init {
        // When the ViewModel is created, load the initial data.
        loadSampleMetrics()
    }

    private fun loadSampleMetrics() {
        _metrics.value = listOf(
            NutritionMetric(label = "Calories", value = "1,850 / 2,000", status = "92% of goal"),
            NutritionMetric(label = "Protein", value = "110g / 125g", status = "+2 day streak"),
            NutritionMetric(label = "Carbs", value = "210g / 230g", status = "on track"),
            NutritionMetric(label = "Fats", value = "60g / 70g", status = "slightly low")
        )
    }
}
