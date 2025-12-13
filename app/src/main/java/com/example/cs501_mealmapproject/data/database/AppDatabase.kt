package com.example.cs501_mealmapproject.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cs501_mealmapproject.data.database.MealPlanEntity

@Database(
    entities = [FoodLogEntity::class, PlannedMealEntity::class, RecipeCacheEntity::class, MealPlanEntity::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodLogDao(): FoodLogDao
    abstract fun plannedMealDao(): PlannedMealDao
    abstract fun recipeCacheDao(): RecipeCacheDao
    abstract fun mealPlanDao(): MealPlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Migration from version 1 to 2: Add new nutrition columns
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
        
        // Migration from version 2 to 3: Add planned_meals table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS planned_meals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        mealType TEXT NOT NULL,
                        recipeName TEXT NOT NULL,
                        recipeImageUrl TEXT,
                        ingredients TEXT NOT NULL,
                        instructions TEXT NOT NULL DEFAULT '',
                        sourceUrl TEXT,
                        estimatedCalories INTEGER NOT NULL DEFAULT 0,
                        estimatedProtein REAL NOT NULL DEFAULT 0,
                        estimatedCarbs REAL NOT NULL DEFAULT 0,
                        estimatedFat REAL NOT NULL DEFAULT 0,
                        estimatedFiber REAL NOT NULL DEFAULT 0,
                        estimatedSugar REAL NOT NULL DEFAULT 0,
                        estimatedSodium REAL NOT NULL DEFAULT 0,
                        servings INTEGER NOT NULL DEFAULT 1,
                        userId TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
        
        // Migration from version 3 to 4: Add enhanced food log fields and recipe cache
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to food_logs
                database.execSQL("ALTER TABLE food_logs ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN fromRecipe TEXT")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN loggedTime TEXT")
                database.execSQL("ALTER TABLE food_logs ADD COLUMN imageUrl TEXT")
                
                // Create recipe_cache table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_cache (
                        recipeName TEXT PRIMARY KEY NOT NULL,
                        imageUrl TEXT,
                        sourceUrl TEXT,
                        category TEXT,
                        area TEXT,
                        ingredientList TEXT NOT NULL DEFAULT '[]',
                        estimatedServings INTEGER NOT NULL DEFAULT 4,
                        totalCalories INTEGER NOT NULL DEFAULT 0,
                        totalProtein REAL NOT NULL DEFAULT 0,
                        totalCarbs REAL NOT NULL DEFAULT 0,
                        totalFat REAL NOT NULL DEFAULT 0,
                        totalFiber REAL NOT NULL DEFAULT 0,
                        totalSugar REAL NOT NULL DEFAULT 0,
                        totalSodium REAL NOT NULL DEFAULT 0,
                        isNutritionCalculated INTEGER NOT NULL DEFAULT 0,
                        cachedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop and recreate recipe_cache table with correct column name
                database.execSQL("DROP TABLE IF EXISTS recipe_cache")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recipe_cache (
                        recipeName TEXT PRIMARY KEY NOT NULL,
                        imageUrl TEXT,
                        sourceUrl TEXT,
                        category TEXT,
                        area TEXT,
                        ingredientList TEXT NOT NULL DEFAULT '[]',
                        estimatedServings INTEGER NOT NULL DEFAULT 4,
                        totalCalories INTEGER NOT NULL DEFAULT 0,
                        totalProtein REAL NOT NULL DEFAULT 0,
                        totalCarbs REAL NOT NULL DEFAULT 0,
                        totalFat REAL NOT NULL DEFAULT 0,
                        totalFiber REAL NOT NULL DEFAULT 0,
                        totalSugar REAL NOT NULL DEFAULT 0,
                        totalSodium REAL NOT NULL DEFAULT 0,
                        isNutritionCalculated INTEGER NOT NULL DEFAULT 0,
                        cachedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        mealType TEXT NOT NULL,
                        recipeName TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Recreate food_logs to ensure all expected columns exist
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS food_logs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mealName TEXT NOT NULL,
                        calories INTEGER NOT NULL,
                        protein REAL NOT NULL,
                        carbs REAL NOT NULL,
                        fat REAL NOT NULL,
                        fiber REAL NOT NULL,
                        sugar REAL NOT NULL,
                        sodium REAL NOT NULL,
                        servingSize TEXT NOT NULL,
                        servings REAL NOT NULL,
                        mealType TEXT NOT NULL,
                        source TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        isFavorite INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        fromRecipe TEXT,
                        loggedTime TEXT,
                        imageUrl TEXT
                    )
                """)

                // Copy existing data, filling new columns with defaults where needed
                database.execSQL("""
                    INSERT INTO food_logs_new (
                        id, mealName, calories, protein, carbs, fat, fiber, sugar, sodium,
                        servingSize, servings, mealType, source, timestamp, isFavorite, notes,
                        fromRecipe, loggedTime, imageUrl
                    )
                    SELECT 
                        id,
                        mealName,
                        calories,
                        protein,
                        carbs,
                        fat,
                        0 AS fiber,
                        0 AS sugar,
                        0 AS sodium,
                        '1 serving' AS servingSize,
                        1 AS servings,
                        'SNACK' AS mealType,
                        source,
                        timestamp,
                        isFavorite,
                        notes,
                        fromRecipe,
                        loggedTime,
                        imageUrl
                    FROM food_logs
                """)

                database.execSQL("DROP TABLE food_logs")
                database.execSQL("ALTER TABLE food_logs_new RENAME TO food_logs")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mealmap_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
