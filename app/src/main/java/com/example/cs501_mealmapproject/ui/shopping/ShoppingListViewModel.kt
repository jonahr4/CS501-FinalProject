package com.example.cs501_mealmapproject.ui.shopping

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_mealmapproject.network.MealApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.util.Log
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("meal_plan_prefs", Context.MODE_PRIVATE)
    private val shoppingPrefs = application.getSharedPreferences("shopping_list_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    // Cache for ingredient categories from API
    private var ingredientCategoryCache: Map<String, String> = emptyMap()

    init {
        // Load saved shopping list state on initialization
        loadShoppingListState()
    }

    fun toggleItem(sectionIndex: Int, itemIndex: Int, checked: Boolean) {
        _uiState.update { state ->
            val newSections = state.sections.mapIndexed { sIdx, section ->
                if (sIdx != sectionIndex) return@mapIndexed section
                val newItems = section.items.mapIndexed { iIdx, item ->
                    if (iIdx == itemIndex && !item.isHeader) item.copy(checked = checked) else item
                }
                section.copy(items = newItems)
            }
            state.copy(sections = newSections)
        }
        saveShoppingListState()
    }

    fun clearCheckedItems() {
        // Collect the removal keys of items being removed (instanceId::ingredientName)
        val itemsBeingRemoved = mutableListOf<String>()
        
        _uiState.update { state ->
            // First, collect removal keys of checked items (non-headers, non-personal items)
            state.sections.forEach { section ->
                if (section.title != "Personal Items") {
                    section.items.filter { it.checked && !it.isHeader }.forEach { item ->
                        // Only track items with an instanceId (items from meal plan)
                        if (item.instanceId.isNotEmpty()) {
                            val removalKey = "${item.instanceId}::${item.name}"
                            itemsBeingRemoved.add(removalKey)
                        }
                    }
                }
            }
            
            val newSections = state.sections.mapNotNull { section ->
                // First, filter out all checked items
                val uncheckedItems = section.items.filter { !it.checked }
                
                Log.d("ShoppingListVM", "Section ${section.title}: ${uncheckedItems.size} unchecked items")
                
                // Now remove orphaned headers
                // A header is orphaned if all its sub-items (items starting with "  • ") are gone
                val cleanedItems = mutableListOf<ShoppingItem>()
                
                for (i in uncheckedItems.indices) {
                    val item = uncheckedItems[i]
                    
                    if (item.isHeader) {
                        // Look ahead to see if there are any sub-items (starting with "  • ") before the next header
                        var hasSubItems = false
                        for (j in (i + 1) until uncheckedItems.size) {
                            val nextItem = uncheckedItems[j]
                            if (nextItem.isHeader) {
                                // Hit another header, stop looking
                                break
                            }
                            // Check if this is a sub-item (starts with bullet point)
                            if (nextItem.name.startsWith("  • ")) {
                                hasSubItems = true
                                break
                            }
                        }
                        
                        // Only add the header if it has sub-items under it
                        if (hasSubItems) {
                            cleanedItems.add(item)
                        }
                    } else {
                        // Regular item, always add
                        cleanedItems.add(item)
                    }
                }
                
                Log.d("ShoppingListVM", "After cleanup: ${cleanedItems.size} items")
                
                if (cleanedItems.isNotEmpty()) {
                    section.copy(items = cleanedItems)
                } else {
                    null 
                }
            }
            state.copy(sections = newSections)
        }
        
        // Save removed items so they don't come back when meal plan changes
        if (itemsBeingRemoved.isNotEmpty()) {
            addToRemovedItems(itemsBeingRemoved)
            Log.d("ShoppingListVM", "Marked ${itemsBeingRemoved.size} items as removed")
        }
        
        saveShoppingListState()
    }

    fun addManualItem(itemName: String) {
        if (itemName.isBlank()) return
        _uiState.update { state ->
            val manualSectionTitle = "Personal Items"
            val existingSectionIndex = state.sections.indexOfFirst { it.title == manualSectionTitle }
            
            val newItem = ShoppingItem(itemName, checked = false, isHeader = false)
            
            val newSections = if (existingSectionIndex != -1) {
                state.sections.mapIndexed { index, section ->
                    if (index == existingSectionIndex) {
                        section.copy(items = section.items + newItem)
                    } else {
                        section
                    }
                }
            } else {
                val newSection = ShoppingSection(manualSectionTitle, listOf(newItem))
                listOf(newSection) + state.sections
            }
            state.copy(sections = newSections)
        }
        saveShoppingListState()
    }

    fun generateShoppingListFromMealPlan() {
        viewModelScope.launch {
            // Check if meal plan has changed since last generation
            val currentMealPlanHash = getMealPlanHash()
            val savedMealPlanHash = shoppingPrefs.getString("last_meal_plan_hash", null)
            
            // If meal plan hasn't changed and we have saved state, don't regenerate
            if (currentMealPlanHash == savedMealPlanHash && _uiState.value.sections.isNotEmpty()) {
                Log.d("ShoppingListVM", "Meal plan unchanged, using saved shopping list")
                return@launch
            }
            
            Log.d("ShoppingListVM", "Meal plan changed, merging new items with existing list")
            
            // 1. Fetch Ingredient Dictionary if not cached
            if (ingredientCategoryCache.isEmpty()) {
                try {
                    Log.d("ShoppingListVM", "Fetching ingredient dictionary from API...")
                    val response = MealApi.retrofitService.getAllIngredients()
                    ingredientCategoryCache = response.ingredients?.associate { 
                        it.strIngredient.lowercase() to (it.strType ?: "Misc") 
                    } ?: emptyMap()
                    Log.d("ShoppingListVM", "Loaded ${ingredientCategoryCache.size} ingredients into dictionary")
                } catch (e: Exception) {
                    Log.e("ShoppingListVM", "Failed to fetch ingredient dictionary", e)
                }
            }

            val mealInstances = loadMealInstancesFromMealPlan()
            
            if (mealInstances.isEmpty()) {
                Log.d("ShoppingListVM", "No recipes in meal plan")
                _uiState.update { it.copy(sections = emptyList()) }
                saveShoppingListState()
                shoppingPrefs.edit()
                    .putString("last_meal_plan_hash", currentMealPlanHash)
                    .putString("removed_items", "[]")
                    .apply()
                return@launch
            }

            // Map: recipeName -> list of (ingredient, instanceId)
            val allRecipeIngredients = mutableMapOf<String, MutableList<Pair<String, String>>>() 
            
            // Cache for recipe ingredients (so we don't fetch same recipe multiple times)
            val recipeIngredientsCache = mutableMapOf<String, List<String>>()
            
            for (mealInstance in mealInstances) {
                try {
                    // Use cache if available
                    val ingredients = recipeIngredientsCache.getOrPut(mealInstance.recipeName) {
                        val response = MealApi.retrofitService.searchMeals(mealInstance.recipeName)
                        val meal = response.meals?.firstOrNull()
                        if (meal != null) extractIngredients(meal) else emptyList()
                    }
                    
                    if (ingredients.isNotEmpty()) {
                        // Store with instanceId instead of just recipeName
                        allRecipeIngredients.getOrPut(mealInstance.recipeName) { mutableListOf() }
                            .addAll(ingredients.map { it to mealInstance.instanceId })
                    }
                } catch (e: Exception) {
                    Log.e("ShoppingListVM", "Failed to fetch recipe ${mealInstance.recipeName}", e)
                }
            }

            val categorizedIngredients = smartCategorizeIngredients(allRecipeIngredients)
            
            // Load the set of items that user has previously removed (now tracked by instanceId)
            val removedItems = loadRemovedItems()
            Log.d("ShoppingListVM", "Previously removed items: ${removedItems.size}")
            
            // Get current meal instance IDs to clean up old removed items
            val currentInstanceIds = mealInstances.map { it.instanceId }.toSet()
            cleanupOldRemovedItems(currentInstanceIds)
            
            val newSections = categorizedIngredients.map { (category, items) ->
                // Filter out items that user has previously removed
                val filteredItems = filterRemovedItems(items, removedItems)
                ShoppingSection(
                    title = category,
                    items = filteredItems
                )
            }.filter { it.items.isNotEmpty() }

            _uiState.update { currentState ->
                val manualSection = currentState.sections.find { it.title == "Personal Items" }
                val finalSections = if (manualSection != null) {
                    listOf(manualSection) + newSections
                } else {
                    newSections
                }
                currentState.copy(sections = finalSections)
            }
            
            // Save the new state and hash
            saveShoppingListState()
            shoppingPrefs.edit().putString("last_meal_plan_hash", currentMealPlanHash).apply()
        }
    }
    
    /**
     * Clean up removed items for meal instances that no longer exist in the meal plan
     */
    private fun cleanupOldRemovedItems(currentInstanceIds: Set<String>) {
        try {
            val removedItems = loadRemovedItems()
            // Only keep removed items whose instanceId is still in the current meal plan
            val validRemovedItems = removedItems.filter { removedKey ->
                // Format: "instanceId::ingredientName"
                val instanceId = removedKey.substringBefore("::")
                currentInstanceIds.contains(instanceId)
            }.toSet()
            
            if (validRemovedItems.size != removedItems.size) {
                val arr = JSONArray(validRemovedItems.toList())
                shoppingPrefs.edit().putString("removed_items", arr.toString()).apply()
                Log.d("ShoppingListVM", "Cleaned up ${removedItems.size - validRemovedItems.size} old removed items")
            }
        } catch (e: Exception) {
            Log.e("ShoppingListVM", "Failed to cleanup old removed items", e)
        }
    }
    
    /**
     * Filter out items that were previously removed by the user.
     * Also cleans up orphaned headers after filtering.
     * Removed items are tracked by "instanceId::ingredientName" format.
     */
    private fun filterRemovedItems(items: List<ShoppingItem>, removedItems: Set<String>): List<ShoppingItem> {
        // First pass: filter out removed items (but keep headers for now)
        val filtered = items.filter { item ->
            if (item.isHeader) {
                true // Keep headers initially, we'll clean orphans later
            } else {
                // Check if this specific instance's item was removed
                // Format: "instanceId::ingredientName"
                val removalKey = "${item.instanceId}::${item.name}"
                val isRemoved = removedItems.contains(removalKey)
                if (isRemoved) {
                    Log.d("ShoppingListVM", "Filtering out previously removed item: ${item.name} (instance: ${item.instanceId})")
                }
                !isRemoved
            }
        }
        
        // Second pass: remove orphaned headers (headers with no sub-items)
        val cleanedItems = mutableListOf<ShoppingItem>()
        for (i in filtered.indices) {
            val item = filtered[i]
            if (item.isHeader) {
                // Look ahead to see if there are any sub-items before the next header
                var hasSubItems = false
                for (j in (i + 1) until filtered.size) {
                    val nextItem = filtered[j]
                    if (nextItem.isHeader) break
                    if (nextItem.name.startsWith("  • ")) {
                        hasSubItems = true
                        break
                    }
                }
                if (hasSubItems) {
                    cleanedItems.add(item)
                }
            } else {
                cleanedItems.add(item)
            }
        }
        
        return cleanedItems
    }
    
    /**
     * Load the set of item names that user has removed
     */
    private fun loadRemovedItems(): Set<String> {
        return try {
            val json = shoppingPrefs.getString("removed_items", "[]") ?: "[]"
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (e: Exception) {
            Log.e("ShoppingListVM", "Failed to load removed items", e)
            emptySet()
        }
    }
    
    /**
     * Save an item to the removed items set
     */
    private fun addToRemovedItems(itemNames: List<String>) {
        try {
            val existing = loadRemovedItems().toMutableSet()
            existing.addAll(itemNames)
            val arr = JSONArray(existing.toList())
            shoppingPrefs.edit().putString("removed_items", arr.toString()).apply()
            Log.d("ShoppingListVM", "Saved ${existing.size} removed items")
        } catch (e: Exception) {
            Log.e("ShoppingListVM", "Failed to save removed items", e)
        }
    }
    
    private fun getMealPlanHash(): String {
        val raw = prefs.getString("meal_plan_serialized_v1", null) ?: return ""
        return raw.hashCode().toString()
    }
    
    private fun saveShoppingListState() {
        try {
            val sectionsJson = JSONArray()
            _uiState.value.sections.forEach { section ->
                val sectionJson = JSONObject().apply {
                    put("title", section.title)
                    val itemsJson = JSONArray()
                    section.items.forEach { item ->
                        val itemJson = JSONObject().apply {
                            put("name", item.name)
                            put("checked", item.checked)
                            put("isHeader", item.isHeader)
                            put("instanceId", item.instanceId)
                        }
                        itemsJson.put(itemJson)
                    }
                    put("items", itemsJson)
                }
                sectionsJson.put(sectionJson)
            }
            shoppingPrefs.edit().putString("shopping_list_state", sectionsJson.toString()).apply()
            Log.d("ShoppingListVM", "Saved shopping list state: ${_uiState.value.sections.size} sections")
        } catch (e: Exception) {
            Log.e("ShoppingListVM", "Failed to save shopping list state", e)
        }
    }
    
    private fun loadShoppingListState() {
        try {
            val json = shoppingPrefs.getString("shopping_list_state", null) ?: return
            val sectionsJson = JSONArray(json)
            val sections = mutableListOf<ShoppingSection>()
            
            for (i in 0 until sectionsJson.length()) {
                val sectionJson = sectionsJson.getJSONObject(i)
                val title = sectionJson.getString("title")
                val itemsJson = sectionJson.getJSONArray("items")
                val items = mutableListOf<ShoppingItem>()
                
                for (j in 0 until itemsJson.length()) {
                    val itemJson = itemsJson.getJSONObject(j)
                    items.add(ShoppingItem(
                        name = itemJson.getString("name"),
                        checked = itemJson.getBoolean("checked"),
                        isHeader = itemJson.getBoolean("isHeader"),
                        instanceId = itemJson.optString("instanceId", "")
                    ))
                }
                
                sections.add(ShoppingSection(title, items))
            }
            
            _uiState.update { it.copy(sections = sections) }
            Log.d("ShoppingListVM", "Loaded shopping list state: ${sections.size} sections")
        } catch (e: Exception) {
            Log.e("ShoppingListVM", "Failed to load shopping list state", e)
        }
    }

    private fun smartCategorizeIngredients(recipeIngredients: Map<String, List<Pair<String, String>>>): Map<String, List<ShoppingItem>> {
        // Store tuples of (ingredient, recipeName, instanceId)
        data class IngredientInfo(val ingredient: String, val recipeName: String, val instanceId: String)
        
        val proteins = mutableMapOf<String, MutableList<IngredientInfo>>() 
        val produce = mutableMapOf<String, MutableList<IngredientInfo>>()
        val dairy = mutableMapOf<String, MutableList<IngredientInfo>>() 
        val spices = mutableMapOf<String, MutableList<IngredientInfo>>() 
        val pantry = mutableMapOf<String, MutableList<IngredientInfo>>() 
        val misc = mutableMapOf<String, MutableList<IngredientInfo>>() 
        
        // Backup keywords (still useful for fallback or offline)
        val spiceKeywords = listOf("salt", "pepper", "cumin", "coriander", "turmeric", "paprika", "chili", "cinnamon", 
                                   "cardamom", "garam masala", "bay", "thyme", "oregano", "basil", "parsley", "seeds",
                                   "nutmeg", "cloves", "ginger powder")
        val proteinKeywords = listOf("chicken", "beef", "pork", "fish", "lamb", "shrimp", "tofu", "egg", "bacon", "ham", "turkey", "salmon", "tuna")
        val produceKeywords = listOf("onion", "garlic", "tomato", "pepper", "ginger", "vegetable", "carrot", "celery", 
                                     "lettuce", "spinach", "potato", "lemon", "lime", "cucumber", "avocado", "mushroom", "bean", "corn")
        val dairyKeywords = listOf("milk", "cream", "yogurt", "cheese", "butter", "ghee", "parmesan", "cheddar", "mozzarella")
        val pantryKeywords = listOf("rice", "flour", "oil", "sugar", "water", "stock", "sauce", "vinegar", "honey", 
                                    "syrup", "mustard", "ketchup", "mayo", "bread", "pasta", "noodle", "yeast", "baking")

        recipeIngredients.forEach { (recipeName, ingredients) ->
            ingredients.forEach { (ingredient, instanceId) ->
                val lowerIng = ingredient.lowercase()
                val info = IngredientInfo(ingredient, recipeName, instanceId)
                
                // 1. Try API Lookup first
                val apiType = ingredientCategoryCache[lowerIng] 
                    ?: ingredientCategoryCache.entries.find { lowerIng.contains(it.key) }?.value

                var categorized = false

                if (apiType != null) {
                    when (apiType) {
                        "Meat", "Poultry", "Seafood", "Pork", "Beef", "Chicken", "Lamb" -> {
                             val base = extractIngredientBase(ingredient, proteinKeywords)
                             proteins.getOrPut(base) { mutableListOf() }.add(info)
                             categorized = true
                        }
                        "Vegetable", "Fruit" -> {
                             val base = extractIngredientBase(ingredient, produceKeywords)
                             produce.getOrPut(base) { mutableListOf() }.add(info)
                             categorized = true
                        }
                        "Dairy" -> {
                             val base = extractIngredientBase(ingredient, dairyKeywords)
                             dairy.getOrPut(base) { mutableListOf() }.add(info)
                             categorized = true
                        }
                        "Spice", "Herb" -> {
                             val base = extractIngredientBase(ingredient, spiceKeywords)
                             spices.getOrPut(base) { mutableListOf() }.add(info)
                             categorized = true
                        }
                        "Pasta", "Grain", "Cereal", "Baking", "Jam", "Sauce" -> {
                             val base = extractIngredientBase(ingredient, pantryKeywords)
                             pantry.getOrPut(base) { mutableListOf() }.add(info)
                             categorized = true
                        }
                    }
                }

                // 2. Fallback to Keywords if API didn't catch it
                if (!categorized) {
                    when {
                        proteinKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, proteinKeywords)
                            proteins.getOrPut(base) { mutableListOf() }.add(info)
                        }
                        produceKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, produceKeywords)
                            produce.getOrPut(base) { mutableListOf() }.add(info)
                        }
                        dairyKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, dairyKeywords)
                            dairy.getOrPut(base) { mutableListOf() }.add(info)
                        }
                        spiceKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, spiceKeywords)
                            spices.getOrPut(base) { mutableListOf() }.add(info)
                        }
                        pantryKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, pantryKeywords)
                            pantry.getOrPut(base) { mutableListOf() }.add(info)
                        }
                        else -> {
                            // 3. Finally, misc
                            misc.getOrPut(ingredient) { mutableListOf() }.add(info)
                        }
                    }
                }
            }
        }

        // Helper to format display text based on whether this specific recipe has duplicates
        // hasDuplicates means the SAME recipe appears multiple times (e.g., Chicken Handi on Dec 11 and Dec 13)
        fun formatDisplayText(info: IngredientInfo, showDateSlot: Boolean): String {
            return if (showDateSlot) {
                // Show date/slot to distinguish same recipe on different days: "1 kg Chicken (Chicken Handi - Dec 11 Lunch)"
                val mealSlotLabel = formatMealSlotLabel(info.instanceId)
                if (mealSlotLabel.isNotEmpty()) {
                    "${info.ingredient} (${info.recipeName} - $mealSlotLabel)"
                } else {
                    "${info.ingredient} (${info.recipeName})"
                }
            } else {
                // No duplicates for this recipe, just show recipe name: "1 kg Chicken (Chicken Handi)"
                "${info.ingredient} (${info.recipeName})"
            }
        }

        // Build item list with instanceId preserved - only show date/slot when SAME recipe appears multiple times
        fun buildItemList(map: Map<String, MutableList<IngredientInfo>>): List<ShoppingItem> {
            return map.flatMap { (base, items) ->
                // Count occurrences of each recipe name - only show date/slot if same recipe appears twice
                val recipeCount = items.groupBy { it.recipeName }.mapValues { it.value.size }
                
                if (items.size == 1) {
                    val displayText = formatDisplayText(items[0], false)
                    listOf(ShoppingItem(displayText, false, isHeader = false, instanceId = items[0].instanceId))
                } else {
                    listOf(ShoppingItem("$base:", false, isHeader = true)) + 
                    items.map { info ->
                        // Only show date/slot if this specific recipe appears more than once
                        val showDateSlot = (recipeCount[info.recipeName] ?: 0) > 1
                        val displayText = formatDisplayText(info, showDateSlot)
                        ShoppingItem("  • $displayText", false, isHeader = false, instanceId = info.instanceId) 
                    }
                }
            }
        }
        
        fun buildDairyList(map: Map<String, MutableList<IngredientInfo>>): List<ShoppingItem> {
            return map.flatMap { (base, items) ->
                // Count occurrences of each recipe name - only show date/slot if same recipe appears twice
                val recipeCount = items.groupBy { it.recipeName }.mapValues { it.value.size }
                
                if (items.size == 1) {
                    val displayText = formatDisplayText(items[0], false)
                    listOf(ShoppingItem(displayText, false, isHeader = false, instanceId = items[0].instanceId))
                } else {
                    listOf(ShoppingItem("$base: (buy one container)", false, isHeader = true)) + 
                    items.map { info ->
                        // Only show date/slot if this specific recipe appears more than once
                        val showDateSlot = (recipeCount[info.recipeName] ?: 0) > 1
                        val displayText = formatDisplayText(info, showDateSlot)
                        ShoppingItem("  • $displayText", false, isHeader = false, instanceId = info.instanceId) 
                    }
                }
            }
        }

        val result = mutableMapOf<String, List<ShoppingItem>>()
        if (proteins.isNotEmpty()) result["Proteins & Meat"] = buildItemList(proteins)
        if (produce.isNotEmpty()) result["Produce"] = buildItemList(produce)
        if (dairy.isNotEmpty()) result["Dairy"] = buildDairyList(dairy)
        if (spices.isNotEmpty()) {
            val sortedSpices = spices.entries.sortedWith(
                compareByDescending<Map.Entry<String, MutableList<IngredientInfo>>> { it.value.size > 1 }.thenBy { it.key }
            ).associate { it.key to it.value }
            result["Spices & Seasonings"] = buildDairyList(sortedSpices)
        }
        if (pantry.isNotEmpty()) result["Pantry & Staples"] = buildDairyList(pantry)
        if (misc.isNotEmpty()) result["Other Items"] = buildItemList(misc)

        return result
    }

    private fun extractIngredientBase(ingredient: String, keywords: List<String>): String {
        val lower = ingredient.lowercase()
        val matchedKeyword = keywords
            .filter { lower.contains(it) }
            .maxByOrNull { it.length }
        
        return when {
            matchedKeyword != null -> {
                val baseName = matchedKeyword.replaceFirstChar { it.uppercase() }
                when {
                    lower.contains("$matchedKeyword seeds") -> "$baseName Seeds"
                    lower.contains("$matchedKeyword powder") -> "$baseName Powder"
                    lower.contains("garam masala") -> "Garam Masala"
                    else -> baseName
                }
            }
            else -> ingredient
        }
    }
    
    /**
     * Format a human-readable label from the instanceId for display
     * instanceId format: "2025-12-11|Lunch|Chicken Handi"
     * Returns: "Dec 11 Lunch" or "Thu Lunch" for a nicer display
     */
    private fun formatMealSlotLabel(instanceId: String): String {
        return try {
            val parts = instanceId.split("|")
            if (parts.size < 2) return ""
            
            val dateStr = parts[0]
            val mealType = parts[1]
            
            // Parse the date and format it nicely
            val date = LocalDate.parse(dateStr)
            val dayOfWeek = date.dayOfWeek.toString().lowercase().replaceFirstChar { it.uppercase() }.take(3)
            val month = date.month.toString().lowercase().replaceFirstChar { it.uppercase() }.take(3)
            val dayOfMonth = date.dayOfMonth
            
            "$month $dayOfMonth $mealType"
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractIngredients(meal: com.example.cs501_mealmapproject.network.MealDto): List<String> {
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

        val result = mutableListOf<String>()
        for (i in ingredients.indices) {
            val ing = ingredients[i]?.trim().orEmpty()
            if (ing.isEmpty() || ing.equals("null", ignoreCase = true)) continue
            val measure = measures.getOrNull(i)?.trim().orEmpty()
            val entry = if (measure.isNotEmpty() && !measure.equals("null", ignoreCase = true)) {
                "$measure $ing".trim()
            } else {
                ing
            }
            result.add(entry)
        }
        return result
    }

    private fun loadMealInstancesFromMealPlan(): List<MealInstance> {
        val raw = prefs.getString("meal_plan_serialized_v1", null) ?: return emptyList()
        return try {
            val mealInstances = mutableListOf<MealInstance>()
            raw.lines().forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = line.split('|')
                if (parts.size < 3) return@forEach
                val date = parts[0]
                val mealType = parts[1]
                val recipe = parts.subList(2, parts.size).joinToString("|").replace("\\|", "|")
                if (recipe != "Tap to add a recipe" && recipe.isNotBlank()) {
                    mealInstances.add(MealInstance(date, mealType, recipe))
                }
            }
            mealInstances
        } catch (e: Exception) {
            Log.w("ShoppingListVM", "Failed to load meal plan: ${e.message}")
            emptyList()
        }
    }
}

data class ShoppingListUiState(
    val sections: List<ShoppingSection> = emptyList()
)

data class ShoppingSection(
    val title: String,
    val items: List<ShoppingItem>
)

data class ShoppingItem(
    val name: String,
    val checked: Boolean,
    val isHeader: Boolean = false,
    // Instance ID tracks which meal slot this item belongs to (e.g., "2025-12-11|Lunch|Chicken Handi")
    // This allows us to track removals per meal instance, not globally
    val instanceId: String = ""
)

// Represents a meal instance in the meal plan
data class MealInstance(
    val date: String,
    val mealType: String,
    val recipeName: String
) {
    // Unique ID for this meal instance
    val instanceId: String get() = "$date|$mealType|$recipeName"
}
