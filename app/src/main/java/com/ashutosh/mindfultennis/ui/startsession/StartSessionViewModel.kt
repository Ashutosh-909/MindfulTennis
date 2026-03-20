package com.ashutosh.mindfultennis.ui.startsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import com.ashutosh.mindfultennis.data.repository.FocusPointRepository
import com.ashutosh.mindfultennis.domain.usecase.StartSessionUseCase
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
class StartSessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val focusPointRepository: FocusPointRepository,
    private val startSessionUseCase: StartSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StartSessionUiState())
    val uiState: StateFlow<StartSessionUiState> = _uiState.asStateFlow()

    init {
        loadRecentFocusPoints()
    }

    fun onEvent(event: StartSessionUiEvent) {
        when (event) {
            is StartSessionUiEvent.FocusNoteChanged -> {
                _uiState.update { it.copy(focusNote = event.text.take(MAX_FOCUS_NOTE_LENGTH)) }
            }
            is StartSessionUiEvent.FocusPointChipClicked -> {
                val current = _uiState.value.focusNote
                val newNote = if (current.isBlank()) {
                    event.text
                } else {
                    "$current, ${event.text}"
                }.take(MAX_FOCUS_NOTE_LENGTH)
                _uiState.update { it.copy(focusNote = newNote) }
            }
            is StartSessionUiEvent.StartSessionClicked -> startSession()
            is StartSessionUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun loadRecentFocusPoints() {
        viewModelScope.launch {
            authRepository.authState.collectLatest { authState ->
                if (authState is AuthState.Authenticated) {
                    focusPointRepository.observeAll(authState.userId)
                        .catch { /* ignore */ }
                        .collectLatest { _ ->
                            // Re-compute average scores whenever focus points change
                            val pointsWithScores = focusPointRepository
                                .getAllWithAverageScore(authState.userId)
                            _uiState.update {
                                it.copy(
                                    recentFocusPoints = pointsWithScores
                                        .take(MAX_RECENT_FOCUS_POINTS),
                                )
                            }
                        }
                }
            }
        }
    }

    private fun startSession() {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = startSessionUseCase(_uiState.value.focusNote)
            result.fold(
                onSuccess = { session ->
                    _uiState.update { it.copy(isLoading = false, sessionStarted = true) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to start session",
                        )
                    }
                },
            )
        }
    }

    companion object {
        private const val MAX_FOCUS_NOTE_LENGTH = 500
        private const val MAX_RECENT_FOCUS_POINTS = 10
    }
}
