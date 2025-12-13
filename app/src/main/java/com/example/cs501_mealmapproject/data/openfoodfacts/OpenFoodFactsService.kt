package com.example.cs501_mealmapproject.data.openfoodfacts

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class ProductResponse(
    val status: Int,
    val product: Product?
)

@JsonClass(generateAdapter = true)
data class Product(
    @Json(name = "product_name") val productName: String?,
    @Json(name = "brands") val brands: String?,
    @Json(name = "nutriments") val nutriments: Nutriments?,
    @Json(name = "serving_size") val servingSize: String?,
    @Json(name = "serving_quantity") val servingQuantity: Double?,
    @Json(name = "product_quantity") val productQuantity: String?
)

@JsonClass(generateAdapter = true)
data class Nutriments(
    // Per 100g values (fallback)
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double?,
    @Json(name = "proteins_100g") val proteins100g: Double?,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double?,
    @Json(name = "fat_100g") val fat100g: Double?,
    @Json(name = "fiber_100g") val fiber100g: Double?,
    @Json(name = "sugars_100g") val sugars100g: Double?,
    @Json(name = "sodium_100g") val sodium100g: Double?,

    // Per serving values (preferred)
    @Json(name = "energy-kcal_serving") val energyKcalServing: Double?,
    @Json(name = "proteins_serving") val proteinsServing: Double?,
    @Json(name = "carbohydrates_serving") val carbohydratesServing: Double?,
    @Json(name = "fat_serving") val fatServing: Double?,
    @Json(name = "fiber_serving") val fiberServing: Double?,
    @Json(name = "sugars_serving") val sugarsServing: Double?,
    @Json(name = "sodium_serving") val sodiumServing: Double?
)

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): ProductResponse
}

object OpenFoodFactsService {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: OpenFoodFactsApi = retrofit.create(OpenFoodFactsApi::class.java)
}
