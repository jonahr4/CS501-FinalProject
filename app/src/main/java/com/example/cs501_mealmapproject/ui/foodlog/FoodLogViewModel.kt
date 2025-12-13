package com.example.cs501_mealmapproject.ui.foodlog

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_mealmapproject.data.database.AppDatabase
import com.example.cs501_mealmapproject.data.database.FoodLogEntity
import com.example.cs501_mealmapproject.data.database.RecipeCacheEntity
import com.example.cs501_mealmapproject.data.database.IngredientWithMeasure
import com.example.cs501_mealmapproject.data.nutrition.FoodItem
import com.example.cs501_mealmapproject.data.nutrition.NutritionApi
import com.example.cs501_mealmapproject.data.openfoodfacts.OpenFoodFactsService
import com.example.cs501_mealmapproject.network.MealApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FoodLogViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val foodLogDao = database.foodLogDao()
    private val recipeCacheDao = database.recipeCacheDao()
    
    private val appContext = application.applicationContext
    private var currentUserId: String? = null
    private var prefs: SharedPreferences = application.getSharedPreferences("meal_plan_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(FoodLogUiState())
    val uiState: StateFlow<FoodLogUiState> = _uiState.asStateFlow()
    
    // For debouncing search queries
    private var searchJob: Job? = null

    init {
        loadFoodLogs()
        loadFavorites()
        loadFrequentFoods()
        loadTodaysPlannedMeals()
    }

    /**
     * Set the current user and load their data
     */
    fun setCurrentUser(userId: String) {
        if (currentUserId == userId) return
        currentUserId = userId
        prefs = appContext.getSharedPreferences("meal_plan_prefs_$userId", Context.MODE_PRIVATE)
        loadTodaysPlannedMeals()
    }

    private fun loadFoodLogs() {
        viewModelScope.launch {
            foodLogDao.getRecentFoodLogs(100).collect { entities ->
                val entries = entities.map { it.toFoodLogEntry() }
                
                // Split into today's logs by meal type
                val todayStart = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                val todaysLogs = entries.filter { it.timestamp >= todayStart }
                val breakfastLogs = todaysLogs.filter { it.mealType == MealType.BREAKFAST }
                val lunchLogs = todaysLogs.filter { it.mealType == MealType.LUNCH }
                val dinnerLogs = todaysLogs.filter { it.mealType == MealType.DINNER }
                val snackLogs = todaysLogs.filter { it.mealType == MealType.SNACK }
                
                _uiState.update { 
                    it.copy(
                        recentLogs = entries,
                        breakfastLogs = breakfastLogs,
                        lunchLogs = lunchLogs,
                        dinnerLogs = dinnerLogs,
                        snackLogs = snackLogs,
                        todaysSummary = calculateDailySummary(todaysLogs)
                    ) 
                }
            }
        }
    }
    
    private fun loadFavorites() {
        viewModelScope.launch {
            foodLogDao.getFavoriteFoods().collect { entities ->
                val favorites = entities.map { it.toFoodLogEntry() }
                _uiState.update { it.copy(favoriteFoods = favorites) }
            }
        }
    }
    
    private fun loadFrequentFoods() {
        viewModelScope.launch {
            foodLogDao.getRecentUniqueFoods(30).collect { entities ->
                val recent = entities.map { it.toFoodLogEntry() }
                _uiState.update { it.copy(recentUniqueFoods = recent) }
            }
        }
    }
    
    /**
     * Load today's planned meals from SharedPreferences (meal plan data)
     */
    private fun loadTodaysPlannedMeals() {
        viewModelScope.launch {
            try {
                val raw = prefs.getString("meal_plan_serialized_v1", null)
                if (raw.isNullOrBlank()) {
                    _uiState.update { it.copy(todaysPlannedMeals = emptyList()) }
                    return@launch
                }
                
                val today = LocalDate.now()
                val plannedMeals = mutableListOf<PlannedMealForLog>()
                
                raw.lines().forEach { line ->
                    if (line.isBlank()) return@forEach
                    val parts = line.split('|')
                    if (parts.size < 3) return@forEach
                    
                    val date = try { LocalDate.parse(parts[0]) } catch (e: Exception) { return@forEach }
                    if (date != today) return@forEach
                    
                    val mealTypeStr = parts[1]
                    val recipeName = parts.subList(2, parts.size).joinToString("|").replace("\\|", "|")
                    
                    if (recipeName != "Tap to add a recipe") {
                        val mealType = when (mealTypeStr) {
                            "Breakfast" -> MealType.BREAKFAST
                            "Lunch" -> MealType.LUNCH
                            "Dinner" -> MealType.DINNER
                            "Snack" -> MealType.SNACK
                            else -> MealType.SNACK
                        }
                        
                        // Check if recipe is in cache and get nutrition
                        val cachedRecipe = recipeCacheDao.getRecipe(recipeName)
                        
                        plannedMeals.add(PlannedMealForLog(
                            recipeName = recipeName,
                            mealType = mealType,
                            date = today,
                            estimatedCalories = cachedRecipe?.totalCalories ?: 0,
                            estimatedProtein = cachedRecipe?.totalProtein ?: 0f,
                            estimatedCarbs = cachedRecipe?.totalCarbs ?: 0f,
                            estimatedFat = cachedRecipe?.totalFat ?: 0f,
                            imageUrl = cachedRecipe?.imageUrl,
                            hasNutritionData = cachedRecipe?.isNutritionCalculated == true
                        ))
                    }
                }
                
                _uiState.update { it.copy(todaysPlannedMeals = plannedMeals) }
                
                // Fetch and cache recipes that aren't cached yet
                plannedMeals.filter { it.estimatedCalories == 0 }.forEach { meal ->
                    cacheRecipeWithNutrition(meal.recipeName)
                }
                
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error loading planned meals", e)
            }
        }
    }
    
    /**
     * Cache a recipe from TheMealDB API and calculate nutrition
     */
    private suspend fun cacheRecipeWithNutrition(recipeName: String) {
        try {
            // Check if already cached
            if (recipeCacheDao.isRecipeCached(recipeName)) return
            
            // Fetch from TheMealDB
            val response = MealApi.retrofitService.searchMeals(recipeName)
            val meal = response.meals?.firstOrNull() ?: return
            
            // Extract ingredients
            val ingredients = extractIngredientsFromMeal(meal)
            
            val entity = RecipeCacheEntity(
                recipeName = recipeName,
                imageUrl = meal.strMealThumb,
                sourceUrl = meal.strSource,
                category = meal.strCategory,
                area = meal.strArea,
                ingredients = ingredients,
                estimatedServings = 4
            )
            
            recipeCacheDao.insertRecipe(entity)
            Log.d("FoodLogViewModel", "Cached recipe: $recipeName with ${ingredients.size} ingredients")
            
            // Now calculate nutrition from ingredients
            calculateRecipeNutrition(recipeName, ingredients)
            
        } catch (e: Exception) {
            Log.e("FoodLogViewModel", "Error caching recipe: $recipeName", e)
        }
    }
    
    private fun extractIngredientsFromMeal(meal: com.example.cs501_mealmapproject.network.MealDto): List<IngredientWithMeasure> {
        val ingredients = listOf(
            meal.strIngredient1, meal.strIngredient2, meal.strIngredient3, meal.strIngredient4, meal.strIngredient5,
            meal.strIngredient6, meal.strIngredient7, meal.strIngredient8, meal.strIngredient9, meal.strIngredient10,
            meal.strIngredient11, meal.strIngredient12, meal.strIngredient13, meal.strIngredient14, meal.strIngredient15,
            meal.strIngredient16, meal.strIngredient17, meal.strIngredient18, meal.strIngredient19, meal.strIngredient20
        )
        val measures = listOf(
            meal.strMeasure1, meal.strMeasure2, meal.strMeasure3, meal.strMeasure4, meal.strMeasure5,
            meal.strMeasure6, meal.strMeasure7, meal.strMeasure8, meal.strMeasure9, meal.strMeasure10,
            meal.strMeasure11, meal.strMeasure12, meal.strMeasure13, meal.strMeasure14, meal.strMeasure15,
            meal.strMeasure16, meal.strMeasure17, meal.strMeasure18, meal.strMeasure19, meal.strMeasure20
        )
        
        return ingredients.mapIndexedNotNull { index, ingredient ->
            if (ingredient.isNullOrBlank() || ingredient.equals("null", ignoreCase = true)) {
                null
            } else {
                val measure = measures.getOrNull(index)?.takeIf { 
                    !it.isNullOrBlank() && !it.equals("null", ignoreCase = true) 
                } ?: ""
                IngredientWithMeasure(ingredient.trim(), measure.trim())
            }
        }
    }
    
    /**
     * Calculate nutrition for a recipe using USDA API for each ingredient
     */
    private suspend fun calculateRecipeNutrition(recipeName: String, ingredients: List<IngredientWithMeasure>) {
        try {
            var totalCalories = 0
            var totalProtein = 0f
            var totalCarbs = 0f
            var totalFat = 0f
            var totalFiber = 0f
            var totalSugar = 0f
            var totalSodium = 0f
            
            for (ingredient in ingredients) {
                try {
                    // Search USDA for each ingredient
                    val searchResponse = NutritionApi.api.searchFoods(ingredient.ingredient, pageSize = 1)
                    val food = searchResponse.foods?.firstOrNull()?.let { FoodItem.fromSearchResult(it) }
                    
                    if (food != null) {
                        // Estimate portion from measure (rough estimate - assume ~100g per ingredient)
                        val portion = estimatePortion(ingredient.measure)
                        
                        totalCalories += (food.calories * portion).toInt()
                        totalProtein += food.protein * portion
                        totalCarbs += food.carbs * portion
                        totalFat += food.fat * portion
                        totalFiber += food.fiber * portion
                        totalSugar += food.sugar * portion
                        totalSodium += food.sodium * portion
                    }
                    
                    // Small delay to avoid rate limiting
                    delay(100)
                } catch (e: Exception) {
                    Log.w("FoodLogViewModel", "Couldn't get nutrition for ${ingredient.ingredient}", e)
                }
            }
            
            // Update the cache with calculated nutrition
            recipeCacheDao.updateNutrition(
                name = recipeName,
                calories = totalCalories,
                protein = totalProtein,
                carbs = totalCarbs,
                fat = totalFat,
                fiber = totalFiber,
                sugar = totalSugar,
                sodium = totalSodium
            )
            
            Log.d("FoodLogViewModel", "Calculated nutrition for $recipeName: ${totalCalories}cal")
            
            // Reload planned meals to show updated nutrition
            loadTodaysPlannedMeals()
            
        } catch (e: Exception) {
            Log.e("FoodLogViewModel", "Error calculating nutrition for $recipeName", e)
        }
    }
    
    /**
     * Estimate portion multiplier from measure string
     */
    private fun estimatePortion(measure: String): Float {
        val lower = measure.lowercase()
        return when {
            lower.contains("cup") -> 2.4f  // ~240g
            lower.contains("tbsp") || lower.contains("tablespoon") -> 0.15f // ~15g
            lower.contains("tsp") || lower.contains("teaspoon") -> 0.05f // ~5g
            lower.contains("lb") || lower.contains("pound") -> 4.5f // ~450g
            lower.contains("oz") -> 0.28f // ~28g
            lower.contains("kg") -> 10f
            lower.contains("g") -> {
                // Try to extract number
                val num = lower.filter { it.isDigit() }.toFloatOrNull() ?: 100f
                num / 100f
            }
            lower.contains("whole") || lower.contains("large") -> 1.5f
            lower.contains("medium") -> 1f
            lower.contains("small") -> 0.5f
            lower.contains("pinch") || lower.contains("dash") -> 0.01f
            else -> 1f // Default to 1 serving (100g)
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            foodLogDao.deleteFoodLog(id)
        }
    }
    
    /**
     * Toggle favorite status for a food log
     */
    fun toggleFavorite(id: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            foodLogDao.setFavorite(id, isFavorite)
        }
    }
    
    /**
     * Update servings for an existing log
     */
    fun updateLogServings(id: Long, newServings: Float, originalEntry: FoodLogEntry) {
        viewModelScope.launch {
            val ratio = newServings / originalEntry.servings
            foodLogDao.updateServings(
                id = id,
                servings = newServings,
                calories = (originalEntry.calories * ratio).toInt(),
                protein = originalEntry.protein * ratio,
                carbs = originalEntry.carbs * ratio,
                fat = originalEntry.fat * ratio
            )
        }
    }
    
    /**
     * Update meal type for an existing log
     */
    fun updateLogMealType(id: Long, newMealType: MealType) {
        viewModelScope.launch {
            foodLogDao.updateMealType(id, newMealType.name)
        }
    }
    
    /**
     * Add notes to a log
     */
    fun updateLogNotes(id: Long, notes: String) {
        viewModelScope.launch {
            foodLogDao.updateNotes(id, notes)
        }
    }
    
    /**
     * Search for foods using USDA FoodData Central API
     */
    fun searchFoods(query: String) {
        searchJob?.cancel()
        
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        
        searchJob = viewModelScope.launch {
            delay(300)
            
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            
            try {
                Log.d("FoodLogViewModel", "Searching for: $query")
                val response = NutritionApi.api.searchFoods(query)
                
                val foods = response.foods?.map { FoodItem.fromSearchResult(it) } ?: emptyList()
                Log.d("FoodLogViewModel", "Found ${foods.size} results")
                
                _uiState.update { it.copy(searchResults = foods, isSearching = false) }
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Search failed", e)
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, searchError = e.message) }
            }
        }
    }
    
    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, searchError = null) }
    }
    
    /**
     * Log a food item from search results
     */
    fun logFoodItem(foodItem: FoodItem, servings: Float, mealType: MealType) {
        viewModelScope.launch {
            val multiplier = servings
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            
            val entity = FoodLogEntity(
                mealName = foodItem.name,
                calories = (foodItem.calories * multiplier).toInt(),
                protein = foodItem.protein * multiplier,
                carbs = foodItem.carbs * multiplier,
                fat = foodItem.fat * multiplier,
                fiber = foodItem.fiber * multiplier,
                sugar = foodItem.sugar * multiplier,
                sodium = foodItem.sodium * multiplier,
                servingSize = foodItem.servingSize ?: "100g",
                servings = servings,
                mealType = mealType.name,
                source = if (foodItem.brand != null) "USDA • ${foodItem.brand}" else "USDA Database",
                loggedTime = currentTime
            )
            foodLogDao.insertFoodLog(entity)
            Log.d("FoodLogViewModel", "Logged: ${foodItem.name} x$servings servings")
        }
    }

    /**
     * Log a planned meal from the meal plan with one tap
     */
    fun logPlannedMeal(plannedMeal: PlannedMealForLog, servings: Float = 1f) {
        viewModelScope.launch {
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            
            // Get cached nutrition if available
            val cached = recipeCacheDao.getRecipe(plannedMeal.recipeName)
            val perServing = (cached?.estimatedServings ?: 4).toFloat()
            
            val entity = FoodLogEntity(
                mealName = plannedMeal.recipeName,
                calories = ((cached?.totalCalories ?: 0) / perServing * servings).toInt(),
                protein = (cached?.totalProtein ?: 0f) / perServing * servings,
                carbs = (cached?.totalCarbs ?: 0f) / perServing * servings,
                fat = (cached?.totalFat ?: 0f) / perServing * servings,
                fiber = (cached?.totalFiber ?: 0f) / perServing * servings,
                sugar = (cached?.totalSugar ?: 0f) / perServing * servings,
                sodium = (cached?.totalSodium ?: 0f) / perServing * servings,
                servingSize = "1 serving",
                servings = servings,
                mealType = plannedMeal.mealType.name,
                source = "From Meal Plan",
                fromRecipe = plannedMeal.recipeName,
                imageUrl = plannedMeal.imageUrl,
                loggedTime = currentTime
            )
            foodLogDao.insertFoodLog(entity)
            Log.d("FoodLogViewModel", "Logged planned meal: ${plannedMeal.recipeName}")
        }
    }
    
    /**
     * Quick re-log a favorite or recent food
     */
    fun quickRelogFood(previousEntry: FoodLogEntry, mealType: MealType) {
        viewModelScope.launch {
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            
            val entity = FoodLogEntity(
                mealName = previousEntry.meal,
                calories = previousEntry.calories,
                protein = previousEntry.protein,
                carbs = previousEntry.carbs,
                fat = previousEntry.fat,
                fiber = previousEntry.fiber,
                sugar = previousEntry.sugar,
                sodium = previousEntry.sodium,
                servingSize = previousEntry.servingSize,
                servings = previousEntry.servings,
                mealType = mealType.name,
                source = previousEntry.source,
                isFavorite = previousEntry.isFavorite,
                fromRecipe = previousEntry.fromRecipe,
                imageUrl = previousEntry.imageUrl,
                loggedTime = currentTime
            )
            foodLogDao.insertFoodLog(entity)
            Log.d("FoodLogViewModel", "Re-logged: ${previousEntry.meal}")
        }
    }
    
    /**
     * Quick add just calories (for estimation)
     */
    fun quickAddCalories(calories: Int, mealType: MealType, name: String = "Quick Add") {
        viewModelScope.launch {
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            
            val entity = FoodLogEntity(
                mealName = name,
                calories = calories,
                protein = 0f,
                carbs = 0f,
                fat = 0f,
                servingSize = "1 serving",
                servings = 1f,
                mealType = mealType.name,
                source = "Quick Add",
                loggedTime = currentTime
            )
            foodLogDao.insertFoodLog(entity)
            Log.d("FoodLogViewModel", "Quick added: $calories calories")
        }
    }
    
    /**
     * Copy all meals from one day to another (for meal prep / repetitive eating)
     */
    fun copyMealsFromDate(sourceDate: LocalDate, targetMealType: MealType? = null) {
        viewModelScope.launch {
            val sourceStart = sourceDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val sourceEnd = sourceDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val logsFlow = if (targetMealType != null) {
                foodLogDao.getLogsByMealType(targetMealType.name, sourceStart, sourceEnd)
            } else {
                foodLogDao.getLogsForDate(sourceStart, sourceEnd)
            }
            
            val sourceLogs = logsFlow.first()
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            
            sourceLogs.forEach { log ->
                val newEntity = log.copy(
                    id = 0, // New ID
                    timestamp = System.currentTimeMillis(),
                    loggedTime = currentTime
                )
                foodLogDao.insertFoodLog(newEntity)
            }
            
            Log.d("FoodLogViewModel", "Copied ${sourceLogs.size} meals from $sourceDate")
        }
    }

    fun addManualLog(
        mealName: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        fiber: Float = 0f,
        sugar: Float = 0f,
        sodium: Float = 0f,
        servingSize: String = "1 serving",
        servings: Float = 1f,
        mealType: MealType = MealType.SNACK
    ) {
        viewModelScope.launch {
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            
            val entity = FoodLogEntity(
                mealName = mealName,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                sugar = sugar,
                sodium = sodium,
                servingSize = servingSize,
                servings = servings,
                mealType = mealType.name,
                source = "Manual entry",
                loggedTime = currentTime
            )
            foodLogDao.insertFoodLog(entity)
            Log.d("FoodLogViewModel", "Saved manual log: $mealName")
        }
    }

    fun addLogFromBarcode(barcode: String, mealType: MealType = MealType.SNACK) {
        viewModelScope.launch {
            try {
                Log.d("FoodLogViewModel", "Fetching product for barcode: $barcode")
                val response = OpenFoodFactsService.api.getProduct(barcode)
                val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))

                if (response.status == 1 && response.product != null) {
                    val product = response.product
                    val productName = product.productName ?: "Unknown Product"
                    val brands = product.brands ?: ""
                    val displayName = if (brands.isNotEmpty()) {
                        "$productName ($brands)"
                    } else {
                        productName
                    }

                    val nutriments = product.nutriments

                    val calories = nutriments?.energyKcalServing?.toInt() 
                        ?: nutriments?.energyKcal100g?.toInt() 
                        ?: 0
                    val protein = nutriments?.proteinsServing?.toFloat() 
                        ?: nutriments?.proteins100g?.toFloat() 
                        ?: 0f
                    val carbs = nutriments?.carbohydratesServing?.toFloat() 
                        ?: nutriments?.carbohydrates100g?.toFloat() 
                        ?: 0f
                    val fat = nutriments?.fatServing?.toFloat() 
                        ?: nutriments?.fat100g?.toFloat() 
                        ?: 0f
                    val fiber = nutriments?.fiberServing?.toFloat() 
                        ?: nutriments?.fiber100g?.toFloat() 
                        ?: 0f
                    val sugar = nutriments?.sugarsServing?.toFloat() 
                        ?: nutriments?.sugars100g?.toFloat() 
                        ?: 0f
                    val sodium = nutriments?.sodiumServing?.toFloat() 
                        ?: nutriments?.sodium100g?.toFloat() 
                        ?: 0f

                    val servingSize = product.servingSize ?: "1 serving"
                    val sourceLabel = if (nutriments?.energyKcalServing != null) {
                        "Scanned • Per serving ($servingSize)"
                    } else {
                        "Scanned • Per 100g"
                    }

                    Log.d("FoodLogViewModel", "Product found: $displayName")

                    val entity = FoodLogEntity(
                        mealName = displayName,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        fiber = fiber,
                        sugar = sugar,
                        sodium = sodium,
                        servingSize = servingSize,
                        servings = 1f,
                        mealType = mealType.name,
                        source = sourceLabel,
                        imageUrl = product.imageFrontUrl,
                        loggedTime = currentTime
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
                        servingSize = "Unknown",
                        servings = 1f,
                        mealType = mealType.name,
                        source = "Scanned • Barcode: $barcode (not found)",
                        loggedTime = currentTime
                    )
                    foodLogDao.insertFoodLog(entity)
                }
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching product", e)
                val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
                val entity = FoodLogEntity(
                    mealName = "Error Loading Product",
                    calories = 0,
                    protein = 0f,
                    carbs = 0f,
                    fat = 0f,
                    servingSize = "Unknown",
                    servings = 1f,
                    mealType = mealType.name,
                    source = "Scanned • Barcode: $barcode (error)",
                    loggedTime = currentTime
                )
                foodLogDao.insertFoodLog(entity)
            }
        }
    }
    
    private fun calculateDailySummary(logs: List<FoodLogEntry>): NutritionSummary {
        return NutritionSummary(
            totalCalories = logs.sumOf { it.calories },
            totalProtein = logs.sumOf { it.protein.toDouble() }.toFloat(),
            totalCarbs = logs.sumOf { it.carbs.toDouble() }.toFloat(),
            totalFat = logs.sumOf { it.fat.toDouble() }.toFloat(),
            totalFiber = logs.sumOf { it.fiber.toDouble() }.toFloat(),
            totalSugar = logs.sumOf { it.sugar.toDouble() }.toFloat(),
            mealCount = logs.size
        )
    }
    
    fun getTodaysSummary(): NutritionSummary {
        return _uiState.value.todaysSummary
    }
    
    /**
     * Refresh planned meals (call when navigating to food log)
     */
    fun refreshPlannedMeals() {
        loadTodaysPlannedMeals()
    }
}

