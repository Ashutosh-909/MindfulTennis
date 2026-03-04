package com.ashutosh.mindfultennis.domain.model

/**
 * Result of a single session: WIN, LOSS, or DRAW.
 */
enum class GameResult { WIN, LOSS, DRAW }

/**
 * Win/Loss record for display on the dashboard.
 * [recentResults] holds the last N game results (most recent last), used for the domino strip.
 */
data class WinLossRecord(
    val wins: Int,
    val losses: Int,
    val draws: Int = 0,
    val recentResults: List<GameResult> = emptyList(),
) {
    val total: Int get() = wins + losses + draws

    val winPercentage: Float
        get() = if (total > 0) (wins.toFloat() / total.toFloat()) * 100f else 0f
}
