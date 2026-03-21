package com.ashutosh.mindfultennis.util

import com.ashutosh.mindfultennis.domain.model.SetScore
import com.ashutosh.mindfultennis.ui.theme.SessionAverage
import com.ashutosh.mindfultennis.ui.theme.SessionGood
import com.ashutosh.mindfultennis.ui.theme.SessionPoor
import com.ashutosh.mindfultennis.ui.theme.SessionUnrated
import androidx.compose.ui.graphics.Color

/**
 * Utility for computing performance scores and win/loss from session data.
 */
object ScoreCalculator {

    /**
     * Computes the overall performance score (0-100) from 8 self-ratings (each 1-5).
     * Formula: ((mean_of_ratings - 1) / 4) * 100
     */
    fun computeOverallScore(ratings: List<Int>): Int {
        if (ratings.isEmpty()) return 0
        val mean = ratings.average()
        return (((mean - 1.0) / 4.0) * 100.0).toInt()
    }

    /**
     * Determines if a session is a win based on set scores.
     * A session is a "win" if the user won a majority of recorded sets.
     * Returns null if no set scores are provided.
     */
    fun isWin(setScores: List<SetScore>): Boolean? {
        if (setScores.isEmpty()) return null
        val userSetsWon = setScores.count { it.userScore > it.opponentScore }
        val totalSets = setScores.size
        return userSetsWon > totalSets / 2
    }

    /**
     * Returns the session color based on overall score.
     */
    fun sessionColor(overallScore: Int?): Color {
        return when {
            overallScore == null -> SessionUnrated
            overallScore >= 70 -> SessionGood
            overallScore >= 40 -> SessionAverage
            else -> SessionPoor
        }
    }

    /**
     * Returns a label for the session score range.
     */
    fun sessionLabel(overallScore: Int?): String {
        return when {
            overallScore == null -> "Unrated"
            overallScore >= 70 -> "Great"
            overallScore >= 40 -> "Average"
            else -> "Needs Work"
        }
    }
}
