package com.ashutosh.mindfultennis.domain.model

/**
 * Win/Loss record for display on the dashboard.
 */
data class WinLossRecord(
    val wins: Int,
    val losses: Int,
) {
    val total: Int get() = wins + losses

    val winPercentage: Float
        get() = if (total > 0) (wins.toFloat() / total.toFloat()) * 100f else 0f
}
