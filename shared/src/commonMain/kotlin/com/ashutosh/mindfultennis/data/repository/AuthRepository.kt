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

    /** Sign in with Apple via Supabase Auth (iOS only). */
    suspend fun signInWithApple(): Result<Unit>

    /** Sign in with email and password. */
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    /** Sign up with email and password. */
    suspend fun signUpWithEmail(email: String, password: String): Result<Unit>

    /** Handle the OAuth callback URL after Google sign-in. */
    suspend fun handleAuthCallback(url: String): Result<Unit>

    /** Sign out and clear local state. */
    suspend fun signOut(): Result<Unit>

    /** Check if the user is currently authenticated. */
    suspend fun isAuthenticated(): Boolean

    /** Get the current user ID or null. */
    suspend fun getCurrentUserId(): String?
}
