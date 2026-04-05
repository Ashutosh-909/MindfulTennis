package com.ashutosh.mindfultennis.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : AuthRepository {

    override val authState: Flow<AuthState> =
        supabaseClient.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    AuthState.Authenticated(
                        userId = user?.id ?: "",
                        email = user?.email,
                        displayName = user?.userMetadata?.get("full_name")?.toString()
                            ?.removeSurrounding("\""),
                        photoUrl = user?.userMetadata?.get("avatar_url")?.toString()
                            ?.removeSurrounding("\""),
                    )
                }
                is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
                is SessionStatus.Initializing -> AuthState.Loading
                is SessionStatus.RefreshFailure -> AuthState.Unauthenticated
            }
        }

    override suspend fun signInWithGoogle(): Result<String> {
        return try {
            // Initiate Google OAuth — this returns a URL to open in the device browser.
            // The Supabase SDK handles the OAuth flow via a redirect.
            supabaseClient.auth.signInWith(Google)
            Result.success("")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithApple(): Result<Unit> {
        return try {
            supabaseClient.auth.signInWith(Apple)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<Unit> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun handleAuthCallback(url: String): Result<Unit> {
        return try {
            // The Supabase SDK's deeplink handler will process the callback URL
            // This is handled automatically when the redirect comes back to the app
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isAuthenticated(): Boolean {
        return supabaseClient.auth.currentSessionOrNull() != null
    }

    override suspend fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentSessionOrNull()?.user?.id
    }
}
