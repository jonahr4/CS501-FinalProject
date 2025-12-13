package com.example.cs501_mealmapproject.data.nutrition

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Service that estimates total nutrition for a recipe by looking up each ingredient
 * in the USDA food database and summing up the nutritional values.
 */
class RecipeNutritionEstimator {
    
    data class RecipeNutrition(
        val calories: Int = 0,
        val protein: Float = 0f,
        val carbs: Float = 0f,
        val fat: Float = 0f,
        val fiber: Float = 0f,
        val sugar: Float = 0f,
        val sodium: Float = 0f,
        val ingredientBreakdown: List<IngredientNutrition> = emptyList()
    )
    
    data class IngredientNutrition(
        val ingredientName: String,
        val originalText: String,
        val estimatedGrams: Float,
        val calories: Int,
        val protein: Float,
        val carbs: Float,
        val fat: Float
    )
    
    /**
     * Estimates the total nutrition for a recipe given its list of ingredients.
     * Each ingredient string is typically in format "1 cup chicken breast" or "100g rice".
     */
    suspend fun estimateRecipeNutrition(ingredients: List<String>): RecipeNutrition = withContext(Dispatchers.IO) {
        val ingredientNutritions = mutableListOf<IngredientNutrition>()
        
        // Process ingredients (limit concurrency to avoid rate limiting)
        val results = ingredients.map { ingredient ->
            async {
                try {
                    estimateIngredientNutrition(ingredient)
                } catch (e: Exception) {
                    Log.w("RecipeNutritionEstimator", "Failed to estimate: $ingredient", e)
                    null
                }
            }
        }.awaitAll().filterNotNull()
        
        ingredientNutritions.addAll(results)
        
        // Sum up all nutrition values
        return@withContext RecipeNutrition(
            calories = ingredientNutritions.sumOf { it.calories },
            protein = ingredientNutritions.sumOf { it.protein.toDouble() }.toFloat(),
            carbs = ingredientNutritions.sumOf { it.carbs.toDouble() }.toFloat(),
            fat = ingredientNutritions.sumOf { it.fat.toDouble() }.toFloat(),
            fiber = 0f, // Could be added if we expand ingredient lookup
            sugar = 0f,
            sodium = 0f,
            ingredientBreakdown = ingredientNutritions
        )
    }
    
    /**
     * Estimates nutrition for a single ingredient string.
     * Parses the quantity/unit and looks up the food in the database.
     */
    private suspend fun estimateIngredientNutrition(ingredientText: String): IngredientNutrition? {
        // Parse the ingredient text to extract quantity, unit, and food name
        val parsed = parseIngredient(ingredientText)
        
        // Search for the food in the database using NutritionApi
        val searchResponse = NutritionApi.api.searchFoods(parsed.foodName, pageSize = 3)
        val searchResults = searchResponse.foods?.map { FoodItem.fromSearchResult(it) } ?: emptyList()
        if (searchResults.isEmpty()) {
            Log.d("RecipeNutritionEstimator", "No results for: ${parsed.foodName}")
            return null
        }
        
        // Use the best match
        val bestMatch = searchResults.first()
        
        // Convert to estimated grams based on quantity and unit
        val estimatedGrams = convertToGrams(parsed.quantity, parsed.unit, bestMatch.name)
        
        // Calculate nutrition based on grams (API gives per 100g)
        val factor = estimatedGrams / 100f
        
        return IngredientNutrition(
            ingredientName = bestMatch.name,
            originalText = ingredientText,
            estimatedGrams = estimatedGrams,
            calories = (bestMatch.calories * factor).toInt(),
            protein = bestMatch.protein * factor,
            carbs = bestMatch.carbs * factor,
            fat = bestMatch.fat * factor
        )
    }
    
    private data class ParsedIngredient(
        val quantity: Float,
        val unit: String,
        val foodName: String
    )
    
