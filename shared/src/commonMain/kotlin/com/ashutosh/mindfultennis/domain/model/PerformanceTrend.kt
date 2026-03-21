package com.ashutosh.mindfultennis.domain.model

/**
 * Represents a single data point in the performance trend chart.
 */
data class PerformanceTrend(
    val sessionId: String,
    val date: Long, // epoch ms
    val overallScore: Int, // 0–100
)
