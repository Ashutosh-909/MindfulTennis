package com.ashutosh.mindfultennis.ui.endsession

import androidx.compose.runtime.Immutable
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.MatchType
import com.ashutosh.mindfultennis.domain.model.Opponent
import com.ashutosh.mindfultennis.domain.model.Partner
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.ui.endsession.components.SetScoreInputData

/**
 * UI state for the End Session / Rating screen.
 */
@Immutable
data class EndSessionUiState(
    val session: Session? = null,
    val selfRatings: Map<Aspect, Int> = Aspect.entries.associateWith { 0 },
    val notes: String = "",
    val partnerRatings: Map<Aspect, Int> = emptyMap(),
    val setScoreData: SetScoreInputData? = null,
    val opponents: List<Opponent> = emptyList(),
    val partners: List<Partner> = emptyList(),
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val validationError: String? = null,
    val submitted: Boolean = false,
)

/**
 * UI events for the End Session / Rating screen.
 */
sealed interface EndSessionUiEvent {
    data class SelfRatingChanged(val aspect: Aspect, val rating: Int) : EndSessionUiEvent
    data class NotesChanged(val text: String) : EndSessionUiEvent
    data class PartnerRatingsSaved(val ratings: Map<Aspect, Int>) : EndSessionUiEvent
    data object PartnerRatingsCleared : EndSessionUiEvent
    data class SetScoresSaved(val data: SetScoreInputData) : EndSessionUiEvent
    data object SetScoresCleared : EndSessionUiEvent
    data class OpponentCreated(val name: String) : EndSessionUiEvent
    data class PartnerCreated(val name: String) : EndSessionUiEvent
    data object SubmitClicked : EndSessionUiEvent
    data object ErrorDismissed : EndSessionUiEvent
    data object ValidationErrorDismissed : EndSessionUiEvent
}
