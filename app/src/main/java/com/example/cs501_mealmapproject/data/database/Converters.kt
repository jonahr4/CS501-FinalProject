package com.example.cs501_mealmapproject.data.database

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Room type converters for complex data types
 */
class Converters {
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    private val ingredientListType = Types.newParameterizedType(
        List::class.java,
        IngredientWithMeasure::class.java
    )
    
    private val ingredientListAdapter = moshi.adapter<List<IngredientWithMeasure>>(ingredientListType)
    
    @TypeConverter
    fun fromIngredientList(ingredients: List<IngredientWithMeasure>): String {
        return ingredientListAdapter.toJson(ingredients)
    }
    
    @TypeConverter
    fun toIngredientList(json: String): List<IngredientWithMeasure> {
        return try {
            ingredientListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
