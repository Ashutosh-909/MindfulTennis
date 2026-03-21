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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: "Sign-in failed. Please try again.",
                    )
                }
            }
        }
    }

    private fun signInWithEmail() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val isSignUp = _uiState.value.isSignUpMode

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Please enter both email and password.") }
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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = exception.message ?: if (isSignUp) "Sign-up failed." else "Sign-in failed.",
                    )
                }
            }
        }
    }
}
