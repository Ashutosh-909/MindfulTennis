package com.ashutosh.mindfultennis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        _uiState.update {
                            it.copy(
                                email = authState.email,
                                displayName = authState.displayName,
                            )
                        }
                    }
                    is AuthState.Unauthenticated,
                    is AuthState.Loading -> { /* no-op */ }
                }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SignOutClicked -> signOut()
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            userPreferences.clearAll()
            authRepository.signOut()
            _uiState.update { it.copy(isSigningOut = false, isSignedOut = true) }
        }
    }
}
