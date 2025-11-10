package com.example.cs501_mealmapproject.data.auth

import android.content.Context
import android.content.Intent

/**
 * No-op Google auth client used until Firebase/Play Services is re-enabled.
 */
class GoogleAuthClient(
    context: Context,
    webClientId: String
) {

    @Suppress("unused")
    private val appContext = context.applicationContext

    val signInIntent: Intent
        get() = Intent(Intent.ACTION_VIEW)

    @Suppress("UNUSED_PARAMETER")
    suspend fun getCredentialFromIntent(intent: Intent?): AuthCredential? = null

    suspend fun signOut() = Unit
}
