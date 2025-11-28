package com.example.cs501_mealmapproject.ui.recipes

import android.util.Log
import com.example.cs501_mealmapproject.network.MealApi
import com.example.cs501_mealmapproject.network.MealDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Repository that uses Retrofit/Moshi to fetch and map meals from TheMealDB.
class RecipeDiscoveryRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val api: MealApi = MealApi
) {

    suspend fun searchRecipes(query: String): List<RecipeSummary> = withContext(dispatcher) {
        try {
            Log.d("RecipeRepo", "Starting search for query='$query'")
            val response = api.retrofitService.searchMeals(query)
            Log.d("RecipeRepo", "Got response: meals=${response.meals?.size ?: 0}")
            
            val meals = response.meals
            return@withContext if (!meals.isNullOrEmpty()) {
                val mapped = meals.mapNotNull { dto ->
                    Log.d("RecipeRepo", "Mapping meal: ${dto.strMeal}")
                    dto.toRecipeSummary()
                }
                Log.d("RecipeRepo", "Mapped ${mapped.size} recipes")
                mapped
            } else {
                Log.w("RecipeRepo", "Response meals list is null or empty")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("RecipeRepo", "Search failed for query='$query'", e)
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun MealDto.toRecipeSummary(): RecipeSummary? {
        val title = strMeal?.takeIf { it.isNotBlank() } ?: return null

        val subtitleParts = listOfNotNull(
            strArea?.takeIf { it.isNotBlank() },
            strCategory?.takeIf { it.isNotBlank() }
        )

        val tags = when (val raw = strTags) {
            null, "", "null" -> emptyList()
            else -> raw.split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        }

        val instructions = strInstructions?.trim().orEmpty()
        val shortDescription = instructions.lineSequence().firstOrNull()?.take(140)
            ?: "Tap to view recipe details"

        val imageUrl = strMealThumb?.takeIf { it.isNotBlank() && it != "null" }

        val sourceUrl = strSource?.takeIf { it.isNotBlank() && it != "null" }
            ?: idMeal?.takeIf { it.isNotBlank() && it != "null" }?.let { id ->
                "https://www.themealdb.com/meal/$id"
            }

        return RecipeSummary(
            title = title,
            subtitle = subtitleParts.joinToString(separator = " • ").ifBlank { "Meal inspiration" },
            description = shortDescription,
            tags = if (tags.isNotEmpty()) tags else listOf("Source: TheMealDB"),
            imageUrl = imageUrl,
            instructions = instructions.ifBlank { "Detailed instructions coming soon." },
            ingredients = buildIngredientList(),
            sourceUrl = sourceUrl
        )
    }

    private fun MealDto.buildIngredientList(): List<String> {
        val items = mutableListOf<String>()
        val ingredients = listOf(
            strIngredient1, strIngredient2, strIngredient3, strIngredient4, strIngredient5,
            strIngredient6, strIngredient7, strIngredient8, strIngredient9, strIngredient10,
            strIngredient11, strIngredient12, strIngredient13, strIngredient14, strIngredient15,
            strIngredient16, strIngredient17, strIngredient18, strIngredient19, strIngredient20
        )
        val measures = listOf(
            strMeasure1, strMeasure2, strMeasure3, strMeasure4, strMeasure5,
            strMeasure6, strMeasure7, strMeasure8, strMeasure9, strMeasure10,
            strMeasure11, strMeasure12, strMeasure13, strMeasure14, strMeasure15,
            strMeasure16, strMeasure17, strMeasure18, strMeasure19, strMeasure20
        )

        for (i in ingredients.indices) {
            val ing = ingredients[i]?.trim().orEmpty()
            if (ing.isEmpty() || ing.equals("null", ignoreCase = true)) continue
            val measure = measures.getOrNull(i)?.trim().orEmpty()
            val entry = if (measure.isNotEmpty() && !measure.equals("null", ignoreCase = true)) {
                "$measure $ing".trim()
            } else {
                ing
            }
            items += entry
        }
        return items
    }


}
