package com.example.cs501_mealmapproject.ui

import androidx.lifecycle.ViewModel
import com.example.cs501_mealmapproject.ui.model.AppUser
import com.example.cs501_mealmapproject.ui.onboarding.OnboardingProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SessionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SessionState())
    val uiState: StateFlow<SessionState> = _uiState.asStateFlow()

    fun signInDemoUser() {
        _uiState.update {
            it.copy(
                user = AppUser(
                    id = "demo-user",
                    displayName = "Demo Student",
                    email = "demo@student.edu"
                ),
                onboardingComplete = it.onboardingProfile != null
            )
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
        _uiState.value = SessionState()
    }
}

data class SessionState(
    val user: AppUser? = null,
    val onboardingProfile: OnboardingProfile? = null,
    val onboardingComplete: Boolean = false
)
