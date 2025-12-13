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
    
    // Rate limiting for USDA API (max 30 requests per minute with DEMO_KEY)
    private var lastApiCallTime = 0L
    private val minApiCallInterval = 2000L // 2 seconds between calls
    
    // Search cache to avoid repeated API calls
    private val searchCache = mutableMapOf<String, List<FoodItem>>()

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
                val raw = prefs.getString("meal_plan_serialized_v2", null)
                if (raw.isNullOrBlank()) {
                    Log.d("FoodLogViewModel", "No meal plan data found")
                    _uiState.update { it.copy(todaysPlannedMeals = emptyList()) }
                    return@launch
                }
                
                val today = LocalDate.now()
                val plannedMeals = mutableListOf<PlannedMealForLog>()
                
                Log.d("FoodLogViewModel", "Loading planned meals for today: $today")
                
                raw.lines().forEach { line ->
                    if (line.isBlank()) return@forEach
                    val parts = line.split('|')
                    if (parts.size < 3) return@forEach
                    
                    val date = try { LocalDate.parse(parts[0]) } catch (e: Exception) { return@forEach }
                    if (date != today) return@forEach
                    
                    val mealTypeStr = parts[1]
                    val recipeName = parts.subList(2, parts.size).joinToString("|").replace("\\|", "|")
                    
                    if (recipeName != "Tap to add a recipe" && recipeName.isNotBlank()) {
                        val mealType = when (mealTypeStr) {
                            "Breakfast" -> MealType.BREAKFAST
                            "Lunch" -> MealType.LUNCH
                            "Dinner" -> MealType.DINNER
                            "Snack" -> MealType.SNACK
                            else -> MealType.SNACK
                        }
                        
                        Log.d("FoodLogViewModel", "Found planned meal: $recipeName for $mealTypeStr")
                        
                        // Check if recipe is in cache and get nutrition
                        val cachedRecipe = recipeCacheDao.getRecipe(recipeName)
                        
                        val servings = cachedRecipe?.estimatedServings ?: 4
                        val totalCal = cachedRecipe?.totalCalories ?: 0
                        val perServingCal = if (servings > 0) totalCal / servings else totalCal
                        val perServingProtein = if (servings > 0) (cachedRecipe?.totalProtein ?: 0f) / servings else 0f
                        val perServingCarbs = if (servings > 0) (cachedRecipe?.totalCarbs ?: 0f) / servings else 0f
                        val perServingFat = if (servings > 0) (cachedRecipe?.totalFat ?: 0f) / servings else 0f
                        
                        plannedMeals.add(PlannedMealForLog(
                            recipeName = recipeName,
                            mealType = mealType,
                            date = today,
                            estimatedCalories = perServingCal,
                            estimatedProtein = perServingProtein,
                            estimatedCarbs = perServingCarbs,
                            estimatedFat = perServingFat,
                            estimatedServings = servings,
                            totalCalories = totalCal,
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
            var successfulLookups = 0
            
            Log.d("FoodLogViewModel", "Calculating nutrition for $recipeName with ${ingredients.size} ingredients")
            
            for (ingredient in ingredients) {
                try {
                    // Rate limiting check
                    val timeSinceLastCall = System.currentTimeMillis() - lastApiCallTime
                    if (timeSinceLastCall < minApiCallInterval) {
                        delay(minApiCallInterval - timeSinceLastCall)
                    }
                    
                    // Search USDA for each ingredient
                    lastApiCallTime = System.currentTimeMillis()
                    val searchResponse = NutritionApi.api.searchFoods(ingredient.ingredient, pageSize = 1)
                    val food = searchResponse.foods?.firstOrNull()?.let { FoodItem.fromSearchResult(it) }
                    
                    if (food != null) {
                        // Estimate portion from measure
                        val portion = estimatePortion(ingredient.measure)
                        
                        totalCalories += (food.calories * portion).toInt()
                        totalProtein += food.protein * portion
                        totalCarbs += food.carbs * portion
                        totalFat += food.fat * portion
                        totalFiber += food.fiber * portion
                        totalSugar += food.sugar * portion
                        totalSodium += food.sodium * portion
                        successfulLookups++
                        
                        Log.d("FoodLogViewModel", "  ${ingredient.ingredient}: ${(food.calories * portion).toInt()} cal")
                    } else {
                        // Use fallback estimation
                        val fallback = estimateFallbackNutrition(ingredient.ingredient, ingredient.measure)
                        totalCalories += fallback.calories
                        totalProtein += fallback.protein
                        totalCarbs += fallback.carbs
                        totalFat += fallback.fat
                        Log.d("FoodLogViewModel", "  ${ingredient.ingredient}: ${fallback.calories} cal (fallback)")
                    }
                    
                } catch (e: retrofit2.HttpException) {
                    if (e.code() == 429) {
                        Log.w("FoodLogViewModel", "Rate limited, using fallback for ${ingredient.ingredient}")
                        // Use fallback when rate limited
                        val fallback = estimateFallbackNutrition(ingredient.ingredient, ingredient.measure)
                        totalCalories += fallback.calories
                        totalProtein += fallback.protein
                        totalCarbs += fallback.carbs
                        totalFat += fallback.fat
                    }
                } catch (e: Exception) {
                    Log.w("FoodLogViewModel", "Couldn't get nutrition for ${ingredient.ingredient}: ${e.message}")
                    // Use fallback
                    val fallback = estimateFallbackNutrition(ingredient.ingredient, ingredient.measure)
                    totalCalories += fallback.calories
                    totalProtein += fallback.protein
                    totalCarbs += fallback.carbs
                    totalFat += fallback.fat
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
            
            Log.d("FoodLogViewModel", "Calculated nutrition for $recipeName: ${totalCalories}cal ($successfulLookups/${ingredients.size} from API)")
            
            // Reload planned meals to show updated nutrition
            loadTodaysPlannedMeals()
            
        } catch (e: Exception) {
            Log.e("FoodLogViewModel", "Error calculating nutrition for $recipeName", e)
        }
    }
    
    /**
     * Fallback nutrition estimation when API is unavailable
     */
    private data class FallbackNutrition(val calories: Int, val protein: Float, val carbs: Float, val fat: Float)
    
    private fun estimateFallbackNutrition(ingredient: String, measure: String): FallbackNutrition {
        val lower = ingredient.lowercase()
        val portion = estimatePortion(measure)
        
        // Base values per 100g for common food categories
        val base: FallbackNutrition = when {
            // Proteins
            lower.contains("chicken") -> FallbackNutrition(165, 31f, 0f, 3.6f)
            lower.contains("beef") || lower.contains("steak") -> FallbackNutrition(250, 26f, 0f, 17f)
            lower.contains("pork") -> FallbackNutrition(242, 27f, 0f, 14f)
            lower.contains("lamb") -> FallbackNutrition(294, 25f, 0f, 21f)
            lower.contains("fish") || lower.contains("salmon") || lower.contains("tuna") -> FallbackNutrition(208, 20f, 0f, 13f)
            lower.contains("shrimp") || lower.contains("prawn") -> FallbackNutrition(99, 24f, 0f, 0.3f)
            lower.contains("egg") -> FallbackNutrition(155, 13f, 1f, 11f)
            lower.contains("tofu") -> FallbackNutrition(76, 8f, 2f, 4f)
            
            // Dairy
            lower.contains("milk") -> FallbackNutrition(42, 3.4f, 5f, 1f)
            lower.contains("cheese") -> FallbackNutrition(402, 25f, 1f, 33f)
            lower.contains("yogurt") || lower.contains("yoghurt") -> FallbackNutrition(59, 10f, 4f, 0.7f)
            lower.contains("cream") -> FallbackNutrition(340, 2f, 3f, 36f)
            lower.contains("butter") -> FallbackNutrition(717, 0.9f, 0f, 81f)
            
            // Grains & Carbs
            lower.contains("rice") -> FallbackNutrition(130, 2.7f, 28f, 0.3f)
            lower.contains("pasta") || lower.contains("noodle") -> FallbackNutrition(131, 5f, 25f, 1f)
            lower.contains("bread") -> FallbackNutrition(265, 9f, 49f, 3f)
            lower.contains("flour") -> FallbackNutrition(364, 10f, 76f, 1f)
            lower.contains("tortilla") -> FallbackNutrition(312, 8f, 52f, 8f)
            lower.contains("potato") -> FallbackNutrition(77, 2f, 17f, 0.1f)
            
            // Vegetables
            lower.contains("onion") -> FallbackNutrition(40, 1f, 9f, 0.1f)
            lower.contains("garlic") -> FallbackNutrition(149, 6f, 33f, 0.5f)
            lower.contains("tomato") -> FallbackNutrition(18, 0.9f, 3.9f, 0.2f)
            lower.contains("carrot") -> FallbackNutrition(41, 0.9f, 10f, 0.2f)
            lower.contains("pepper") || lower.contains("capsicum") -> FallbackNutrition(31, 1f, 6f, 0.3f)
            lower.contains("spinach") -> FallbackNutrition(23, 2.9f, 3.6f, 0.4f)
            lower.contains("broccoli") -> FallbackNutrition(34, 2.8f, 7f, 0.4f)
            lower.contains("mushroom") -> FallbackNutrition(22, 3f, 3f, 0.3f)
            lower.contains("ginger") -> FallbackNutrition(80, 2f, 18f, 0.8f)
            lower.contains("celery") -> FallbackNutrition(16, 0.7f, 3f, 0.2f)
            lower.contains("lettuce") -> FallbackNutrition(15, 1.4f, 2.9f, 0.2f)
            lower.contains("cucumber") -> FallbackNutrition(16, 0.7f, 3.6f, 0.1f)
            
            // Legumes
            lower.contains("bean") || lower.contains("lentil") -> FallbackNutrition(116, 9f, 20f, 0.4f)
            lower.contains("chickpea") -> FallbackNutrition(164, 9f, 27f, 2.6f)
            
            // Oils & Fats
            lower.contains("oil") -> FallbackNutrition(884, 0f, 0f, 100f)
            
            // Sauces & Condiments
            lower.contains("sauce") || lower.contains("paste") -> FallbackNutrition(50, 1f, 10f, 0.5f)
            lower.contains("soy sauce") -> FallbackNutrition(53, 8f, 5f, 0f)
            lower.contains("vinegar") -> FallbackNutrition(18, 0f, 0.1f, 0f)
            lower.contains("sugar") -> FallbackNutrition(387, 0f, 100f, 0f)
            lower.contains("honey") -> FallbackNutrition(304, 0.3f, 82f, 0f)
            
            // Spices (very low cal)
            lower.contains("salt") || lower.contains("pepper") || lower.contains("spice") ||
            lower.contains("cumin") || lower.contains("paprika") || lower.contains("cinnamon") ||
            lower.contains("turmeric") || lower.contains("coriander") || lower.contains("cilantro") -> FallbackNutrition(5, 0.2f, 1f, 0.1f)
            
            // Nuts & Seeds
            lower.contains("almond") || lower.contains("nut") -> FallbackNutrition(579, 21f, 22f, 50f)
            lower.contains("coconut") -> FallbackNutrition(354, 3f, 15f, 33f)
            
            // Fruits
            lower.contains("lemon") || lower.contains("lime") -> FallbackNutrition(29, 1f, 9f, 0.3f)
            lower.contains("apple") -> FallbackNutrition(52, 0.3f, 14f, 0.2f)
            lower.contains("banana") -> FallbackNutrition(89, 1f, 23f, 0.3f)
            
            // Stock/Broth
            lower.contains("stock") || lower.contains("broth") -> FallbackNutrition(10, 1f, 1f, 0.2f)
            lower.contains("water") -> FallbackNutrition(0, 0f, 0f, 0f)
            
            // Default
            else -> FallbackNutrition(50, 2f, 8f, 1f) // Conservative default
        }
        
        return FallbackNutrition(
            calories = (base.calories * portion).toInt(),
            protein = base.protein * portion,
            carbs = base.carbs * portion,
            fat = base.fat * portion
        )
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
     * Search for foods using USDA FoodData Central API with rate limiting
     */
    fun searchFoods(query: String) {
        searchJob?.cancel()
        
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        
        // Check cache first
        val cacheKey = query.lowercase().trim()
        if (searchCache.containsKey(cacheKey)) {
            Log.d("FoodLogViewModel", "Using cached results for: $query")
            _uiState.update { it.copy(searchResults = searchCache[cacheKey] ?: emptyList(), isSearching = false) }
            return
        }
        
        searchJob = viewModelScope.launch {
            delay(300)
            
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            
            // Rate limiting: wait if needed
            val timeSinceLastCall = System.currentTimeMillis() - lastApiCallTime
            if (timeSinceLastCall < minApiCallInterval) {
                val waitTime = minApiCallInterval - timeSinceLastCall
                Log.d("FoodLogViewModel", "Rate limiting: waiting ${waitTime}ms")
                delay(waitTime)
            }
            
            try {
                Log.d("FoodLogViewModel", "Searching for: $query")
                lastApiCallTime = System.currentTimeMillis()
                val response = NutritionApi.api.searchFoods(query)
                
                val foods = response.foods?.map { FoodItem.fromSearchResult(it) } ?: emptyList()
                Log.d("FoodLogViewModel", "Found ${foods.size} results")
                
                // Cache the results
                searchCache[cacheKey] = foods
                
                _uiState.update { it.copy(searchResults = foods, isSearching = false) }
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 429) {
                    Log.e("FoodLogViewModel", "Rate limit exceeded, please wait")
                    _uiState.update { it.copy(
                        searchResults = emptyList(), 
                        isSearching = false, 
                        searchError = "Too many requests. Please wait a moment and try again."
                    )}
                } else {
                    Log.e("FoodLogViewModel", "Search failed", e)
                    _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, searchError = e.message) }
                }
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
    
    // ==================== MEAL BUILDER FUNCTIONS ====================
    
    /**
     * Start building a custom meal with multiple ingredients
     */
    fun startMealBuilder(mealName: String = "", mealType: MealType = MealType.SNACK) {
        _uiState.update { it.copy(mealBuilder = MealBuilder(mealName = mealName, mealType = mealType)) }
        Log.d("FoodLogViewModel", "Started meal builder for: $mealName")
    }
    
    /**
     * Update the meal name in the builder
     */
    fun updateMealBuilderName(name: String) {
        _uiState.update { state ->
            state.mealBuilder?.let {
                state.copy(mealBuilder = it.copy(mealName = name))
            } ?: state
        }
    }
    
    /**
     * Update the meal type in the builder
     */
    fun updateMealBuilderType(mealType: MealType) {
        _uiState.update { state ->
            state.mealBuilder?.let {
                state.copy(mealBuilder = it.copy(mealType = mealType))
            } ?: state
        }
    }
    
    /**
     * Add an ingredient to the meal builder
     */
    fun addIngredientToBuilder(foodItem: FoodItem, servings: Float = 1f) {
        _uiState.update { state ->
            state.mealBuilder?.let { builder ->
                val newIngredient = MealIngredient(foodItem, servings)
                val updatedIngredients = builder.ingredients + newIngredient
                Log.d("FoodLogViewModel", "Added ${foodItem.name} x$servings to meal builder (${updatedIngredients.size} total)")
                state.copy(mealBuilder = builder.copy(ingredients = updatedIngredients))
            } ?: state
        }
    }
    
    /**
     * Remove an ingredient from the meal builder
     */
    fun removeIngredientFromBuilder(index: Int) {
        _uiState.update { state ->
            state.mealBuilder?.let { builder ->
                val updatedIngredients = builder.ingredients.filterIndexed { i, _ -> i != index }
                Log.d("FoodLogViewModel", "Removed ingredient at index $index")
                state.copy(mealBuilder = builder.copy(ingredients = updatedIngredients))
            } ?: state
        }
    }
    
    /**
     * Update servings for an ingredient in the meal builder
     */
    fun updateIngredientServings(index: Int, servings: Float) {
        _uiState.update { state ->
            state.mealBuilder?.let { builder ->
                val updatedIngredients = builder.ingredients.mapIndexed { i, ingredient ->
                    if (i == index) ingredient.copy(servings = servings) else ingredient
                }
                state.copy(mealBuilder = builder.copy(ingredients = updatedIngredients))
            } ?: state
        }
    }
    
    /**
     * Save the built meal to the food log
     */
    fun saveMealBuilder() {
        viewModelScope.launch {
            val builder = _uiState.value.mealBuilder ?: return@launch
            
            if (!builder.isValid) {
                Log.w("FoodLogViewModel", "Cannot save invalid meal builder")
                return@launch
            }
            
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            
            // Create a notes string with ingredient breakdown
            val ingredientList = builder.ingredients.joinToString("\n") { ingredient ->
                "${ingredient.foodItem.name} (${ingredient.servings}x ${ingredient.foodItem.servingSize ?: "serving"})"
            }
            
            val entity = FoodLogEntity(
                mealName = builder.mealName,
                calories = builder.totalCalories,
                protein = builder.totalProtein,
                carbs = builder.totalCarbs,
                fat = builder.totalFat,
                fiber = builder.totalFiber,
                sugar = builder.totalSugar,
                sodium = builder.totalSodium,
                servingSize = "1 meal",
                servings = 1f,
                mealType = builder.mealType.name,
                source = "Custom Meal (${builder.ingredients.size} ingredients)",
                notes = "Ingredients:\n$ingredientList",
                loggedTime = currentTime
            )
            
            foodLogDao.insertFoodLog(entity)
            Log.d("FoodLogViewModel", "Saved custom meal: ${builder.mealName} with ${builder.ingredients.size} ingredients")
            
            // Clear the meal builder
            _uiState.update { it.copy(mealBuilder = null) }
        }
    }
    
    /**
     * Cancel meal building and discard the builder
     */
    fun cancelMealBuilder() {
        _uiState.update { it.copy(mealBuilder = null) }
        Log.d("FoodLogViewModel", "Cancelled meal builder")
    }
    
    // ==================== END MEAL BUILDER FUNCTIONS ====================

    /**
     * Log a planned meal from the meal plan with one tap
     */
    fun logPlannedMeal(plannedMeal: PlannedMealForLog, servings: Float = 1f) {
        viewModelScope.launch {
            val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))
            
            // Use pre-calculated per-serving values from PlannedMealForLog
            val entity = FoodLogEntity(
                mealName = plannedMeal.recipeName,
                calories = (plannedMeal.estimatedCalories * servings).toInt(),
                protein = plannedMeal.estimatedProtein * servings,
                carbs = plannedMeal.estimatedCarbs * servings,
                fat = plannedMeal.estimatedFat * servings,
                fiber = 0f,  // Not tracked per-serving yet
                sugar = 0f,
                sodium = 0f,
                servingSize = "1 serving",
                servings = servings,
                mealType = plannedMeal.mealType.name,
                source = "From Meal Plan",
                fromRecipe = plannedMeal.recipeName,
                imageUrl = plannedMeal.imageUrl,
                loggedTime = currentTime
            )
            foodLogDao.insertFoodLog(entity)
            Log.d("FoodLogViewModel", "Logged planned meal: ${plannedMeal.recipeName} with ${(plannedMeal.estimatedCalories * servings).toInt()} cal")
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
    val todaysSummary: NutritionSummary = NutritionSummary(0, 0f, 0f, 0f, 0f, 0f, 0),
    // Multi-ingredient meal builder
    val mealBuilder: MealBuilder? = null
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
    val estimatedCalories: Int = 0,  // Per serving
    val estimatedProtein: Float = 0f,  // Per serving
    val estimatedCarbs: Float = 0f,  // Per serving
    val estimatedFat: Float = 0f,  // Per serving
    val estimatedServings: Int = 4,
    val totalCalories: Int = 0,  // Full recipe
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

/**
 * Meal builder for creating custom meals with multiple ingredients
 */
data class MealBuilder(
    val mealName: String = "",
    val ingredients: List<MealIngredient> = emptyList(),
    val mealType: MealType = MealType.SNACK
) {
    val totalCalories: Int get() = ingredients.sumOf { it.totalCalories }
    val totalProtein: Float get() = ingredients.sumOf { it.totalProtein.toDouble() }.toFloat()
    val totalCarbs: Float get() = ingredients.sumOf { it.totalCarbs.toDouble() }.toFloat()
    val totalFat: Float get() = ingredients.sumOf { it.totalFat.toDouble() }.toFloat()
    val totalFiber: Float get() = ingredients.sumOf { it.totalFiber.toDouble() }.toFloat()
    val totalSugar: Float get() = ingredients.sumOf { it.totalSugar.toDouble() }.toFloat()
    val totalSodium: Float get() = ingredients.sumOf { it.totalSodium.toDouble() }.toFloat()
    
    val isValid: Boolean get() = mealName.isNotBlank() && ingredients.isNotEmpty()
}

/**
 * Individual ingredient in a meal builder
 */
data class MealIngredient(
    val foodItem: FoodItem,
    val servings: Float = 1f
) {
    val totalCalories: Int get() = (foodItem.calories * servings).toInt()
    val totalProtein: Float get() = foodItem.protein * servings
    val totalCarbs: Float get() = foodItem.carbs * servings
    val totalFat: Float get() = foodItem.fat * servings
    val totalFiber: Float get() = foodItem.fiber * servings
    val totalSugar: Float get() = foodItem.sugar * servings
    val totalSodium: Float get() = foodItem.sodium * servings
}