package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SessionRepository
import kotlinx.datetime.Clock

/**
 * Ends an active session by setting endedAt, isActive = false, and optional notes.
 * The session must exist and be active.
 */
class EndSessionUseCase(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        notes: String? = null,
    ): Result<Unit> {
        val endedAt = Clock.System.now().toEpochMilliseconds()
        return sessionRepository.endSession(sessionId, endedAt, notes)
    }
}
