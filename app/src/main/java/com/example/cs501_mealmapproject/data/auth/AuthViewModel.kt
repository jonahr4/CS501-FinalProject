package com.example.cs501_mealmapproject.data.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cs501_mealmapproject.ui.model.AppUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val googleAuthClient: GoogleAuthClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(user = authRepository.getCurrentUser()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user, isLoading = false, errorMessage = null) }
            }
        }
    }

    fun requestGoogleSignIn(launch: (Intent) -> Unit) {
        launch(googleAuthClient.signInIntent)
    }

    fun onSignInResult(intent: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val credential = googleAuthClient.getCredentialFromIntent(intent)
                if (credential == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Sign-in cancelled") }
                    return@launch
                }
                val user = authRepository.signInWithCredential(credential)
                _uiState.update { it.copy(user = user, isLoading = false, errorMessage = null) }
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to sign in") }
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                googleAuthClient.signOut()
            } finally {
                authRepository.signOut()
                _uiState.update { it.copy(user = null) }
                onSignedOut()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        fun provideFactory(
            authRepository: AuthRepository,
            googleAuthClient: GoogleAuthClient
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                    return AuthViewModel(authRepository, googleAuthClient) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
