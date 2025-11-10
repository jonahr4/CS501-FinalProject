package com.example.cs501_mealmapproject.data.auth

import com.example.cs501_mealmapproject.ui.model.AppUser

data class AuthUiState(
    val user: AppUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
