package com.ashutosh.mindfultennis.ui.settings

data class SettingsUiState(
    val email: String? = null,
    val displayName: String? = null,
    val isSigningOut: Boolean = false,
    val isSignedOut: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val syncResult: String? = null,
)

sealed interface SettingsUiEvent {
    data object SignOutClicked : SettingsUiEvent
    data object SyncClicked : SettingsUiEvent
    data object SyncResultDismissed : SettingsUiEvent
}
