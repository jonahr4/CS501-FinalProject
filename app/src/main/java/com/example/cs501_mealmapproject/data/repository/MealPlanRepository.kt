package com.example.cs501_mealmapproject.data.repository

import android.content.Context
import android.util.Log
import com.example.cs501_mealmapproject.data.database.AppDatabase
import com.example.cs501_mealmapproject.data.database.MealPlanDao
import com.example.cs501_mealmapproject.data.database.MealPlanEntity
import com.example.cs501_mealmapproject.data.firestore.MealPlanFirestore
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
 * Repository for managing meal plans with dual persistence:
 * - Room database (local, instant access)
 * - Firestore (cloud sync)
 */
class MealPlanRepository(context: Context) {

    private val mealPlanDao: MealPlanDao = AppDatabase.getDatabase(context).mealPlanDao()
    private val firestore: FirebaseFirestore = Firebase.firestore

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_MEAL_PLANS = "mealPlans"
        const val TAG = "MealPlanRepository"
    }

    private fun planId(date: String, mealType: String) = "${date}_$mealType"

    // ========== Read Operations (from Room) ==========

    /**
     * Get all meal plans (from local Room database)
     */
    fun getAllMealPlans(): Flow<List<MealPlanEntity>> {
        return mealPlanDao.getAllMealPlans()
    }

    /**
     * Get meal plans for a specific week
     */
    fun getMealPlansForWeek(startDate: String, endDate: String): Flow<List<MealPlanEntity>> {
        return mealPlanDao.getMealPlansForWeek(startDate, endDate)
    }

    // ========== Write Operations (Dual Persistence) ==========

    /**
     * Save multiple meal plans at once
     * 1. Write to Room immediately
     * 2. Sync to Firestore in background
     */
    suspend fun saveMealPlans(userId: String, mealPlans: List<MealPlanEntity>): Result<Unit> {
        return try {
            // 1. Write to Room immediately
            mealPlanDao.insertMealPlans(mealPlans)
            Log.d(TAG, "Saved ${mealPlans.size} meal plans to Room")

            // 2. Sync to Firestore in background
            withContext(Dispatchers.IO) {
                try {
                    mealPlans.forEach { plan ->
                        val planDocId = planId(plan.date, plan.mealType)
                        val firestorePlan = plan.toFirestore(planDocId)

                        firestore.collection(COLLECTION_USERS)
                            .document(userId)
                            .collection(COLLECTION_MEAL_PLANS)
                            .document(planDocId)
                            .set(firestorePlan)
                            .await()
                    }

                    Log.d(TAG, "Synced ${mealPlans.size} meal plans to Firestore")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync meal plans to Firestore", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save meal plans", e)
            Result.failure(e)
        }
    }

    /**
     * Save a single meal plan
     */
    suspend fun saveMealPlan(userId: String, mealPlan: MealPlanEntity): Result<Unit> {
        return try {
            // 1. Write to Room
            mealPlanDao.insertMealPlan(mealPlan)

            // 2. Sync to Firestore
            withContext(Dispatchers.IO) {
                try {
                    val planDocId = planId(mealPlan.date, mealPlan.mealType)
                    val firestorePlan = mealPlan.toFirestore(planDocId)

                    firestore.collection(COLLECTION_USERS)
                        .document(userId)
                        .collection(COLLECTION_MEAL_PLANS)
                        .document(planDocId)
                        .set(firestorePlan)
                        .await()

                    Log.d(TAG, "Meal plan synced to Firestore")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync meal plan to Firestore", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save meal plan", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a specific meal plan slot
     */
    suspend fun deleteMealPlan(userId: String, date: String, mealType: String): Result<Unit> {
        return try {
            // 1. Delete from Room
            mealPlanDao.deleteMealPlanByDateAndType(date, mealType)
            Log.d(TAG, "Deleted meal plan from Room: $date $mealType")

            // 2. Mark as deleted in Firestore
            withContext(Dispatchers.IO) {
                try {
                    val planDocId = planId(date, mealType)
                    firestore.collection(COLLECTION_USERS)
                        .document(userId)
                        .collection(COLLECTION_MEAL_PLANS)
                        .document(planDocId)
                        .set(
                            mapOf(
                                "id" to planDocId,
                                "deleted" to true,
                                "lastUpdatedAt" to System.currentTimeMillis()
                            ),
                            com.google.firebase.firestore.SetOptions.merge()
                        )
                        .await()

                    Log.d(TAG, "Meal plan soft deleted in Firestore")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete meal plan from Firestore", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete meal plan", e)
            Result.failure(e)
        }
    }

    // ========== Sync Operations ==========

    /**
     * Sync meal plans from Firestore to Room
     * Called on app launch when user signs in
     */
    suspend fun syncFromFirestore(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Starting sync from Firestore for user: $userId")

            // Pull all non-deleted plans from Firestore
            val firestorePlans = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_MEAL_PLANS)
                .whereEqualTo("deleted", false)
                .get()
                .await()
                .toObjects(MealPlanFirestore::class.java)

            Log.d(TAG, "Found ${firestorePlans.size} meal plans in Firestore")

            // Clear old data and insert new (simple approach for course project)
            mealPlanDao.deleteAllMealPlans()

            // Insert/update in Room
            val entities = firestorePlans.map { it.toEntity() }
            if (entities.isNotEmpty()) {
                mealPlanDao.insertMealPlans(entities)
            }

            Log.d(TAG, "Sync completed: ${firestorePlans.size} meal plans synced")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync from Firestore failed", e)
            Result.failure(e)
        }
    }
}
