package com.ashutosh.mindfultennis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import com.ashutosh.mindfultennis.data.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load last sync time
            val lastSync = userPreferences.lastSyncTimestamp.first()
            if (lastSync > 0L) {
                _uiState.update { it.copy(lastSyncTime = lastSync) }
            }
        }
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
            is SettingsUiEvent.SyncClicked -> sync()
            is SettingsUiEvent.SyncResultDismissed -> {
                _uiState.update { it.copy(syncResult = null) }
            }
        }
    }

    private fun sync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncResult = null) }
            val userId = userPreferences.cachedUserId.first()
            if (userId == null) {
                _uiState.update { it.copy(isSyncing = false, syncResult = "Not signed in") }
                return@launch
            }
            val result = syncManager.sync(userId)
            val lastSync = userPreferences.lastSyncTimestamp.first()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = if (lastSync > 0L) lastSync else null,
                    syncResult = if (result.isSuccess) "Sync completed" else "Sync failed: ${result.exceptionOrNull()?.message}",
                )
            }
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
