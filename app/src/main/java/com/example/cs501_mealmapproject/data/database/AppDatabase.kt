package com.example.cs501_mealmapproject.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FoodLogEntity::class, MealPlanEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodLogDao(): FoodLogDao
    abstract fun mealPlanDao(): MealPlanDao

    /**
     * Clear all local data - called on logout to prevent data leakage between accounts
     */
    suspend fun clearAllData() {
        foodLogDao().deleteAllFoodLogs()
        mealPlanDao().deleteAllMealPlans()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mealmap_database"
                )
                    .fallbackToDestructiveMigration() // Simple migration for course project
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
