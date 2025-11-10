package com.example.cs501_mealmapproject.data.auth

import com.example.cs501_mealmapproject.ui.model.AppUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simplified in-memory auth store while Firebase is disabled.
 */
class AuthRepository {

    private val userState = MutableStateFlow<AppUser?>(null)

    val currentUser: Flow<AppUser?> = userState.asStateFlow()

    suspend fun signInWithCredential(@Suppress("UNUSED_PARAMETER") credential: AuthCredential): AppUser? {
        throw UnsupportedOperationException("Firebase authentication is temporarily disabled")
    }

    fun getCurrentUser(): AppUser? = userState.value

    fun signOut() {
        userState.value = null
    }
}
