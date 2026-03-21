package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.domain.model.Aspect
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.RatingType
import com.ashutosh.mindfultennis.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Computes the average rating for each of the 8 aspects
 * across sessions within a date range, optionally filtered by opponent IDs.
 * Supports self ratings, partner ratings, or both based on [RatingType].
 */
class GetAspectAveragesUseCase(
    private val sessionRepository: SessionRepository,
) {

    operator fun invoke(
        userId: String,
        durationFilter: DurationFilter,
        opponentIds: Set<String> = emptySet(),
        ratingType: RatingType = RatingType.SELF,
    ): Flow<Map<Aspect, Float>> {
        val fromMs = durationFilter.startEpochMs()
        val toMs = Clock.System.now().toEpochMilliseconds()
        return sessionRepository.observeSessionsInRange(userId, fromMs, toMs).map { sessions ->
            val filtered = filterByOpponents(sessions, opponentIds)
                .filter { !it.isActive }
            computeAverages(filtered, ratingType)
        }
    }

    private suspend fun computeAverages(
        sessions: List<Session>,
        ratingType: RatingType,
    ): Map<Aspect, Float> {
        if (sessions.isEmpty()) return emptyMap()

        val sessionIds = sessions.map { it.id }

        val selfRatings = if (ratingType == RatingType.SELF || ratingType == RatingType.BOTH) {
            sessionRepository.getSelfRatingsForSessions(sessionIds).getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val partnerRatings = if (ratingType == RatingType.PARTNER || ratingType == RatingType.BOTH) {
            sessionRepository.getPartnerRatingsForSessions(sessionIds).getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val allRatings = selfRatings + partnerRatings
        if (allRatings.isEmpty()) return emptyMap()

        return allRatings
            .groupBy { it.aspect }
            .mapValues { (_, ratings) ->
                ratings.map { it.rating.toFloat() }.average().toFloat()
            }
    }

    private fun filterByOpponents(
        sessions: List<Session>,
        opponentIds: Set<String>,
    ): List<Session> {
        if (opponentIds.isEmpty()) return sessions
        return sessions.filter { session ->
            val sessionOpponentIds = listOfNotNull(session.opponent1Id, session.opponent2Id)
            sessionOpponentIds.any { it in opponentIds }
        }
    }
}
