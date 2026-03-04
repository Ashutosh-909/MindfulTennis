package com.ashutosh.mindfultennis.domain.model

/**
 * Specifies which type of ratings to aggregate for aspect performance.
 */
enum class RatingType(val label: String) {
    SELF("Self"),
    PARTNER("Partner"),
    BOTH("Both"),
}
