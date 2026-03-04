package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.PerformanceTrend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Retrieves performance trend data points for the dashboard chart.
 * Each data point maps a completed session's date to its overall score (0–100).
 * Sessions without an overall score (unrated) are excluded.
 */
class GetPerformanceTrendUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {

    operator fun invoke(
        userId: String,
        durationFilter: DurationFilter,
    ): Flow<List<PerformanceTrend>> {
        val fromMs = durationFilter.startEpochMs()
        val toMs = System.currentTimeMillis()
        return sessionRepository.observeSessionsInRange(userId, fromMs, toMs).map { sessions ->
            sessions
                .filter { it.overallScore != null && !it.isActive }
                .sortedBy { it.startedAt }
                .map { session ->
                    PerformanceTrend(
                        sessionId = session.id,
                        date = session.startedAt,
                        overallScore = session.overallScore!!,
                    )
                }
        }
    }
}
