package com.ashutosh.mindfultennis.ui.startsession

import androidx.compose.runtime.Immutable
import com.ashutosh.mindfultennis.domain.model.FocusPoint

/**
 * UI state for the Start Session screen.
 */
@Immutable
data class StartSessionUiState(
    val focusNote: String = "",
    val recentFocusPoints: List<FocusPoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionStarted: Boolean = false,
)

/**
 * UI events for the Start Session screen.
 */
sealed interface StartSessionUiEvent {
    data class FocusNoteChanged(val text: String) : StartSessionUiEvent
    data class FocusPointChipClicked(val text: String) : StartSessionUiEvent
    data object StartSessionClicked : StartSessionUiEvent
    data object ErrorDismissed : StartSessionUiEvent
}
