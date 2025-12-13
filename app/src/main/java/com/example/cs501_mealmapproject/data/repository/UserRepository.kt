package com.example.cs501_mealmapproject.data.repository

import android.util.Log
import com.example.cs501_mealmapproject.data.firestore.OnboardingProfileFirestore
import com.example.cs501_mealmapproject.data.firestore.UserProfileFirestore
import com.example.cs501_mealmapproject.data.firestore.toAppUser
import com.example.cs501_mealmapproject.data.firestore.toFirestore
import com.example.cs501_mealmapproject.data.firestore.toOnboardingProfile
import com.example.cs501_mealmapproject.ui.model.AppUser
import com.example.cs501_mealmapproject.ui.onboarding.OnboardingProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing user profile and onboarding data in Firestore
 */
class UserRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore

    private companion object {
        const val COLLECTION_USERS = "users"
        const val DOCUMENT_PROFILE = "profile"
        const val DOCUMENT_ONBOARDING = "onboardingProfile"
        const val TAG = "UserRepository"
    }

    // ========== User Profile ==========

    /**
     * Save user profile to Firestore
     */
    suspend fun saveUserProfile(user: AppUser): Result<Unit> {
        return try {
            val firestoreProfile = user.toFirestore()

            firestore.collection(COLLECTION_USERS)
                .document(user.id)
                .collection(DOCUMENT_PROFILE)
                .document("data")
                .set(firestoreProfile)
                .await()

            Log.d(TAG, "User profile saved: ${user.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile", e)
            Result.failure(e)
        }
    }

    /**
     * Load user profile from Firestore
     */
    suspend fun getUserProfile(userId: String): AppUser? {
        return try {
            val doc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(DOCUMENT_PROFILE)
                .document("data")
                .get()
                .await()

            if (doc.exists()) {
                val firestoreProfile = doc.toObject(UserProfileFirestore::class.java)
                firestoreProfile?.toAppUser()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load user profile", e)
            null
        }
    }

    // ========== Onboarding Profile ==========

    /**
     * Save onboarding profile to Firestore
     */
    suspend fun saveOnboardingProfile(userId: String, profile: OnboardingProfile): Result<Unit> {
        return try {
            val firestoreProfile = profile.toFirestore()

            firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(DOCUMENT_ONBOARDING)
                .document("data")
                .set(firestoreProfile)
                .await()

            Log.d(TAG, "Onboarding profile saved for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save onboarding profile", e)
            Result.failure(e)
        }
    }

    /**
     * Load onboarding profile from Firestore
     */
    suspend fun getOnboardingProfile(userId: String): OnboardingProfile? {
        return try {
            val doc = firestore.collection(COLLECTION_USERS)
                .document(userId)
                .collection(DOCUMENT_ONBOARDING)
                .document("data")
                .get()
                .await()

            if (doc.exists()) {
                val firestoreProfile = doc.toObject(OnboardingProfileFirestore::class.java)
                firestoreProfile?.toOnboardingProfile()
            } else {
                Log.d(TAG, "No onboarding profile found for user: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load onboarding profile", e)
            null
        }
    }
}