private fun FoodLogEntity.toFoodLogEntry() = FoodLogEntry(
    id = id,
    meal = mealName,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    fiber = fiber,
    sugar = sugar,
    sodium = sodium,
    servingSize = servingSize,
    servings = servings,
    mealType = MealType.fromString(mealType),
    source = source,
    timestamp = timestamp,
    isFavorite = isFavorite,
    notes = notes,
    fromRecipe = fromRecipe,
    loggedTime = loggedTime,
    imageUrl = imageUrl
)

data class FoodLogUiState(
    val recentLogs: List<FoodLogEntry> = emptyList(),
    val breakfastLogs: List<FoodLogEntry> = emptyList(),
    val lunchLogs: List<FoodLogEntry> = emptyList(),
    val dinnerLogs: List<FoodLogEntry> = emptyList(),
    val snackLogs: List<FoodLogEntry> = emptyList(),
    val favoriteFoods: List<FoodLogEntry> = emptyList(),
    val recentUniqueFoods: List<FoodLogEntry> = emptyList(),
    val todaysPlannedMeals: List<PlannedMealForLog> = emptyList(),
    val searchResults: List<FoodItem> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val todaysSummary: NutritionSummary = NutritionSummary(0, 0f, 0f, 0f, 0f, 0f, 0)
)

