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

    fun toggleItem(sectionIndex: Int, itemIndex: Int, checked: Boolean) {
        _uiState.update { state ->
            val newSections = state.sections.mapIndexed { sIdx, section ->
                if (sIdx != sectionIndex) return@mapIndexed section
                val newItems = section.items.mapIndexed { iIdx, item ->
                    if (iIdx == itemIndex) item.copy(checked = checked) else item
                }
                section.copy(items = newItems)
            }
            state.copy(sections = newSections)
        }
    }

    fun generateShoppingListFromMealPlan() {
        viewModelScope.launch {
            val recipeNames = loadRecipeNamesFromMealPlan()
            
            if (recipeNames.isEmpty()) {
                Log.d("ShoppingListVM", "No recipes in meal plan, using demo data")
                _uiState.update { it.copy(sections = demoSections) }
                return@launch
            }

            Log.d("ShoppingListVM", "Found ${recipeNames.size} recipes in meal plan: $recipeNames")

            // Collect all ingredients across all recipes
            val allRecipeIngredients = mutableMapOf<String, MutableList<Pair<String, String>>>() // recipe -> ingredients
            
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

            // Smart ingredient grouping by category
            val categorizedIngredients = smartCategorizeIngredients(allRecipeIngredients)
            
            val sections = categorizedIngredients.map { (category, items) ->
                ShoppingSection(
                    title = category,
                    items = items.map { ShoppingItem(it, checked = false) }
                )
            }

            _uiState.update { it.copy(sections = sections) }
        }
    }

    private fun smartCategorizeIngredients(recipeIngredients: Map<String, List<Pair<String, String>>>): Map<String, List<String>> {
        val proteins = mutableMapOf<String, MutableList<String>>() // ingredient base -> amounts with recipe
        val produce = mutableMapOf<String, MutableList<String>>()
        val dairy = mutableMapOf<String, MutableList<String>>() // Combine amounts
        val spices = mutableMapOf<String, MutableList<String>>() // Combine amounts
        val pantry = mutableMapOf<String, MutableList<String>>() // Combine amounts
        
        val spiceKeywords = listOf("salt", "pepper", "cumin", "coriander", "turmeric", "paprika", "chili", "cinnamon", 
                                   "cardamom", "garam masala", "bay", "thyme", "oregano", "basil", "parsley", "seeds")
        val proteinKeywords = listOf("chicken", "beef", "pork", "fish", "lamb", "shrimp", "tofu", "egg")
        val produceKeywords = listOf("onion", "garlic", "tomato", "pepper", "ginger", "vegetable", "carrot", "celery", "lettuce")
        val dairyKeywords = listOf("milk", "cream", "yogurt", "cheese", "butter", "ghee")
        val pantryKeywords = listOf("rice", "flour", "oil", "sugar", "water", "stock", "sauce", "vinegar")

        recipeIngredients.forEach { (recipeName, ingredients) ->
            ingredients.forEach { (ingredient, _) ->
                val lowerIng = ingredient.lowercase()
                
                when {
                    // Proteins - keep separate per recipe (portions matter)
                    proteinKeywords.any { lowerIng.contains(it) } -> {
                        val base = extractIngredientBase(ingredient, proteinKeywords)
                        proteins.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                    }
                    // Produce - combine but show recipes
                    produceKeywords.any { lowerIng.contains(it) } -> {
                        val base = extractIngredientBase(ingredient, produceKeywords)
                        produce.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                    }
                    // Dairy - combine and show total (buy one container)
                    dairyKeywords.any { lowerIng.contains(it) } -> {
                        val base = extractIngredientBase(ingredient, dairyKeywords)
                        dairy.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                    }
                    // Spices - combine amounts (buy one container)
                    spiceKeywords.any { lowerIng.contains(it) } -> {
                        val base = extractIngredientBase(ingredient, spiceKeywords)
                        spices.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                    }
                    // Pantry staples - combine (buy one container)
                    pantryKeywords.any { lowerIng.contains(it) } -> {
                        val base = extractIngredientBase(ingredient, pantryKeywords)
                        pantry.getOrPut(base) { mutableListOf() }.add("$ingredient (for $recipeName)")
                    }
                }
            }
        }

        return buildMap {
            if (proteins.isNotEmpty()) {
                put("Proteins & Meat", proteins.flatMap { (base, amounts) ->
                    if (amounts.size == 1) listOf(amounts[0])
                    else listOf("$base:") + amounts.map { "  • $it" }
                })
            }
            if (produce.isNotEmpty()) {
                put("Produce", produce.flatMap { (base, amounts) ->
                    if (amounts.size == 1) listOf(amounts[0])
                    else listOf("$base:") + amounts.map { "  • $it" }
                })
            }
            if (dairy.isNotEmpty()) {
                put("Dairy", dairy.flatMap { (base, amounts) ->
                    if (amounts.size == 1) listOf(amounts[0])
                    else listOf("$base: (buy one container)") + amounts.map { "  • $it" }
                })
            }
            if (spices.isNotEmpty()) {
                // Sort by whether it has multiple items (grouped) vs single items
                val sortedSpices = spices.entries.sortedWith(
                    compareByDescending<Map.Entry<String, MutableList<String>>> { it.value.size > 1 }
                        .thenBy { it.key }
                )
                put("Spices & Seasonings", sortedSpices.flatMap { (base, amounts) ->
                    if (amounts.size == 1) listOf(amounts[0])
                    else listOf("$base: (buy one container)") + amounts.map { "  • $it" }
                })
            }
            if (pantry.isNotEmpty()) {
                put("Pantry & Staples", pantry.flatMap { (base, amounts) ->
                    if (amounts.size == 1) listOf(amounts[0])
                    else listOf("$base: (buy one container)") + amounts.map { "  • $it" }
                })
            }
        }
    }

    private fun extractIngredientBase(ingredient: String, keywords: List<String>): String {
        val lower = ingredient.lowercase()
        // Find the longest matching keyword (to handle cases like "cumin seeds" vs "cumin")
        val matchedKeyword = keywords
            .filter { lower.contains(it) }
            .maxByOrNull { it.length }
        
        return when {
            matchedKeyword != null -> {
                // For spices that often have "seeds" or "powder" variants, extract just the base spice name
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
    val sections: List<ShoppingSection> = demoSections
)

data class ShoppingSection(
    val title: String,
    val items: List<ShoppingItem>
)

data class ShoppingItem(
    val name: String,
    val checked: Boolean
)

private val demoSections = listOf(
    ShoppingSection(
        title = "Produce",
        items = listOf(
            ShoppingItem("Spinach", true),
            ShoppingItem("Cherry tomatoes", false)
        )
    ),
    ShoppingSection(
        title = "Pantry",
        items = listOf(
            ShoppingItem("Whole grain wraps", false),
            ShoppingItem("Chickpeas", true)
        )
    )
)
