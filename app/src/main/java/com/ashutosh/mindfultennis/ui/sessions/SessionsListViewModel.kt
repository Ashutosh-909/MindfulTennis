package com.ashutosh.mindfultennis.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.usecase.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
class SessionsListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getSessionsUseCase: GetSessionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsListUiState())
    val uiState: StateFlow<SessionsListUiState> = _uiState.asStateFlow()

    private var sessionsJob: Job? = null

    init {
        loadSessions()
    }

    fun onEvent(event: SessionsListUiEvent) {
        when (event) {
            is SessionsListUiEvent.DurationChanged -> {
                _uiState.update { it.copy(selectedDuration = event.filter, isLoading = true) }
                loadSessions()
            }
            is SessionsListUiEvent.SessionClicked -> {
                // Handled by the screen composable for navigation
            }
            is SessionsListUiEvent.RetryClicked -> {
                _uiState.update { it.copy(isLoading = true, error = null) }
                loadSessions()
            }
            is SessionsListUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun loadSessions() {
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch {
            val authState = authRepository.authState.first { it !is AuthState.Loading }
            if (authState !is AuthState.Authenticated) {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            getSessionsUseCase(
                userId = authState.userId,
                durationFilter = _uiState.value.selectedDuration,
            )
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load sessions",
                        )
                    }
                }
                .collectLatest { sessions ->
                    _uiState.update {
                        it.copy(
                            sessions = sessions,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }
}
