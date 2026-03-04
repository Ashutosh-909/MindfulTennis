package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.FocusPointRepository
import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.domain.model.FocusPoint
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.util.generateId
import java.util.TimeZone
import javax.inject.Inject

/**
 * Creates a new active session in Room (isActive = true) and pushes to Supabase.
 * Also saves the focus note as a reusable focus point if non-blank.
 */
class StartSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val focusPointRepository: FocusPointRepository,
) {
    suspend operator fun invoke(focusNote: String): Result<Session> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(IllegalStateException("User not authenticated"))

        // Check for existing active session
        val existing = sessionRepository.getActiveSession(userId)
        if (existing != null) {
            return Result.failure(IllegalStateException("An active session already exists"))
        }

        val now = System.currentTimeMillis()
        val session = Session(
            id = generateId(),
            userId = userId,
            focusNote = focusNote.trim(),
            startedAt = now,
            timeZoneId = TimeZone.getDefault().id,
            isActive = true,
            createdAt = now,
            updatedAt = now,
        )

        val result = sessionRepository.createSession(session)

        // Save focus note as a reusable focus point if non-blank
        if (result.isSuccess && focusNote.isNotBlank()) {
            val focusPoint = FocusPoint(
                id = generateId(),
                userId = userId,
                text = focusNote.trim(),
                createdAt = now,
            )
            focusPointRepository.create(focusPoint)
        }

        return result
    }
}
