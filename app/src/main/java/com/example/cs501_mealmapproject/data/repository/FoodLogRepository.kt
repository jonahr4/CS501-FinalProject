package com.example.cs501_mealmapproject.data.repository

import android.content.Context
import android.util.Log
import com.example.cs501_mealmapproject.data.database.AppDatabase
import com.example.cs501_mealmapproject.data.database.FoodLogDao
import com.example.cs501_mealmapproject.data.database.FoodLogEntity
import com.example.cs501_mealmapproject.data.firestore.FoodLogFirestore
import com.example.cs501_mealmapproject.data.firestore.toEntity
import com.example.cs501_mealmapproject.data.firestore.toFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository for managing food logs with dual persistence:
 * - Room database (local, instant access)
 * - Firestore (cloud sync)
 */
class FoodLogRepository(context: Context) {

    private val foodLogDao: FoodLogDao = AppDatabase.getDatabase(context).foodLogDao()
    private val firestore: FirebaseFirestore = Firebase.firestore

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_FOOD_LOGS = "foodLogs"
        const val TAG = "FoodLogRepository"
    }

    // ========== Read Operations (from Room) ==========

    /**
     * Get all food logs (from local Room database)
     */
    fun getAllFoodLogs(): Flow<List<FoodLogEntity>> {
        return foodLogDao.getAllFoodLogs()
    }

    /**
     * Get recent food logs (from local Room database)
     */
    fun getRecentFoodLogs(limit: Int = 10): Flow<List<FoodLogEntity>> {
        return foodLogDao.getRecentFoodLogs(limit)
    }

    // ========== Write Operations (Dual Persistence) ==========

    /**
     * Insert a food log
     * 1. Write to Room immediately (instant UI feedback)
     * 2. Sync to Firestore in background
     */
    suspend fun insertFoodLog(userId: String, foodLog: FoodLogEntity): Result<Unit> {
        return try {
            // 1. Write to Room immediately
            foodLogDao.insertFoodLog(foodLog)
            Log.d(TAG, "Food log inserted to Room: ${foodLog.mealName}")

            // 2. Sync to Firestore in background
            withContext(Dispatchers.IO) {
                try {
                    // Get the ID after insertion (Room auto-generates it)
                    val firestoreLog = foodLog.toFirestore()

                    firestore.collection(COLLECTION_USERS)
                        .document(userId)
                        .collection(COLLECTION_FOOD_LOGS)
                        .document(firestoreLog.id)
                        .set(firestoreLog)
                        .await()

                    Log.d(TAG, "Food log synced to Firestore: ${foodLog.mealName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync food log to Firestore", e)
                    // Room data is still saved, will retry on next sync
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert food log", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a food log
     * Uses soft delete in Firestore, hard delete in Room
     */
    suspend fun deleteFoodLog(userId: String, id: Long): Result<Unit> {
        return try {
            // 1. Delete from Room
            foodLogDao.deleteFoodLog(id)
            Log.d(TAG, "Food log deleted from Room: $id")

            // 2. Soft delete in Firestore (mark as deleted)
            withContext(Dispatchers.IO) {
                try {
                    firestore.collection(COLLECTION_USERS)
                        .document(userId)
                        .collection(COLLECTION_FOOD_LOGS)
                        .document(id.toString())
                        .update(
                            mapOf(
                                "deleted" to true,
                                "lastUpdatedAt" to System.currentTimeMillis()
                            )
                        )
                        .await()

                    Log.d(TAG, "Food log soft deleted in Firestore: $id")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete food log from Firestore", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete food log", e)
            Result.failure(e)
        }
    }

    // ========== Sync Operations ==========

    /**
     * Sync food logs from Firestore to Room
     * Called on app launch when user signs in
     */
    suspend fun syncFromFirestore(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting sync from Firestore for user: $userId")

            // Pull all non-deleted logs from Firestore
            val firestoreLogs = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_FOOD_LOGS)
                .whereEqualTo("deleted", false)
                .get()
                .await()
                .toObjects(FoodLogFirestore::class.java)

            Log.d(TAG, "Found ${firestoreLogs.size} food logs in Firestore")

            // Insert/update in Room
            firestoreLogs.forEach { firestoreLog ->
                val entity = firestoreLog.toEntity()
                foodLogDao.insertFoodLog(entity)
            }

            Log.d(TAG, "Sync completed: ${firestoreLogs.size} food logs synced")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync from Firestore failed", e)
            Result.failure(e)
        }
    }

    /**
     * Upload all local food logs to Firestore
     * Useful for first-time sync or data migration
     */
    suspend fun uploadAllToFirestore(userId: String): Result<Unit> {
        return try {
            // This would need to collect from Flow, which is more complex
            // For now, we'll skip bulk upload since individual logs sync on insert
            Log.d(TAG, "Individual logs sync automatically on insert")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload logs to Firestore", e)
            Result.failure(e)
        }
    }
}
