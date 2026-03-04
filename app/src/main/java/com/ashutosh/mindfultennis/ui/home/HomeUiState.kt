package com.ashutosh.mindfultennis.ui.home

import androidx.compose.runtime.Immutable
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.FocusPoint
import com.ashutosh.mindfultennis.domain.model.Opponent
import com.ashutosh.mindfultennis.domain.model.PerformanceTrend
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.WinLossRecord

/**
 * UI state for the Home / Dashboard screen.
 */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val performanceTrend: List<PerformanceTrend> = emptyList(),
    val selectedDuration: DurationFilter = DurationFilter.ONE_MONTH,
    val winLossRecord: WinLossRecord? = null,
    val focusPoints: List<FocusPoint> = emptyList(),
    val aspectAverages: Map<Aspect, Float> = emptyMap(),
    val opponents: List<Opponent> = emptyList(),
    val selectedWinLossOpponentIds: Set<String> = emptySet(),
    val selectedAspectOpponentIds: Set<String> = emptySet(),
    val activeSession: Session? = null,
    val activeSessionElapsedMs: Long = 0L,
    val trendError: String? = null,
    val winLossError: String? = null,
    val aspectError: String? = null,
    val error: String? = null,
)

/**
 * UI events from the Home / Dashboard screen.
 */
sealed interface HomeUiEvent {
    data class DurationChanged(val duration: DurationFilter) : HomeUiEvent
    data class WinLossOpponentFilterChanged(val ids: Set<String>) : HomeUiEvent
    data class AspectOpponentFilterChanged(val ids: Set<String>) : HomeUiEvent
    data object StartSessionClicked : HomeUiEvent
    data object EndSessionClicked : HomeUiEvent
    data object ShowSessionsClicked : HomeUiEvent
    data object RetryClicked : HomeUiEvent
    data object RefreshClicked : HomeUiEvent
    data object ErrorDismissed : HomeUiEvent
}
