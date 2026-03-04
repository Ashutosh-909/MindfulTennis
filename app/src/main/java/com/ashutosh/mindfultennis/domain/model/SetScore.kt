package com.ashutosh.mindfultennis.domain.model

/**
 * Domain model for set scores within a session.
 */
data class SetScore(
    val id: String,
    val sessionId: String,
    val setNumber: Int,
    val userScore: Int,
    val opponentScore: Int,
)
