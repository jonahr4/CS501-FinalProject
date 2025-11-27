package com.example.cs501_mealmapproject.ui.recipes

import android.util.Log
import com.example.cs501_mealmapproject.network.MealApi
import com.example.cs501_mealmapproject.network.MealDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Repository that uses Retrofit/Moshi (`MealApi`) to fetch and map meals from TheMealDB.
// It attempts Retrofit first and falls back to a manual HTTP+JSON parser if needed.
class RecipeDiscoveryRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val api: MealApi = MealApi
) {

    suspend fun searchRecipes(query: String): List<RecipeSummary> = withContext(dispatcher) {
        // Try Retrofit/Moshi path first
        try {
            val response = api.retrofitService.searchMeals(query)
            val meals = response.meals
            if (!meals.isNullOrEmpty()) {
                Log.d("RecipeRepo", "Retrofit returned ${meals.size} meals for query='$query'")
                return@withContext meals.mapNotNull { it.toRecipeSummary() }
            } else {
                Log.w("RecipeRepo", "Retrofit returned empty meals for query='$query'")
            }
        } catch (e: Exception) {
            Log.e("RecipeRepo", "Retrofit search failed for query='$query'", e)
            // allow fallthrough to backup parser
        }

        // Fallback: manual HTTP call + JSONObject parsing
        try {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val url = URL("https://www.themealdb.com/api/json/v1/1/search.php?s=$encodedQuery")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext emptyList()
                }
                val response = connection.inputStream.use { stream ->
                    BufferedReader(InputStreamReader(stream)).use { it.readText() }
                }
                return@withContext parseMeals(response)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
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

    private fun parseMeals(rawJson: String): List<RecipeSummary> {
        val root = JSONObject(rawJson)
        val mealsArray = root.optJSONArray("meals") ?: return emptyList()
        return buildList {
            for (index in 0 until mealsArray.length()) {
                val meal = mealsArray.optJSONObject(index) ?: continue
                add(meal.toRecipeSummary())
            }
        }
    }

    // JSONObject extension used by the fallback parser
    private fun org.json.JSONObject.toRecipeSummary(): RecipeSummary {
        val subtitleParts = listOfNotNull(
            optString("strArea").takeIf { it.isNotBlank() },
            optString("strCategory").takeIf { it.isNotBlank() }
        )
        val tags = when (val rawTags = optString("strTags")) {
            null, "null", "" -> emptyList()
            else -> rawTags.split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        }
        val instructions = optString("strInstructions").orEmpty().trim()
        val shortDescription = instructions.lineSequence().firstOrNull()?.take(140)
            ?: "Tap to view recipe details"
        val imageUrl = optString("strMealThumb").takeIf { it.isNotBlank() && it != "null" }
        val sourceUrl = optString("strSource").takeIf { it.isNotBlank() && it != "null" }
            ?: optString("idMeal").takeIf { it.isNotBlank() && it != "null" }?.let { id ->
                "https://www.themealdb.com/meal/$id"
            }

        return RecipeSummary(
            title = optString("strMeal"),
            subtitle = subtitleParts.joinToString(separator = " • ").ifBlank { "Meal inspiration" },
            description = shortDescription,
            tags = if (tags.isNotEmpty()) tags else listOf("Source: TheMealDB"),
            imageUrl = imageUrl,
            instructions = instructions.ifBlank { "Detailed instructions coming soon." },
            ingredients = buildIngredientListFromJson(this),
            sourceUrl = sourceUrl
        )
    }

    private fun buildIngredientListFromJson(obj: org.json.JSONObject): List<String> {
        val items = mutableListOf<String>()
        for (index in 1..20) {
            val ingredient = obj.optString("strIngredient$index").orEmpty().trim()
            if (ingredient.isEmpty() || ingredient.equals("null", ignoreCase = true)) continue
            val measure = obj.optString("strMeasure$index").orEmpty().trim()
            val entry = if (measure.isNotEmpty() && !measure.equals("null", ignoreCase = true)) {
                "$measure $ingredient".trim()
            } else {
                ingredient
            }
            items += entry
        }
        return items
    }
}
