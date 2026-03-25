package com.ashutosh.mindfultennis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lastSync = userPreferences.lastSyncTimestamp.first()
            _uiState.update { it.copy(lastSyncTime = if (lastSync > 0L) lastSync else null) }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SyncClicked -> onSyncClicked()
            is SettingsUiEvent.LogoutClicked -> onLogoutClicked()
            is SettingsUiEvent.SyncResultDismissed -> _uiState.update { it.copy(syncResult = null) }
        }
    }

    private fun onSyncClicked() {
        viewModelScope.launch {
            val userId = userPreferences.cachedUserId.first()
            if (userId.isNullOrBlank()) {
                _uiState.update { it.copy(syncResult = "No user ID found. Please log in again.") }
                return@launch
            }

            _uiState.update { it.copy(isSyncing = true, syncResult = null) }
            val result = syncManager.sync(userId)
            val lastSync = userPreferences.lastSyncTimestamp.first()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = if (lastSync > 0L) lastSync else null,
                    syncResult = if (result.isSuccess) "Sync completed successfully" else "Sync failed: ${result.exceptionOrNull()?.message}",
                )
            }
        }
    }

    private fun onLogoutClicked() {
        viewModelScope.launch {
            // Reset initial sync flag so next login triggers full pull
            userPreferences.setHasCompletedInitialSync(false)
            userPreferences.setLastSyncTimestamp(0L)
            userPreferences.setCachedUserId(null)
            authRepository.signOut()
        }
    }
}
