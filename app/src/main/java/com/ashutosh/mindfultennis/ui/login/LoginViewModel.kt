package com.ashutosh.mindfultennis.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
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
            LoginUiEvent.RetryClicked -> signInWithGoogle()
            LoginUiEvent.ErrorDismissed -> _uiState.update { it.copy(error = null) }
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
                            it.copy(
                                isLoading = false,
                                isSignedIn = false,
                            )
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
            // On success, the auth state observer will handle the state update
        }
    }
}
