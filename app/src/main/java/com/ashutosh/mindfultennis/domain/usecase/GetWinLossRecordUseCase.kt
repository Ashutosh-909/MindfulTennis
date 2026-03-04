package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.WinLossRecord
import com.ashutosh.mindfultennis.util.ScoreCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Computes the win/loss record for sessions within a date range,
 * optionally filtered by opponent IDs.
 * A session is a "win" if the user won a majority of recorded sets.
 * Sessions without set scores are excluded.
 */
class GetWinLossRecordUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {

    operator fun invoke(
        userId: String,
        durationFilter: DurationFilter,
        opponentIds: Set<String> = emptySet(),
    ): Flow<WinLossRecord> {
        val fromMs = durationFilter.startEpochMs()
        val toMs = System.currentTimeMillis()
        return sessionRepository.observeSessionsInRange(userId, fromMs, toMs).map { sessions ->
            val filtered = filterByOpponents(sessions, opponentIds)
                .filter { !it.isActive }
            computeWinLoss(filtered)
        }
    }

    private suspend fun computeWinLoss(sessions: List<Session>): WinLossRecord {
        if (sessions.isEmpty()) return WinLossRecord(wins = 0, losses = 0)

        val sessionIds = sessions.map { it.id }
        val allSetScores = sessionRepository.getSetScoresForSessions(sessionIds)
            .getOrDefault(emptyList())

        val scoresBySession = allSetScores.groupBy { it.sessionId }

        var wins = 0
        var losses = 0

        for (session in sessions) {
            val scores = scoresBySession[session.id] ?: continue
            val result = ScoreCalculator.isWin(scores)
            if (result == true) wins++ else if (result == false) losses++
        }

        return WinLossRecord(wins = wins, losses = losses)
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
