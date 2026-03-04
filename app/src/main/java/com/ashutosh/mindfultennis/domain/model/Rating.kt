package com.ashutosh.mindfultennis.domain.model

/**
 * Domain model for a self-rating or partner-rating on a single aspect.
 */
data class Rating(
    val id: String,
    val sessionId: String,
    val aspect: Aspect,
    val rating: Int, // 1–5
)
