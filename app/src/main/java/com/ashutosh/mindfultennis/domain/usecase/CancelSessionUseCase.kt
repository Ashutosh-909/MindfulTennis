package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SessionRepository
import javax.inject.Inject

/**
 * Cancels (deletes) an active session entirely.
 * This removes the session and all related data (ratings, scores) from Room,
 * and attempts a best-effort remote delete.
 */
class CancelSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> {
        return sessionRepository.deleteSession(sessionId)
    }
}