data class FoodLogEntry(
    val id: Long = 0,
    val meal: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val servingSize: String = "1 serving",
    val servings: Float = 1f,
    val mealType: MealType = MealType.SNACK,
    val source: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val notes: String = "",
    val fromRecipe: String? = null,
    val loggedTime: String? = null,
    val imageUrl: String? = null
)

data class PlannedMealForLog(
    val recipeName: String,
    val mealType: MealType,
    val date: LocalDate,
    val estimatedCalories: Int = 0,
    val estimatedProtein: Float = 0f,
    val estimatedCarbs: Float = 0f,
    val estimatedFat: Float = 0f,
    val imageUrl: String? = null,
    val hasNutritionData: Boolean = false
)

data class NutritionSummary(
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val totalFiber: Float,
    val totalSugar: Float,
    val mealCount: Int
)

enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack");
    
    companion object {
        fun fromString(value: String?): MealType {
            return entries.find { it.name == value } ?: SNACK
        }
    }
}
                    val brands = product.brands ?: ""
                    val displayName = if (brands.isNotEmpty()) {
                        "$productName ($brands)"
                    } else {
                        productName
                    }

                    val nutriments = product.nutriments

                    val calories = nutriments?.energyKcalServing?.toInt() 
                        ?: nutriments?.energyKcal100g?.toInt() 
                        ?: 0
                    val protein = nutriments?.proteinsServing?.toFloat() 
                        ?: nutriments?.proteins100g?.toFloat() 
                        ?: 0f
                    val carbs = nutriments?.carbohydratesServing?.toFloat() 
                        ?: nutriments?.carbohydrates100g?.toFloat() 
                        ?: 0f
                    val fat = nutriments?.fatServing?.toFloat() 
                        ?: nutriments?.fat100g?.toFloat() 
                        ?: 0f
                    val fiber = nutriments?.fiberServing?.toFloat() 
                        ?: nutriments?.fiber100g?.toFloat() 
                        ?: 0f
                    val sugar = nutriments?.sugarsServing?.toFloat() 
                        ?: nutriments?.sugars100g?.toFloat() 
                        ?: 0f
                    val sodium = nutriments?.sodiumServing?.toFloat() 
                        ?: nutriments?.sodium100g?.toFloat() 
                        ?: 0f

                    val servingSize = product.servingSize ?: "1 serving"
                    val sourceLabel = if (nutriments?.energyKcalServing != null) {
                        "Scanned • Per serving ($servingSize)"
                    } else {
                        "Scanned • Per 100g"
                    }

                    Log.d("FoodLogViewModel", "Product found: $displayName")

                    val entity = FoodLogEntity(
                        mealName = displayName,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        fiber = fiber,
                        sugar = sugar,
                        sodium = sodium,
                        servingSize = servingSize,
                        servings = 1f,
                        mealType = mealType.name,
                        source = sourceLabel
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
                        servingSize = "Unknown",
                        servings = 1f,
                        mealType = mealType.name,
                        source = "Scanned • Barcode: $barcode (not found)"
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
                    servingSize = "Unknown",
                    servings = 1f,
                    mealType = mealType.name,
                    source = "Scanned • Barcode: $barcode (error)"
                )
                foodLogDao.insertFoodLog(entity)
            }
        }
    }
    
    /**
     * Get today's nutrition summary
     */
    fun getTodaysSummary(): NutritionSummary {
        val todayStart = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        
        val todaysLogs = _uiState.value.recentLogs.filter { it.timestamp >= todayStart }
        
        return NutritionSummary(
            totalCalories = todaysLogs.sumOf { it.calories },
            totalProtein = todaysLogs.sumOf { it.protein.toDouble() }.toFloat(),
            totalCarbs = todaysLogs.sumOf { it.carbs.toDouble() }.toFloat(),
            totalFat = todaysLogs.sumOf { it.fat.toDouble() }.toFloat(),
            totalFiber = todaysLogs.sumOf { it.fiber.toDouble() }.toFloat(),
            totalSugar = todaysLogs.sumOf { it.sugar.toDouble() }.toFloat(),
            mealCount = todaysLogs.size
        )
    }
}

data class FoodLogUiState(
    val recentLogs: List<FoodLogEntry> = emptyList(),
    val searchResults: List<FoodItem> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null
)

data class FoodLogEntry(
    val id: Long = 0,
    val meal: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val servingSize: String = "1 serving",
    val servings: Float = 1f,
    val mealType: MealType = MealType.SNACK,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class NutritionSummary(
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val totalFiber: Float,
    val totalSugar: Float,
    val mealCount: Int
)

enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack");
    
    companion object {
        fun fromString(value: String?): MealType {
            return entries.find { it.name == value } ?: SNACK
        }
    }
}
