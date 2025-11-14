package com.example.cs501_mealmapproject.ui.recipes

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

//Implementation of using themealDB API
class RecipeDiscoveryRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun searchRecipes(query: String): List<RecipeSummary> = withContext(dispatcher) {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val url = URL("https://www.themealdb.com/api/json/v1/1/search.php?s=$encodedQuery")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("MealDB request failed: ${connection.responseCode}")
            }
            val response = connection.inputStream.use { stream ->
                BufferedReader(InputStreamReader(stream)).use { it.readText() }
            }
            parseMeals(response)
        } finally {
            connection.disconnect()
        }
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

    private fun JSONObject.toRecipeSummary(): RecipeSummary {
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
            ingredients = buildIngredientList(),
            sourceUrl = sourceUrl
        )
    }

    private fun JSONObject.buildIngredientList(): List<String> {
        val items = mutableListOf<String>()
        for (index in 1..20) {
            val ingredient = optString("strIngredient$index").orEmpty().trim()
            if (ingredient.isEmpty() || ingredient.equals("null", ignoreCase = true)) continue
            val measure = optString("strMeasure$index").orEmpty().trim()
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
