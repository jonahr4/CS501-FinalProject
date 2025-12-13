package com.example.cs501_mealmapproject.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cs501_mealmapproject.data.auth.AuthRepository
import com.example.cs501_mealmapproject.ui.model.AppUser
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

    private val _uiState = MutableStateFlow(SessionState())
    val uiState: StateFlow<SessionState> = _uiState.asStateFlow()

    init {
        // Listen to Firebase auth state changes
        viewModelScope.launch {
            authRepository.observeAuthState().collect { firebaseUser ->
                if (firebaseUser != null) {
                    _uiState.update {
                        it.copy(
                            user = AppUser(
                                id = firebaseUser.uid,
                                displayName = firebaseUser.displayName ?: "User",
                                email = firebaseUser.email,
                                photoUrl = firebaseUser.photoUrl?.toString()
                            ),
                            onboardingComplete = it.onboardingProfile != null
                        )
                    }
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
        _uiState.update {
            it.copy(onboardingProfile = profile, onboardingComplete = true)
        }
    }

    fun resetOnboarding() {
        _uiState.update {
            it.copy(onboardingComplete = false)
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = SessionState()
    }
}

data class SessionState(
    val user: AppUser? = null,
    val onboardingProfile: OnboardingProfile? = null,
    val onboardingComplete: Boolean = false
)
