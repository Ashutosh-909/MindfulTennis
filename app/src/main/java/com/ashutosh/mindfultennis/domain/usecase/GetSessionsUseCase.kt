package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Retrieves completed sessions for the Sessions List screen,
 * filtered by a duration range and sorted by startedAt descending.
 */
class GetSessionsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {

    operator fun invoke(
        userId: String,
        durationFilter: DurationFilter,
    ): Flow<List<Session>> {
        val fromMs = durationFilter.startEpochMs()
        val toMs = System.currentTimeMillis()
        return sessionRepository.observeSessionsInRange(userId, fromMs, toMs).map { sessions ->
            sessions
                .filter { !it.isActive }
                .sortedByDescending { it.startedAt }
        }
    }

    /**
     * Retrieves all sessions (including active) for a user.
     */
    fun all(userId: String): Flow<List<Session>> {
        return sessionRepository.observeAllSessions(userId).map { sessions ->
            sessions.sortedByDescending { it.startedAt }
        }
    }
}
