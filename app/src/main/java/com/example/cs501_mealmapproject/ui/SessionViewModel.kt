package com.example.cs501_mealmapproject.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_mealmapproject.data.auth.AuthRepository
import com.example.cs501_mealmapproject.ui.model.AppUser
import com.example.cs501_mealmapproject.ui.onboarding.ActivityLevel
import com.example.cs501_mealmapproject.ui.onboarding.OnboardingProfile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val appContext = application.applicationContext

    private val _uiState = MutableStateFlow(SessionState())
    val uiState: StateFlow<SessionState> = _uiState.asStateFlow()

    init {
        // Listen to Firebase auth state changes
        viewModelScope.launch {
            authRepository.observeAuthState().collect { firebaseUser ->
                if (firebaseUser != null) {
                    // Load saved onboarding profile for this user
                    val savedProfile = loadOnboardingProfile(firebaseUser.uid)
                    
                    _uiState.update {
                        it.copy(
                            user = AppUser(
                                id = firebaseUser.uid,
                                displayName = firebaseUser.displayName ?: "User",
                                email = firebaseUser.email,
                                photoUrl = firebaseUser.photoUrl?.toString()
                            ),
                            onboardingProfile = savedProfile,
                            onboardingComplete = savedProfile != null
                        )
                    }
                    Log.d("SessionViewModel", "User signed in: ${firebaseUser.email}, has profile: ${savedProfile != null}")
                } else {
                    _uiState.update { SessionState() }
                }
            }
        }
    }

    fun getGoogleSignInIntent(): Intent = authRepository.getGoogleSignInIntent()

    suspend fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            Log.d("SessionViewModel", "Got Google account: ${account.email}")

            val idToken = account.idToken ?: throw Exception("No ID token")
            Log.d("SessionViewModel", "Got ID token, signing in with Firebase...")

            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess { user ->
                Log.d("SessionViewModel", "Firebase sign-in successful: ${user.uid}")
            }.onFailure { error ->
                Log.e("SessionViewModel", "Firebase sign-in failed", error)
            }
        } catch (e: ApiException) {
            Log.e("SessionViewModel", "Google Sign-In failed - Status code: ${e.statusCode}", e)
            when (e.statusCode) {
                12500 -> Log.e("SessionViewModel", "Error: SHA-1 fingerprint not configured in Firebase Console")
                10 -> Log.e("SessionViewModel", "Error: Developer error - check google-services.json")
                else -> Log.e("SessionViewModel", "Error code: ${e.statusCode}")
            }
        } catch (e: Exception) {
            Log.e("SessionViewModel", "Sign-in failed", e)
        }
    }

    fun completeOnboarding(profile: OnboardingProfile) {
        val userId = _uiState.value.user?.id
        if (userId != null) {
            saveOnboardingProfile(userId, profile)
        }
        _uiState.update {
            it.copy(onboardingProfile = profile, onboardingComplete = true)
        }
    }

    fun resetOnboarding() {
        val userId = _uiState.value.user?.id
        if (userId != null) {
            // Clear saved profile for this user
            clearOnboardingProfile(userId)
        }
        _uiState.update {
            it.copy(onboardingComplete = false)
        }
    }

    fun signOut() {
        // Sign out from Firebase and Google
        authRepository.signOut()
        
        // Reset session state - user-specific data is stored with userId key,
        // so each user's data remains separate
        _uiState.value = SessionState()
    }
    
    /**
     * Save onboarding profile for a specific user
     */
    private fun saveOnboardingProfile(userId: String, profile: OnboardingProfile) {
        try {
            val prefs = appContext.getSharedPreferences("onboarding_prefs_$userId", Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("calorieTarget", profile.calorieTarget)
                .putFloat("currentWeightLbs", profile.currentWeightLbs)
                .putFloat("goalWeightLbs", profile.goalWeightLbs)
                .putString("activityLevel", profile.activityLevel.name)
                .putBoolean("hasProfile", true)
                .apply()
            Log.d("SessionViewModel", "Saved onboarding profile for user: $userId")
        } catch (e: Exception) {
            Log.e("SessionViewModel", "Failed to save onboarding profile", e)
        }
    }
    
    /**
     * Load onboarding profile for a specific user
     */
    private fun loadOnboardingProfile(userId: String): OnboardingProfile? {
        return try {
            val prefs = appContext.getSharedPreferences("onboarding_prefs_$userId", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("hasProfile", false)) {
                return null
            }
            
            OnboardingProfile(
                calorieTarget = prefs.getInt("calorieTarget", 2000),
                currentWeightLbs = prefs.getFloat("currentWeightLbs", 150f),
                goalWeightLbs = prefs.getFloat("goalWeightLbs", 150f),
                activityLevel = try {
                    ActivityLevel.valueOf(prefs.getString("activityLevel", "Moderate") ?: "Moderate")
                } catch (e: Exception) {
                    ActivityLevel.Moderate
                }
            )
        } catch (e: Exception) {
            Log.e("SessionViewModel", "Failed to load onboarding profile", e)
            null
        }
    }
    
    /**
     * Clear onboarding profile for a specific user (used when resetting goals)
     */
    private fun clearOnboardingProfile(userId: String) {
        try {
            val prefs = appContext.getSharedPreferences("onboarding_prefs_$userId", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d("SessionViewModel", "Cleared onboarding profile for user: $userId")
        } catch (e: Exception) {
            Log.e("SessionViewModel", "Failed to clear onboarding profile", e)
        }
    }
}

data class SessionState(
    val user: AppUser? = null,
    val onboardingProfile: OnboardingProfile? = null,
    val onboardingComplete: Boolean = false
)
