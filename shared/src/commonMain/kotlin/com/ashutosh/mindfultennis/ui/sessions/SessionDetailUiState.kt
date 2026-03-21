package com.ashutosh.mindfultennis.ui.sessions

import androidx.compose.runtime.Immutable
import com.ashutosh.mindfultennis.domain.model.Opponent
import com.ashutosh.mindfultennis.domain.model.Partner
import com.ashutosh.mindfultennis.domain.model.Rating
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.SetScore

/**
 * UI state for the Session Detail screen (read-only).
 */
@Immutable
data class SessionDetailUiState(
    val session: Session? = null,
    val selfRatings: List<Rating> = emptyList(),
    val partnerRatings: List<Rating> = emptyList(),
    val setScores: List<SetScore> = emptyList(),
    val setOpponentNames: Map<String, String> = emptyMap(),
    val opponent1: Opponent? = null,
    val opponent2: Opponent? = null,
    val partner: Partner? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * UI events for the Session Detail screen.
 */
sealed interface SessionDetailUiEvent {
    data object RetryClicked : SessionDetailUiEvent
    data object ErrorDismissed : SessionDetailUiEvent
}
