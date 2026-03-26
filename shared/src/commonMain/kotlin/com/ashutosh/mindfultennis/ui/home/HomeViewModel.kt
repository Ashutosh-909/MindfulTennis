package com.ashutosh.mindfultennis.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import com.ashutosh.mindfultennis.data.repository.FocusPointRepository
import com.ashutosh.mindfultennis.data.repository.OpponentRepository
import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.data.sync.InitialSyncManager
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.RatingType
import com.ashutosh.mindfultennis.domain.usecase.CancelSessionUseCase
import com.ashutosh.mindfultennis.domain.usecase.GetAspectAveragesUseCase
import com.ashutosh.mindfultennis.domain.usecase.GetPerformanceTrendUseCase
import com.ashutosh.mindfultennis.domain.usecase.GetWinLossRecordUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val focusPointRepository: FocusPointRepository,
    private val opponentRepository: OpponentRepository,
    private val userPreferences: UserPreferences,
    private val initialSyncManager: InitialSyncManager,
    private val cancelSessionUseCase: CancelSessionUseCase,
    private val getPerformanceTrendUseCase: GetPerformanceTrendUseCase,
    private val getWinLossRecordUseCase: GetWinLossRecordUseCase,
    private val getAspectAveragesUseCase: GetAspectAveragesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var trendJob: Job? = null
    private var winLossJob: Job? = null
    private var aspectJob: Job? = null
    private var timerJob: Job? = null

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            // Restore saved filter preferences
            val savedFilter = userPreferences.durationFilter.first()
            val duration = runCatching { DurationFilter.valueOf(savedFilter) }
                .getOrDefault(DurationFilter.ONE_MONTH)
            val savedWinLossOpponents = userPreferences.selectedOpponentIds.first()
            val savedAspectRatingType = userPreferences.aspectRatingType.first()
            val aspectRatingType = runCatching { RatingType.valueOf(savedAspectRatingType) }
                .getOrDefault(RatingType.SELF)

            _uiState.update {
                it.copy(
                    selectedDuration = duration,
                    selectedWinLossOpponentIds = savedWinLossOpponents,
                    selectedAspectRatingType = aspectRatingType,
                )
            }

            // Observe auth state to get user ID
            authRepository.authState.collectLatest { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        currentUserId = authState.userId
                        // Cache the user ID for SyncWorker
                        userPreferences.setCachedUserId(authState.userId)
                        // Check if initial sync is needed
                        if (!userPreferences.getHasCompletedInitialSync()) {
                            performInitialSync(authState.userId)
                        } else {
                            loadAllData(authState.userId)
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        currentUserId = null
                        _uiState.update { HomeUiState(isLoading = false) }
                    }
                    is AuthState.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.DurationChanged -> onDurationChanged(event.duration)
            is HomeUiEvent.WinLossOpponentFilterChanged -> onWinLossOpponentFilterChanged(event.ids)
            is HomeUiEvent.AspectOpponentFilterChanged -> onAspectOpponentFilterChanged(event.ids)
            is HomeUiEvent.AspectRatingTypeChanged -> onAspectRatingTypeChanged(event.ratingType)
            is HomeUiEvent.RetryClicked -> retry()
            is HomeUiEvent.ErrorDismissed -> _uiState.update { it.copy(error = null) }
            is HomeUiEvent.CancelSessionClicked -> {
                _uiState.update { it.copy(showCancelSessionDialog = true) }
            }
            is HomeUiEvent.CancelSessionDismissed -> {
                _uiState.update { it.copy(showCancelSessionDialog = false) }
            }
            is HomeUiEvent.CancelSessionConfirmed -> cancelActiveSession()
            // Navigation events handled by screen
            is HomeUiEvent.StartSessionClicked,
            is HomeUiEvent.EndSessionClicked,
            is HomeUiEvent.ShowSessionsClicked,
            is HomeUiEvent.NavigateToSettingsClicked -> { /* handled by HomeScreen */ }
        }
    }

    /**
     * Performs the initial sync for a new user, then loads all data from Room.
     */
    private fun performInitialSync(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialSyncing = true) }
            val result = initialSyncManager.performInitialSync(userId)
            _uiState.update { it.copy(isInitialSyncing = false) }
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Initial sync failed") }
            }
            loadAllData(userId)
        }
    }

    private fun loadAllData(userId: String) {
        _uiState.update { it.copy(isLoading = false) }
        observeActiveSession(userId)
        observeFocusPoints(userId)
        observeOpponents(userId)
        refreshTrend(userId)
        refreshWinLoss(userId)
        refreshAspects(userId)
    }

    private fun observeActiveSession(userId: String) {
        viewModelScope.launch {
            sessionRepository.observeActiveSession(userId)
                .catch { /* ignore active session errors */ }
                .collectLatest { session ->
                    _uiState.update { it.copy(activeSession = session) }
                    updateTimer(session != null, session?.startedAt ?: 0L)
                }
        }
    }

    private fun updateTimer(hasActive: Boolean, startedAt: Long) {
        timerJob?.cancel()
        if (!hasActive) {
            _uiState.update { it.copy(activeSessionElapsedMs = 0L) }
            return
        }
        timerJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update {
                    it.copy(activeSessionElapsedMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startedAt)
                }
                delay(1_000L)
            }
        }
    }

    private fun observeFocusPoints(userId: String) {
        viewModelScope.launch {
            focusPointRepository.observeAll(userId)
                .catch { /* ignore */ }
                .collectLatest { _ ->
                    // Re-compute average scores whenever the focus points list changes
                    val pointsWithScores = focusPointRepository.getAllWithAverageScore(userId)
                    _uiState.update { it.copy(focusPoints = pointsWithScores) }
                }
        }
    }

    private fun observeOpponents(userId: String) {
        viewModelScope.launch {
            opponentRepository.observeAll(userId)
                .catch { /* ignore */ }
                .collectLatest { opponents ->
                    _uiState.update { it.copy(opponents = opponents) }
                }
        }
    }

    private fun refreshTrend(userId: String) {
        trendJob?.cancel()
        trendJob = viewModelScope.launch {
            getPerformanceTrendUseCase(userId, _uiState.value.selectedDuration)
                .catch { e ->
                    _uiState.update { it.copy(trendError = e.message ?: "Failed to load trend") }
                }
                .collectLatest { trend ->
                    _uiState.update { it.copy(performanceTrend = trend, trendError = null) }
                }
        }
    }

    private fun refreshWinLoss(userId: String) {
        winLossJob?.cancel()
        winLossJob = viewModelScope.launch {
            getWinLossRecordUseCase(
                userId,
                _uiState.value.selectedDuration,
                _uiState.value.selectedWinLossOpponentIds,
            )
                .catch { e ->
                    _uiState.update { it.copy(winLossError = e.message ?: "Failed to load W/L") }
                }
                .collectLatest { record ->
                    _uiState.update { it.copy(winLossRecord = record, winLossError = null) }
                }
        }
    }

    private fun refreshAspects(userId: String) {
        aspectJob?.cancel()
        aspectJob = viewModelScope.launch {
            val duration = _uiState.value.selectedDuration
            val opponentIds = _uiState.value.selectedAspectOpponentIds
            val selfFlow = getAspectAveragesUseCase(userId, duration, opponentIds, RatingType.SELF)
            val partnerFlow = getAspectAveragesUseCase(userId, duration, opponentIds, RatingType.PARTNER)
            combine(selfFlow, partnerFlow) { self, partner -> self to partner }
                .catch { e ->
                    _uiState.update { it.copy(aspectError = e.message ?: "Failed to load aspects") }
                }
                .collectLatest { (selfAvg, partnerAvg) ->
                    _uiState.update {
                        it.copy(
                            selfAspectAverages = selfAvg,
                            partnerAspectAverages = partnerAvg,
                            aspectError = null,
                        )
                    }
                }
        }
    }

    private fun onDurationChanged(duration: DurationFilter) {
        _uiState.update { it.copy(selectedDuration = duration) }
        viewModelScope.launch {
            userPreferences.setDurationFilter(duration.name)
        }
        currentUserId?.let { userId ->
            refreshTrend(userId)
            refreshWinLoss(userId)
            refreshAspects(userId)
        }
    }

    private fun onAspectRatingTypeChanged(ratingType: RatingType) {
        _uiState.update { it.copy(selectedAspectRatingType = ratingType) }
        viewModelScope.launch {
            userPreferences.setAspectRatingType(ratingType.name)
        }
        currentUserId?.let { refreshAspects(it) }
    }

    private fun onWinLossOpponentFilterChanged(ids: Set<String>) {
        _uiState.update { it.copy(selectedWinLossOpponentIds = ids) }
        viewModelScope.launch {
            userPreferences.setSelectedOpponentIds(ids)
        }
        currentUserId?.let { refreshWinLoss(it) }
    }

    private fun onAspectOpponentFilterChanged(ids: Set<String>) {
        _uiState.update { it.copy(selectedAspectOpponentIds = ids) }
        currentUserId?.let { refreshAspects(it) }
    }

    private fun retry() {
        currentUserId?.let { userId ->
            _uiState.update {
                it.copy(trendError = null, winLossError = null, aspectError = null, error = null)
            }
            loadAllData(userId)
        }
    }

    private fun cancelActiveSession() {
        val session = _uiState.value.activeSession ?: return
        _uiState.update { it.copy(showCancelSessionDialog = false, isCancellingSession = true) }

        viewModelScope.launch {
            val result = cancelSessionUseCase(session.id)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isCancellingSession = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isCancellingSession = false,
                            error = e.message ?: "Failed to cancel session",
                        )
                    }
                },
            )
        }
    }
}
