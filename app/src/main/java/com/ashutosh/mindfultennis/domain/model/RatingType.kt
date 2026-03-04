package com.ashutosh.mindfultennis.domain.model

/**
 * Specifies which type of ratings to aggregate for aspect performance.
 * PARTNER refers to your partner's feedback on YOUR game, not rating the partner.
 */
enum class RatingType(val label: String) {
    SELF("Self"),
    PARTNER("Partner's Feedback"),
    BOTH("Both"),
}
