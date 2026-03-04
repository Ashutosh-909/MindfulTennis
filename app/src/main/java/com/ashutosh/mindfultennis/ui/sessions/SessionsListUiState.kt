package com.ashutosh.mindfultennis.ui.sessions

import androidx.compose.runtime.Immutable
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.Session

/**
 * UI state for the Sessions List screen.
 */
@Immutable
data class SessionsListUiState(
    val sessions: List<Session> = emptyList(),
    val selectedDuration: DurationFilter = DurationFilter.ONE_MONTH,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * UI events for the Sessions List screen.
 */
sealed interface SessionsListUiEvent {
    data class DurationChanged(val filter: DurationFilter) : SessionsListUiEvent
    data class SessionClicked(val sessionId: String) : SessionsListUiEvent
    data object RetryClicked : SessionsListUiEvent
    data object ErrorDismissed : SessionsListUiEvent
}
