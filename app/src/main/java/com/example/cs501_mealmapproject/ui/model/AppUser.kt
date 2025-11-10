package com.example.cs501_mealmapproject.ui.model


// Class to store data about the user logged in
// TODO: Implement user data to udate when user is logged in.
data class AppUser(
    val id: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String? = null
)
