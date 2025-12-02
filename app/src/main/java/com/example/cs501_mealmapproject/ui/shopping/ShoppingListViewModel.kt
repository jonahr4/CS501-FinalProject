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
import java.time.LocalDate

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("meal_plan_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    // Cache for ingredient categories from API
    private var ingredientCategoryCache: Map<String, String> = emptyMap()

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
    }

    fun clearCheckedItems() {
        _uiState.update { state ->
            val newSections = state.sections.mapNotNull { section ->
                val uncheckedItems = section.items.filter { !it.checked }
                if (uncheckedItems.isNotEmpty()) {
                    section.copy(items = uncheckedItems)
                } else {
                    null 
                }
            }
            state.copy(sections = newSections)
        }
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
    }

    fun generateShoppingListFromMealPlan() {
        viewModelScope.launch {
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

            val recipeNames = loadRecipeNamesFromMealPlan()
            
            if (recipeNames.isEmpty()) {
                Log.d("ShoppingListVM", "No recipes in meal plan")
                 _uiState.update { it.copy(sections = emptyList()) }
                return@launch
            }

            val allRecipeIngredients = mutableMapOf<String, MutableList<Pair<String, String>>>() 
            
            for (recipeName in recipeNames) {
                try {
                    val response = MealApi.retrofitService.searchMeals(recipeName)
                    val meal = response.meals?.firstOrNull()
                    
                    if (meal != null) {
                        val ingredients = extractIngredients(meal)
                        allRecipeIngredients[recipeName] = ingredients.map { it to recipeName }.toMutableList()
                    }
                } catch (e: Exception) {
                    Log.e("ShoppingListVM", "Failed to fetch recipe $recipeName", e)
                }
            }

            val categorizedIngredients = smartCategorizeIngredients(allRecipeIngredients)
            
            val sections = categorizedIngredients.map { (category, items) ->
                ShoppingSection(
                    title = category,
                    items = items
                )
            }

            _uiState.update { currentState ->
                val manualSection = currentState.sections.find { it.title == "Personal Items" }
                val finalSections = if (manualSection != null) {
                    listOf(manualSection) + sections
                } else {
                    sections
                }
                currentState.copy(sections = finalSections)
            }
        }
    }

    private fun smartCategorizeIngredients(recipeIngredients: Map<String, List<Pair<String, String>>>): Map<String, List<ShoppingItem>> {
        val proteins = mutableMapOf<String, MutableList<String>>() 
        val produce = mutableMapOf<String, MutableList<String>>()
        val dairy = mutableMapOf<String, MutableList<String>>() 
        val spices = mutableMapOf<String, MutableList<String>>() 
        val pantry = mutableMapOf<String, MutableList<String>>() 
        val misc = mutableMapOf<String, MutableList<String>>() 
        
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
            ingredients.forEach { (ingredient, _) ->
                val lowerIng = ingredient.lowercase()
                
                // 1. Try API Lookup first
                // We look for exact match or substring match in dictionary
                val apiType = ingredientCategoryCache[lowerIng] 
                    ?: ingredientCategoryCache.entries.find { lowerIng.contains(it.key) }?.value

                var categorized = false

                if (apiType != null) {
                    when (apiType) {
                        "Meat", "Poultry", "Seafood", "Pork", "Beef", "Chicken", "Lamb" -> {
                             val base = extractIngredientBase(ingredient, proteinKeywords) // use keywords for base extraction still, or just use name
                             proteins.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                             categorized = true
                        }
                        "Vegetable", "Fruit" -> {
                             val base = extractIngredientBase(ingredient, produceKeywords)
                             produce.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                             categorized = true
                        }
                        "Dairy" -> {
                             val base = extractIngredientBase(ingredient, dairyKeywords)
                             dairy.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                             categorized = true
                        }
                        "Spice", "Herb" -> {
                             val base = extractIngredientBase(ingredient, spiceKeywords)
                             spices.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                             categorized = true
                        }
                        "Pasta", "Grain", "Cereal", "Baking", "Jam", "Sauce" -> {
                             val base = extractIngredientBase(ingredient, pantryKeywords)
                             pantry.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                             categorized = true
                        }
                    }
                }

                // 2. Fallback to Keywords if API didn't catch it
                if (!categorized) {
                    when {
                        proteinKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, proteinKeywords)
                            proteins.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                        }
                        produceKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, produceKeywords)
                            produce.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                        }
                        dairyKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, dairyKeywords)
                            dairy.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                        }
                        spiceKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, spiceKeywords)
                            spices.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                        }
                        pantryKeywords.any { lowerIng.contains(it) } -> {
                            val base = extractIngredientBase(ingredient, pantryKeywords)
                            pantry.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                        }
                        else -> {
                            // 3. Finally, misc
                            misc.getOrPut(ingredient) { mutableListOf() }.add("$ingredient (for $recipeName)")
                        }
                    }
                }
            }
        }

        fun buildItemList(map: Map<String, MutableList<String>>): List<ShoppingItem> {
            return map.flatMap { (base, amounts) ->
                if (amounts.size == 1) {
                    listOf(ShoppingItem(amounts[0], false))
                } else {
                    listOf(ShoppingItem("$base:", false, isHeader = true)) + 
                    amounts.map { ShoppingItem("  • $it", false) }
                }
            }
        }
        
        fun buildDairyList(map: Map<String, MutableList<String>>): List<ShoppingItem> {
            return map.flatMap { (base, amounts) ->
                if (amounts.size == 1) {
                    listOf(ShoppingItem(amounts[0], false))
                } else {
                    listOf(ShoppingItem("$base: (buy one container)", false, isHeader = true)) + 
                    amounts.map { ShoppingItem("  • $it", false) }
                }
            }
        }

        val result = mutableMapOf<String, List<ShoppingItem>>()
        if (proteins.isNotEmpty()) result["Proteins & Meat"] = buildItemList(proteins)
        if (produce.isNotEmpty()) result["Produce"] = buildItemList(produce)
        if (dairy.isNotEmpty()) result["Dairy"] = buildDairyList(dairy)
        if (spices.isNotEmpty()) {
            val sortedSpices = spices.entries.sortedWith(
                compareByDescending<Map.Entry<String, MutableList<String>>> { it.value.size > 1 }.thenBy { it.key }
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

    private fun loadRecipeNamesFromMealPlan(): List<String> {
        val raw = prefs.getString("meal_plan_serialized_v1", null) ?: return emptyList()
        return try {
            val recipeNames = mutableSetOf<String>()
            raw.lines().forEach { line ->
                if (line.isBlank()) return@forEach
                val parts = line.split('|')
                if (parts.size < 3) return@forEach
                val recipe = parts.subList(2, parts.size).joinToString("|").replace("\\|", "|")
                if (recipe != "Tap to add a recipe" && recipe.isNotBlank()) {
                    recipeNames.add(recipe)
                }
            }
            recipeNames.toList()
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
    val isHeader: Boolean = false
)
