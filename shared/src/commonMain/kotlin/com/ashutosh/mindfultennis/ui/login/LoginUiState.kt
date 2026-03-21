package com.ashutosh.mindfultennis.ui.login

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val displayName: String? = null,
    val error: String? = null,
    val email: String = "",
    val password: String = "",
    val isSignUpMode: Boolean = false,
    val signUpSuccess: String? = null,
)

sealed interface LoginUiEvent {
    data object SignInWithGoogleClicked : LoginUiEvent
    data class EmailChanged(val email: String) : LoginUiEvent
    data class PasswordChanged(val password: String) : LoginUiEvent
    data object SignInWithEmailClicked : LoginUiEvent
    data object ToggleSignUpMode : LoginUiEvent
    data object RetryClicked : LoginUiEvent
    data object ErrorDismissed : LoginUiEvent
    data object SignUpSuccessDismissed : LoginUiEvent
}
