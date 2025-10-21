package com.example.cs501_mealmapproject.ui.model

data class AppUser(
    val id: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String? = null
)
