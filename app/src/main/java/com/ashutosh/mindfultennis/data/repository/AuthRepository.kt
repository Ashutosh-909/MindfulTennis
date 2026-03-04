package com.ashutosh.mindfultennis.data.repository

import kotlinx.coroutines.flow.Flow

/**
 * Auth state sealed interface for observing authentication status.
 */
sealed interface AuthState {
    data object Loading : AuthState
    data class Authenticated(
        val userId: String,
        val email: String?,
        val displayName: String?,
        val photoUrl: String?,
    ) : AuthState
    data object Unauthenticated : AuthState
}

/**
 * Repository interface for authentication operations.
 */
interface AuthRepository {

    /** Observe the current authentication state as a Flow. */
    val authState: Flow<AuthState>

    /** Sign in with Google OAuth via Supabase Auth. Returns the redirect URL to open in browser. */
    suspend fun signInWithGoogle(): Result<String>

    /** Handle the OAuth callback URL after Google sign-in. */
    suspend fun handleAuthCallback(url: String): Result<Unit>

    /** Sign out and clear local state. */
    suspend fun signOut(): Result<Unit>

    /** Check if the user is currently authenticated. */
    suspend fun isAuthenticated(): Boolean

    /** Get the current user ID or null. */
    suspend fun getCurrentUserId(): String?
}