    /**
     * Parses ingredient text like "2 cups chicken breast" into components.
     */
    private fun parseIngredient(text: String): ParsedIngredient {
        val cleaned = text.lowercase().trim()
        
        // Common patterns:
        // "1 cup flour" -> quantity=1, unit=cup, food=flour
        // "100g chicken" -> quantity=100, unit=g, food=chicken
        // "2 large eggs" -> quantity=2, unit=large, food=eggs
        // "salt to taste" -> quantity=0, unit=, food=salt
        
        // Try to extract number at the beginning
        val numberPattern = Regex("""^([\d./]+)\s*(.*)$""")
        val fractionPattern = Regex("""(\d+)/(\d+)""")
        
        var quantity = 1f
        var remaining = cleaned
        
        val numberMatch = numberPattern.find(cleaned)
        if (numberMatch != null) {
            val numStr = numberMatch.groupValues[1]
            quantity = parseQuantity(numStr)
            remaining = numberMatch.groupValues[2].trim()
        }
        
        // Extract unit
        val units = listOf(
            "cup", "cups", "tablespoon", "tablespoons", "tbsp", "teaspoon", "teaspoons", "tsp",
            "ounce", "ounces", "oz", "pound", "pounds", "lb", "lbs",
            "gram", "grams", "g", "kg", "kilogram",
            "ml", "milliliter", "l", "liter",
            "piece", "pieces", "slice", "slices",
            "large", "medium", "small",
            "whole", "clove", "cloves", "bunch", "bunches",
            "can", "cans", "package", "packages"
        )
        
        var unit = ""
        for (u in units) {
            if (remaining.startsWith("$u ") || remaining.startsWith("${u}s ")) {
                unit = u.removeSuffix("s")
                remaining = remaining.removePrefix(u).removePrefix("s").trim()
                break
            }
        }
        
        // Clean up the food name
        val foodName = remaining
            .replace(Regex("""\(.*?\)"""), "") // Remove parenthetical notes
            .replace(Regex("""^(of|the)\s+"""), "") // Remove leading articles
            .replace(",.*".toRegex(), "") // Remove everything after comma
            .trim()
        
        return ParsedIngredient(quantity, unit, foodName.ifEmpty { text })
    }
    
    private fun parseQuantity(str: String): Float {
        return when {
            str.contains("/") -> {
                val parts = str.split("/")
                if (parts.size == 2) {
                    val num = parts[0].toFloatOrNull() ?: 1f
                    val den = parts[1].toFloatOrNull() ?: 1f
                    num / den
                } else {
                    str.toFloatOrNull() ?: 1f
                }
            }
            else -> str.toFloatOrNull() ?: 1f
        }
    }
    
    /**
     * Converts a quantity and unit to estimated grams.
     * This is approximate - different foods have different densities.
     */
    private fun convertToGrams(quantity: Float, unit: String, foodName: String): Float {
        // Approximate conversion factors (varies by food type)
        val baseGrams = when (unit) {
            "cup" -> 240f // varies a lot by food
            "tablespoon", "tbsp" -> 15f
            "teaspoon", "tsp" -> 5f
            "ounce", "oz" -> 28.35f
            "pound", "lb" -> 453.6f
            "gram", "g" -> 1f
            "kg", "kilogram" -> 1000f
            "ml", "milliliter" -> 1f // approximately for water-based
            "l", "liter" -> 1000f
            "large" -> estimatePieceWeight(foodName, "large")
            "medium" -> estimatePieceWeight(foodName, "medium")
            "small" -> estimatePieceWeight(foodName, "small")
            "piece", "whole" -> estimatePieceWeight(foodName, "medium")
            "slice" -> 30f // typical bread slice
            "clove" -> 5f // garlic clove
            "bunch" -> 100f // varies
            "can" -> 400f // typical can
            "package" -> 200f // varies widely
            else -> estimatePieceWeight(foodName, "medium")
        }
        
        return quantity * baseGrams
    }
    
    /**
     * Estimates the weight of a single piece/unit of common foods.
     */
    private fun estimatePieceWeight(foodName: String, size: String): Float {
        val lower = foodName.lowercase()
        val sizeFactor = when (size) {
            "small" -> 0.75f
            "large" -> 1.25f
            else -> 1f
        }
        
        val baseWeight = when {
            lower.contains("egg") -> 50f
            lower.contains("banana") -> 120f
            lower.contains("apple") -> 180f
            lower.contains("orange") -> 130f
            lower.contains("chicken breast") -> 170f
            lower.contains("chicken thigh") -> 110f
            lower.contains("onion") -> 150f
            lower.contains("potato") -> 170f
            lower.contains("tomato") -> 150f
            lower.contains("carrot") -> 60f
            lower.contains("garlic") -> 5f
            lower.contains("lemon") || lower.contains("lime") -> 60f
            lower.contains("avocado") -> 200f
            lower.contains("pepper") || lower.contains("bell") -> 150f
            lower.contains("cucumber") -> 300f
            lower.contains("celery") -> 40f // per stalk
            else -> 100f // default assumption
        }
        
        return baseWeight * sizeFactor
    }
}
