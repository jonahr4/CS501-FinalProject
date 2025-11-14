package com.example.cs501_mealmapproject.ui.foodlog

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FoodLogViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FoodLogUiState())
    val uiState: StateFlow<FoodLogUiState> = _uiState.asStateFlow()
}

data class FoodLogUiState(
    val recentLogs: List<FoodLogEntry> = demoRecentLogs
)

data class FoodLogEntry(
    val meal: String,
    val source: String
)

private val demoRecentLogs = listOf(
    FoodLogEntry(
        meal = "Breakfast • Greek yogurt parfait",
        source = "Logged via barcode"
    ),
    FoodLogEntry(
        meal = "Lunch • Chickpea wrap",
        source = "Manual entry with substitutions"
    )
)
