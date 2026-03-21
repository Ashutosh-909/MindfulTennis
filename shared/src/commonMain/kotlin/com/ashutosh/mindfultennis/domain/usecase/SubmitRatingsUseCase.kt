package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.domain.model.MatchType
import com.ashutosh.mindfultennis.domain.model.Rating
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.SetScore
import com.ashutosh.mindfultennis.util.ScoreCalculator
import kotlinx.datetime.Clock

/**
 * Submits all ratings for a session:
 * 1. Saves self-ratings to Room + Supabase
 * 2. Saves partner ratings (optional)
 * 3. Saves set scores (optional)
 * 4. Computes overallScore from self-ratings
 * 5. Ends the session (sets endedAt, isActive = false)
 * 6. Updates session with match type, opponent/partner IDs, notes, and overallScore
 */
class SubmitRatingsUseCase(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        selfRatings: List<Rating>,
        partnerRatings: List<Rating>,
        setScores: List<SetScore>,
        notes: String?,
        matchType: MatchType,
        opponent1Id: String?,
        opponent2Id: String?,
        partnerId: String?,
    ): Result<Unit> = runCatching {
        require(selfRatings.isNotEmpty()) { "At least one self-rating is required" }

        // 1. Save self-ratings
        sessionRepository.saveSelfRatings(sessionId, selfRatings).getOrThrow()

        // 2. Save partner ratings (optional)
        if (partnerRatings.isNotEmpty()) {
            sessionRepository.savePartnerRatings(sessionId, partnerRatings).getOrThrow()
        }

        // 3. Save set scores (optional)
        if (setScores.isNotEmpty()) {
            sessionRepository.saveSetScores(sessionId, setScores).getOrThrow()
        }

        // 4. Compute overall score
        val overallScore = ScoreCalculator.computeOverallScore(selfRatings.map { it.rating })

        // 5. End session and update with computed data
        val existingSession = sessionRepository.getSession(sessionId).getOrThrow()
            ?: throw IllegalStateException("Session $sessionId not found")

        val now = Clock.System.now().toEpochMilliseconds()
        val updatedSession = existingSession.copy(
            endedAt = existingSession.endedAt ?: now,
            isActive = false,
            notes = notes,
            overallScore = overallScore,
            matchType = matchType,
            opponent1Id = opponent1Id,
            opponent2Id = opponent2Id,
            partnerId = partnerId,
            updatedAt = now,
        )

        sessionRepository.updateSession(updatedSession).getOrThrow()
    }
}
