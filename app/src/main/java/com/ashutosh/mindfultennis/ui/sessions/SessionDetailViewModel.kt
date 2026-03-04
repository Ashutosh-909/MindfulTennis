package com.ashutosh.mindfultennis.ui.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.repository.OpponentRepository
import com.ashutosh.mindfultennis.data.repository.PartnerRepository
import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val opponentRepository: OpponentRepository,
    private val partnerRepository: PartnerRepository,
) : ViewModel() {

    private val sessionId: String = checkNotNull(
        savedStateHandle[Route.SessionDetail.ARG_SESSION_ID]
    )

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        loadSession()
        loadRatingsAndScores()
    }

    fun onEvent(event: SessionDetailUiEvent) {
        when (event) {
            is SessionDetailUiEvent.RetryClicked -> {
                _uiState.update { it.copy(isLoading = true, error = null) }
                loadSession()
                loadRatingsAndScores()
            }
            is SessionDetailUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
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
                    _uiState.update { it.copy(session = session, isLoading = false) }
                    // Load opponent and partner info if available
                    session?.let { loadOpponentAndPartner(it) }
                }
        }
    }

    private fun loadRatingsAndScores() {
        viewModelScope.launch {
            // Load self-ratings
            launch {
                sessionRepository.observeSelfRatings(sessionId)
                    .catch { /* ignore */ }
                    .collectLatest { ratings ->
                        _uiState.update { it.copy(selfRatings = ratings) }
                    }
            }

            // Load partner ratings
            launch {
                sessionRepository.observePartnerRatings(sessionId)
                    .catch { /* ignore */ }
                    .collectLatest { ratings ->
                        _uiState.update { it.copy(partnerRatings = ratings) }
                    }
            }

            // Load set scores
            launch {
                sessionRepository.observeSetScores(sessionId)
                    .catch { /* ignore */ }
                    .collectLatest { scores ->
                        _uiState.update { it.copy(setScores = scores) }
                    }
            }
        }
    }

    private suspend fun loadOpponentAndPartner(session: com.ashutosh.mindfultennis.domain.model.Session) {
        session.opponent1Id?.let { id ->
            opponentRepository.getById(id)?.let { opp ->
                _uiState.update { it.copy(opponent1 = opp) }
            }
        }
        session.opponent2Id?.let { id ->
            opponentRepository.getById(id)?.let { opp ->
                _uiState.update { it.copy(opponent2 = opp) }
            }
        }
        session.partnerId?.let { id ->
            partnerRepository.getById(id)?.let { partner ->
                _uiState.update { it.copy(partner = partner) }
            }
        }
    }
}
