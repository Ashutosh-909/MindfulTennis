package com.ashutosh.mindfultennis.domain.usecase

import com.ashutosh.mindfultennis.data.repository.SessionRepository
import com.ashutosh.mindfultennis.domain.model.DurationFilter
import com.ashutosh.mindfultennis.domain.model.GameResult
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.WinLossRecord
import com.ashutosh.mindfultennis.util.ScoreCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Computes the win/loss record for sessions within a date range,
 * optionally filtered by opponent IDs.
 * A session is a "win" if the user won a majority of recorded sets.
 * Sessions without set scores are excluded.
 */
class GetWinLossRecordUseCase(
    private val sessionRepository: SessionRepository,
) {

    operator fun invoke(
        userId: String,
        durationFilter: DurationFilter,
        opponentIds: Set<String> = emptySet(),
    ): Flow<WinLossRecord> {
        val fromMs = durationFilter.startEpochMs()
        val toMs = Clock.System.now().toEpochMilliseconds()
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
        var draws = 0

        // Ordered list of (session, result) — chronological
        val orderedResults = mutableListOf<GameResult>()

        // Use sessions sorted by startedAt so recentResults is chronological
        val sorted = sessions.sortedBy { it.startedAt }

        for (session in sorted) {
            val scores = scoresBySession[session.id] ?: continue
            if (scores.isEmpty()) continue

            val userSetsWon = scores.count { it.userScore > it.opponentScore }
            val opponentSetsWon = scores.count { it.opponentScore > it.userScore }

            val result = when {
                userSetsWon > opponentSetsWon -> GameResult.WIN
                opponentSetsWon > userSetsWon -> GameResult.LOSS
                else -> GameResult.DRAW
            }

            when (result) {
                GameResult.WIN -> wins++
                GameResult.LOSS -> losses++
                GameResult.DRAW -> draws++
            }
            orderedResults.add(result)
        }

        return WinLossRecord(
            wins = wins,
            losses = losses,
            draws = draws,
            recentResults = orderedResults.takeLast(RECENT_GAMES_COUNT),
        )
    }

    companion object {
        /** Maximum number of recent game results shown in the domino strip. */
        const val RECENT_GAMES_COUNT = 5
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
