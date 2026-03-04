package com.ashutosh.mindfultennis.ui.login

/**
 * UI state for the Login screen.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val displayName: String? = null,
    val error: String? = null,
)

/**
 * UI events emitted from the Login screen.
 */
sealed interface LoginUiEvent {
    data object SignInWithGoogleClicked : LoginUiEvent
    data object RetryClicked : LoginUiEvent
    data object ErrorDismissed : LoginUiEvent
}
