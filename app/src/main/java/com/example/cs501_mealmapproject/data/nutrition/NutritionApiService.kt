package com.example.cs501_mealmapproject.data.nutrition

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * USDA FoodData Central API Service
 * Free API with comprehensive nutrition database
 * API Key: DEMO_KEY (for development - get your own at https://fdc.nal.usda.gov/api-key-signup.html)
 */

// Search response
@JsonClass(generateAdapter = true)
data class FoodSearchResponse(
    val foods: List<SearchResultFood>?,
    val totalHits: Int?,
    val currentPage: Int?,
    val totalPages: Int?
)

@JsonClass(generateAdapter = true)
data class SearchResultFood(
    val fdcId: Int,
    val description: String?,
    val brandOwner: String?,
    val brandName: String?,
    val dataType: String?,
    val foodNutrients: List<SearchFoodNutrient>?
)

@JsonClass(generateAdapter = true)
data class SearchFoodNutrient(
    val nutrientId: Int?,
    val nutrientName: String?,
    val nutrientNumber: String?,
    val unitName: String?,
    val value: Double?
)

// Detail response
@JsonClass(generateAdapter = true)
data class FoodDetailResponse(
    val fdcId: Int,
    val description: String?,
    val brandOwner: String?,
    val brandName: String?,
    val servingSize: Double?,
    val servingSizeUnit: String?,
    val householdServingFullText: String?,
    val foodNutrients: List<FoodNutrient>?,
    val foodPortions: List<FoodPortion>?
)

@JsonClass(generateAdapter = true)
data class FoodNutrient(
    val nutrient: Nutrient?,
    val amount: Double?
)

@JsonClass(generateAdapter = true)
data class Nutrient(
    val id: Int?,
    val number: String?,
    val name: String?,
    val unitName: String?
)

@JsonClass(generateAdapter = true)
data class FoodPortion(
    val id: Int?,
    val gramWeight: Double?,
    val amount: Double?,
    val modifier: String?,
    val portionDescription: String?,
    val measureUnit: MeasureUnit?
)

@JsonClass(generateAdapter = true)
data class MeasureUnit(
    val id: Int?,
    val name: String?,
    val abbreviation: String?
)

interface NutritionApiService {
    
    @GET("foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("dataType") dataType: String = "Foundation,SR Legacy,Branded",
        @Query("pageSize") pageSize: Int = 25,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("api_key") apiKey: String = API_KEY
    ): FoodSearchResponse
    
    @GET("food/{fdcId}")
    suspend fun getFoodDetails(
        @Path("fdcId") fdcId: Int,
        @Query("api_key") apiKey: String = API_KEY
    ): FoodDetailResponse
    
    companion object {
        // Demo key for development - replace with your own key for production
        const val API_KEY = "9bmts7HkHc7L3gfo9dB19B8MbLRzMK7ZEwlK821a"
    }
}

object NutritionApi {
    private const val BASE_URL = "https://api.nal.usda.gov/fdc/v1/"
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    
    val api: NutritionApiService = retrofit.create(NutritionApiService::class.java)
}

/**
 * Helper data class for displaying food items in the UI
 */
data class FoodItem(
    val fdcId: Int,
    val name: String,
    val brand: String?,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val servingSize: String?,
    val servingSizeGrams: Double?
) {
    companion object {
        /**
         * Create FoodItem from search result
         */
        fun fromSearchResult(food: SearchResultFood): FoodItem {
            val nutrients = food.foodNutrients ?: emptyList()
            
            // USDA nutrient IDs and names (API returns different formats):
            // 1008/208 = Energy (kcal)
            // 1003/203 = Protein
            // 1005/205 = Carbohydrates
            // 1004/204 = Total Fat
            // 1079/291 = Fiber
            // 2000/269 = Sugars
            // 1093/307 = Sodium
            
            // Helper function to find nutrient by multiple possible identifiers
            fun findNutrient(vararg checks: (SearchFoodNutrient) -> Boolean): Double {
                for (check in checks) {
                    val found = nutrients.find(check)?.value
                    if (found != null && found > 0) return found
                }
                return 0.0
            }
            
            val calories = findNutrient(
                { it.nutrientId == 1008 },
                { it.nutrientNumber == "208" },
                { it.nutrientName?.contains("Energy", ignoreCase = true) == true },
                { it.nutrientName?.contains("Calories", ignoreCase = true) == true }
            ).toInt()
            
            val protein = findNutrient(
                { it.nutrientId == 1003 },
                { it.nutrientNumber == "203" },
                { it.nutrientName?.contains("Protein", ignoreCase = true) == true }
            ).toFloat()
            
            val carbs = findNutrient(
                { it.nutrientId == 1005 },
                { it.nutrientNumber == "205" },
                { it.nutrientName?.contains("Carbohydrate", ignoreCase = true) == true }
            ).toFloat()
            
            val fat = findNutrient(
                { it.nutrientId == 1004 },
                { it.nutrientNumber == "204" },
                { it.nutrientName?.contains("Total lipid", ignoreCase = true) == true },
                { it.nutrientName?.equals("Fat", ignoreCase = true) == true }
            ).toFloat()
            
            val fiber = findNutrient(
                { it.nutrientId == 1079 },
                { it.nutrientNumber == "291" },
                { it.nutrientName?.contains("Fiber", ignoreCase = true) == true }
            ).toFloat()
            
            val sugar = findNutrient(
                { it.nutrientId == 2000 },
                { it.nutrientNumber == "269" },
                { it.nutrientName?.contains("Sugar", ignoreCase = true) == true }
            ).toFloat()
            
            val sodium = findNutrient(
                { it.nutrientId == 1093 },
                { it.nutrientNumber == "307" },
                { it.nutrientName?.contains("Sodium", ignoreCase = true) == true }
            ).toFloat()
            
            val brand = food.brandOwner ?: food.brandName
            
            return FoodItem(
                fdcId = food.fdcId,
                name = food.description ?: "Unknown Food",
                brand = brand,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                sugar = sugar,
                sodium = sodium,
                servingSize = "100g", // Search results are per 100g
                servingSizeGrams = 100.0
            )
        }
        
        /**
         * Create FoodItem from detailed response with serving info
         */
        fun fromDetailResponse(food: FoodDetailResponse): FoodItem {
            val nutrients = food.foodNutrients ?: emptyList()
            
            val calories = nutrients.find { it.nutrient?.number == "208" || it.nutrient?.id == 1008 }?.amount?.toInt() ?: 0
            val protein = nutrients.find { it.nutrient?.number == "203" || it.nutrient?.id == 1003 }?.amount?.toFloat() ?: 0f
            val carbs = nutrients.find { it.nutrient?.number == "205" || it.nutrient?.id == 1005 }?.amount?.toFloat() ?: 0f
            val fat = nutrients.find { it.nutrient?.number == "204" || it.nutrient?.id == 1004 }?.amount?.toFloat() ?: 0f
            val fiber = nutrients.find { it.nutrient?.number == "291" || it.nutrient?.id == 1079 }?.amount?.toFloat() ?: 0f
            val sugar = nutrients.find { it.nutrient?.number == "269" || it.nutrient?.id == 2000 }?.amount?.toFloat() ?: 0f
            val sodium = nutrients.find { it.nutrient?.number == "307" || it.nutrient?.id == 1093 }?.amount?.toFloat() ?: 0f
            
            val brand = food.brandOwner ?: food.brandName
            val servingText = food.householdServingFullText 
                ?: food.servingSize?.let { "${it.toInt()}${food.servingSizeUnit ?: "g"}" }
                ?: "100g"
            
            return FoodItem(
                fdcId = food.fdcId,
                name = food.description ?: "Unknown Food",
                brand = brand,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                sugar = sugar,
                sodium = sodium,
                servingSize = servingText,
                servingSizeGrams = food.servingSize ?: 100.0
            )
        }
    }
}
