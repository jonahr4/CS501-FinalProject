package com.example.cs501_mealmapproject.ui.foodlog

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_mealmapproject.data.database.AppDatabase
import com.example.cs501_mealmapproject.data.database.FoodLogEntity
import com.example.cs501_mealmapproject.data.openfoodfacts.OpenFoodFactsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FoodLogViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val foodLogDao = database.foodLogDao()

    private val _uiState = MutableStateFlow(FoodLogUiState())
    val uiState: StateFlow<FoodLogUiState> = _uiState.asStateFlow()

    init {
        loadFoodLogs()
    }

    private fun loadFoodLogs() {
        viewModelScope.launch {
            foodLogDao.getRecentFoodLogs(20).collect { entities ->
                val entries = entities.map { entity ->
                    FoodLogEntry(
                        id = entity.id,
                        meal = entity.mealName,
                        calories = entity.calories,
                        protein = entity.protein,
                        carbs = entity.carbs,
                        fat = entity.fat,
                        source = entity.source,
                        timestamp = entity.timestamp
                    )
                }
                _uiState.value = FoodLogUiState(recentLogs = entries)
            }
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            foodLogDao.deleteFoodLog(id)
        }
    }

    fun addManualLog(
        mealName: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float
    ) {
        viewModelScope.launch {
            val entity = FoodLogEntity(
                mealName = mealName,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                source = "Manual entry"
            )
            foodLogDao.insertFoodLog(entity)
            Log.d("FoodLogViewModel", "Saved manual log: $mealName")
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

                    // Extract nutrition info from OpenFoodFacts
                    val nutriments = product.nutriments
                    val calories = nutriments?.energyKcal?.toInt() ?: 0
                    val protein = nutriments?.proteins?.toFloat() ?: 0f
                    val carbs = nutriments?.carbohydrates?.toFloat() ?: 0f
                    val fat = nutriments?.fat?.toFloat() ?: 0f

                    Log.d("FoodLogViewModel", "Product found: $displayName")

                    val entity = FoodLogEntity(
                        mealName = displayName,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        source = "Scanned • Barcode: $barcode"
                    )
                    foodLogDao.insertFoodLog(entity)
                } else {
                    Log.w("FoodLogViewModel", "Product not found for barcode: $barcode")
                    val entity = FoodLogEntity(
                        mealName = "Unknown Product",
                        calories = 0,
                        protein = 0f,
                        carbs = 0f,
                        fat = 0f,
                        source = "Scanned • Barcode: $barcode (not in database)"
                    )
                    foodLogDao.insertFoodLog(entity)
                }
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching product", e)
                val entity = FoodLogEntity(
                    mealName = "Error Loading Product",
                    calories = 0,
                    protein = 0f,
                    carbs = 0f,
                    fat = 0f,
                    source = "Scanned • Barcode: $barcode (network error)"
                )
                foodLogDao.insertFoodLog(entity)
            }
        }
    }
}

data class FoodLogUiState(
    val recentLogs: List<FoodLogEntry> = emptyList()
)

data class FoodLogEntry(
    val id: Long = 0,
    val meal: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)
