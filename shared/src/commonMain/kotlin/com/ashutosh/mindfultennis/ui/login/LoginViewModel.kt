package com.ashutosh.mindfultennis.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            LoginUiEvent.SignInWithGoogleClicked -> signInWithGoogle()
            LoginUiEvent.SignInWithAppleClicked -> signInWithApple()
            is LoginUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.email) }
            is LoginUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.password) }
            LoginUiEvent.SignInWithEmailClicked -> signInWithEmail()
            LoginUiEvent.ToggleSignUpMode -> _uiState.update { it.copy(isSignUpMode = !it.isSignUpMode, error = null) }
            LoginUiEvent.RetryClicked -> signInWithGoogle()
            LoginUiEvent.ErrorDismissed -> _uiState.update { it.copy(error = null) }
            LoginUiEvent.SignUpSuccessDismissed -> _uiState.update { it.copy(signUpSuccess = null) }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSignedIn = true,
                                displayName = state.displayName,
                                error = null,
                            )
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        _uiState.update {
                            it.copy(isLoading = false, isSignedIn = false)
                        }
                    }
                    is AuthState.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    private fun signInWithGoogle() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle()
            result.onFailure { exception ->
                val friendlyMessage = when {
                    exception.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timed out. Please check your internet and try again."
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your connection and try again."
                    else -> "Google sign-in failed. Please try again."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = friendlyMessage,
                    )
                }
            }
        }
    }

    private fun signInWithApple() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signInWithApple()
            result.onFailure { exception ->
                val friendlyMessage = when {
                    exception.message?.contains("timeout", ignoreCase = true) == true ->
                        "Connection timed out. Please check your internet and try again."
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Please check your connection and try again."
                    else -> "Apple sign-in failed. Please try again."
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = friendlyMessage,
                    )
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val pattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return pattern.matches(email)
    }

    private fun signInWithEmail() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val isSignUp = _uiState.value.isSignUpMode

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please enter both email and password.") }
            return
        }
        if (!isValidEmail(email)) {
            _uiState.update { it.copy(error = "Please enter a valid email address.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "Password must be at least 6 characters.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val result = if (isSignUp) {
                authRepository.signUpWithEmail(email, password)
            } else {
                authRepository.signInWithEmail(email, password)
            }
            result.onSuccess {
                if (isSignUp) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            signUpSuccess = "Account created! Check your email to verify, then sign in.",
                            isSignUpMode = false,
                            password = "",
                        )
                    }
                }
                // For sign-in, the auth state observer handles the transition
            }
            result.onFailure { exception ->
                val msg = exception.message.orEmpty()
                val friendlyMessage = when {
                    msg.contains("rate limit", ignoreCase = true) ||
                        msg.contains("429", ignoreCase = true) ->
                        "Too many attempts. Please wait a few minutes and try again."
                    msg.contains("timeout", ignoreCase = true) ->
                        "Connection timed out. Please check your internet and try again."
                    msg.contains("Invalid login credentials", ignoreCase = true) ->
                        "Invalid email or password. Please try again."
                    msg.contains("Email not confirmed", ignoreCase = true) ->
                        "Please verify your email before signing in."
                    msg.contains("email_address_invalid", ignoreCase = true) ||
                        msg.contains("invalid.*email", ignoreCase = true).let { false } ||
                        msg.contains("email address", ignoreCase = true) && msg.contains("invalid", ignoreCase = true) ->
                        "Please enter a valid email address."
                    msg.contains("User already registered", ignoreCase = true) ->
                        "An account with this email already exists. Try signing in instead."
                    msg.contains("network", ignoreCase = true) ->
                        "Network error. Please check your connection and try again."
                    isSignUp -> "Sign-up failed: ${msg.take(100)}"
                    else -> "Sign-in failed: ${msg.take(100)}"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = friendlyMessage,
                    )
                }
            }
        }
    }
}
