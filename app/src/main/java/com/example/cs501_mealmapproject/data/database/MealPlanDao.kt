package com.example.cs501_mealmapproject.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlans(mealPlans: List<MealPlanEntity>)

    @Query("SELECT * FROM meal_plans ORDER BY date, mealType")
    fun getAllMealPlans(): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY mealType")
    fun getMealPlansByDate(date: String): Flow<List<MealPlanEntity>>

    @Query("DELETE FROM meal_plans WHERE id = :id")
    suspend fun deleteMealPlan(id: Long)

    @Query("DELETE FROM meal_plans WHERE date = :date AND mealType = :mealType")
    suspend fun deleteMealPlanByDateAndType(date: String, mealType: String)

    @Query("DELETE FROM meal_plans")
    suspend fun deleteAllMealPlans()

    @Query("SELECT * FROM meal_plans WHERE date >= :startDate AND date <= :endDate ORDER BY date, mealType")
    fun getMealPlansForWeek(startDate: String, endDate: String): Flow<List<MealPlanEntity>>
}
