package com.example.cs501_mealmapproject.ui.foodlog

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_mealmapproject.data.openfoodfacts.OpenFoodFactsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FoodLogViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FoodLogUiState())
    val uiState: StateFlow<FoodLogUiState> = _uiState.asStateFlow()

    fun addLog(entry: FoodLogEntry) {
        _uiState.update { state ->
            state.copy(recentLogs = listOf(entry) + state.recentLogs)
        }
    }

    fun addLogFromBarcode(barcode: String) {
        viewModelScope.launch {
            try {
                Log.d("FoodLogViewModel", "Fetching product for barcode: $barcode")
                val response = OpenFoodFactsService.api.getProduct(barcode)
                
                if (response.status == 1 && response.product != null) {
                    val product = response.product
                    val productName = product.productName ?: "Unknown Product"
                    val brands = product.brands ?: ""
                    val displayName = if (brands.isNotEmpty()) {
                        "$productName ($brands)"
                    } else {
                        productName
                    }
                    
                    Log.d("FoodLogViewModel", "Product found: $displayName")
                    addLog(
                        FoodLogEntry(
                            meal = displayName,
                            source = "Scanned • Barcode: $barcode"
                        )
                    )
                } else {
                    Log.w("FoodLogViewModel", "Product not found for barcode: $barcode")
                    addLog(
                        FoodLogEntry(
                            meal = "Unknown Product",
                            source = "Scanned • Barcode: $barcode (not in database)"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching product", e)
                addLog(
                    FoodLogEntry(
                        meal = "Error Loading Product",
                        source = "Scanned • Barcode: $barcode (network error)"
                    )
                )
            }
        }
    }
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
