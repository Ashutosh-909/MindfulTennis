package com.ashutosh.mindfultennis.ui.settings

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val syncResult: String? = null,
)

sealed interface SettingsUiEvent {
    data object SyncClicked : SettingsUiEvent
    data object LogoutClicked : SettingsUiEvent
    data object SyncResultDismissed : SettingsUiEvent
}
