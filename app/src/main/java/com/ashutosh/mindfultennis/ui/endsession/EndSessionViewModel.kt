package com.ashutosh.mindfultennis.ui.endsession

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import com.ashutosh.mindfultennis.data.repository.OpponentRepository
import com.ashutosh.mindfultennis.data.repository.PartnerRepository
import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.MatchType
import com.ashutosh.mindfultennis.domain.model.Opponent
import com.ashutosh.mindfultennis.domain.model.Partner
import com.ashutosh.mindfultennis.domain.model.Rating
import com.ashutosh.mindfultennis.domain.model.SetScore
import com.ashutosh.mindfultennis.domain.usecase.SubmitRatingsUseCase
import com.ashutosh.mindfultennis.navigation.Route
import com.ashutosh.mindfultennis.ui.endsession.components.SetScoreInputData
import com.ashutosh.mindfultennis.util.generateId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EndSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val opponentRepository: OpponentRepository,
    private val partnerRepository: PartnerRepository,
    private val submitRatingsUseCase: SubmitRatingsUseCase,
) : ViewModel() {

    private val sessionId: String = checkNotNull(
        savedStateHandle[Route.EndSession.ARG_SESSION_ID]
    )

    private val _uiState = MutableStateFlow(EndSessionUiState())
    val uiState: StateFlow<EndSessionUiState> = _uiState.asStateFlow()

    init {
        loadSession()
        loadOpponentsAndPartners()
    }

    fun onEvent(event: EndSessionUiEvent) {
        when (event) {
            is EndSessionUiEvent.SelfRatingChanged -> {
                _uiState.update { state ->
                    state.copy(
                        selfRatings = state.selfRatings.toMutableMap().also {
                            it[event.aspect] = event.rating
                        },
                        validationError = null,
                    )
                }
            }
            is EndSessionUiEvent.NotesChanged -> {
                _uiState.update { it.copy(notes = event.text) }
            }
            is EndSessionUiEvent.PartnerRatingsSaved -> {
                _uiState.update { it.copy(partnerRatings = event.ratings) }
            }
            is EndSessionUiEvent.PartnerRatingsCleared -> {
                _uiState.update { it.copy(partnerRatings = emptyMap()) }
            }
            is EndSessionUiEvent.SetScoresSaved -> {
                _uiState.update { it.copy(setScoreData = event.data) }
            }
            is EndSessionUiEvent.SetScoresCleared -> {
                _uiState.update { it.copy(setScoreData = null) }
            }
            is EndSessionUiEvent.OpponentCreated -> createOpponent(event.name)
            is EndSessionUiEvent.PartnerCreated -> createPartner(event.name)
            is EndSessionUiEvent.SubmitClicked -> submit()
            is EndSessionUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
            }
            is EndSessionUiEvent.ValidationErrorDismissed -> {
                _uiState.update { it.copy(validationError = null) }
            }
        }
    }

    private fun loadSession() {
        viewModelScope.launch {
            sessionRepository.observeSession(sessionId)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load session",
                        )
                    }
                }
                .collectLatest { session ->
                    _uiState.update {
                        it.copy(
                            session = session,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    private fun loadOpponentsAndPartners() {
        viewModelScope.launch {
            val authState = authRepository.authState.first { it !is AuthState.Loading }
            if (authState is AuthState.Authenticated) {
                // Observe opponents
                launch {
                    opponentRepository.observeAll(authState.userId)
                        .catch { /* ignore */ }
                        .collectLatest { opponents ->
                            _uiState.update { it.copy(opponents = opponents) }
                        }
                }
                // Observe partners
                launch {
                    partnerRepository.observeAll(authState.userId)
                        .catch { /* ignore */ }
                        .collectLatest { partners ->
                            _uiState.update { it.copy(partners = partners) }
                        }
                }
            }
        }
    }

    private fun createOpponent(name: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val opponent = Opponent(
                id = generateId(),
                userId = userId,
                name = name,
                createdAt = System.currentTimeMillis(),
            )
            opponentRepository.create(opponent)
        }
    }

    private fun createPartner(name: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val partner = Partner(
                id = generateId(),
                userId = userId,
                name = name,
                createdAt = System.currentTimeMillis(),
            )
            partnerRepository.create(partner)
        }
    }

    private fun submit() {
        val state = _uiState.value

        // Validate at least one self-rating
        val ratedAspects = state.selfRatings.filter { it.value > 0 }
        if (ratedAspects.isEmpty()) {
            _uiState.update {
                it.copy(validationError = "Please rate at least one aspect before submitting.")
            }
            return
        }

        if (state.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, error = null, validationError = null) }

        viewModelScope.launch {
            // Build self-ratings
            val selfRatings = ratedAspects.map { (aspect, rating) ->
                Rating(
                    id = generateId(),
                    sessionId = sessionId,
                    aspect = aspect,
                    rating = rating,
                )
            }

            // Build partner ratings
            val partnerRatings = state.partnerRatings
                .filter { it.value > 0 }
                .map { (aspect, rating) ->
                    Rating(
                        id = generateId(),
                        sessionId = sessionId,
                        aspect = aspect,
                        rating = rating,
                    )
                }

            // Build set scores
            val setScoreData = state.setScoreData
            val setScores = setScoreData?.sets
                ?.filter { it.userScore.isNotBlank() && it.opponentScore.isNotBlank() }
                ?.mapIndexed { index, row ->
                    SetScore(
                        id = generateId(),
                        sessionId = sessionId,
                        setNumber = index + 1,
                        userScore = row.userScore.toIntOrNull() ?: 0,
                        opponentScore = row.opponentScore.toIntOrNull() ?: 0,
                        opponentId = row.opponent?.id,
                    )
                } ?: emptyList()

            val matchType = setScoreData?.matchType ?: MatchType.SINGLES
            val opponent1Id = setScores.firstOrNull { it.opponentId != null }?.opponentId
            val opponent2Id = if (matchType == MatchType.DOUBLES) setScoreData?.opponent2?.id else null
            val partnerId = if (matchType == MatchType.DOUBLES) setScoreData?.partner?.id else null

            val result = submitRatingsUseCase(
                sessionId = sessionId,
                selfRatings = selfRatings,
                partnerRatings = partnerRatings,
                setScores = setScores,
                notes = state.notes.ifBlank { null },
                matchType = matchType,
                opponent1Id = opponent1Id,
                opponent2Id = opponent2Id,
                partnerId = partnerId,
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, submitted = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = e.message ?: "Failed to submit session",
                        )
                    }
                },
            )
        }
    }
}
