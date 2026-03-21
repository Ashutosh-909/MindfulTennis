package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SessionRepository

/**
 * Cancels (deletes) an active session entirely.
 * This removes the session and all related data (ratings, scores) from Room,
 * and attempts a best-effort remote delete.
 */
class CancelSessionUseCase(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> {
        return sessionRepository.deleteSession(sessionId)
    }
}
