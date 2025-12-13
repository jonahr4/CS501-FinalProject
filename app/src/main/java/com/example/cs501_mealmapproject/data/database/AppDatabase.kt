package com.example.cs501_mealmapproject.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodLogEntity::class, RecipeCacheEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(RecipeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodLogDao(): FoodLogDao
    abstract fun recipeCacheDao(): RecipeCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2: Add new nutrition columns to food_logs
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_logs ADD COLUMN fiber REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN sugar REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN sodium REAL NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN servingSize TEXT NOT NULL DEFAULT '1 serving'")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN servings REAL NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN mealType TEXT NOT NULL DEFAULT 'SNACK'")
            }
        }
        
        // Migration from version 2 to 3: Add recipe_cache table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_cache (
                        recipeName TEXT NOT NULL PRIMARY KEY,
                        imageUrl TEXT,
                        sourceUrl TEXT,
                        category TEXT,
                        area TEXT,
                        ingredients TEXT NOT NULL DEFAULT '[]',
                        totalCalories INTEGER NOT NULL DEFAULT 0,
                        totalProtein REAL NOT NULL DEFAULT 0,
                        totalCarbs REAL NOT NULL DEFAULT 0,
                        totalFat REAL NOT NULL DEFAULT 0,
                        totalFiber REAL NOT NULL DEFAULT 0,
                        totalSugar REAL NOT NULL DEFAULT 0,
                        totalSodium REAL NOT NULL DEFAULT 0,
                        estimatedServings INTEGER NOT NULL DEFAULT 4,
                        cachedAt INTEGER NOT NULL DEFAULT 0,
                        nutritionCalculatedAt INTEGER,
                        isNutritionCalculated INTEGER NOT NULL DEFAULT 0,
                        isFavorite INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }
        
        // Migration from version 3 to 4: Add enhanced fields to food_logs
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE food_logs ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN fromRecipe TEXT")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN loggedTime TEXT")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN imageUrl TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mealmap_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
