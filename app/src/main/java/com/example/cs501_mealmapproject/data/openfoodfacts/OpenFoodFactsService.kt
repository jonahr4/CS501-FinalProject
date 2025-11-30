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
    @Json(name = "nutriments") val nutriments: Nutriments?
)

@JsonClass(generateAdapter = true)
data class Nutriments(
    @Json(name = "energy-kcal_100g") val energyKcal: Double?,
    @Json(name = "proteins_100g") val proteins: Double?,
    @Json(name = "carbohydrates_100g") val carbohydrates: Double?,
    @Json(name = "fat_100g") val fat: Double?,
    @Json(name = "fiber_100g") val fiber: Double?,
    @Json(name = "sugars_100g") val sugars: Double?,
    @Json(name = "sodium_100g") val sodium: Double?
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
