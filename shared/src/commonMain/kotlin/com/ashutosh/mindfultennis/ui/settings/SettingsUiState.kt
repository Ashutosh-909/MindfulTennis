package com.ashutosh.mindfultennis.ui.settings

data class SettingsUiState(
    val email: String? = null,
    val displayName: String? = null,
    val isSigningOut: Boolean = false,
    val isSignedOut: Boolean = false,
)

sealed interface SettingsUiEvent {
    data object SignOutClicked : SettingsUiEvent
}
