package com.ashutosh.mindfultennis.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for the `set_scores` table.
 */
@Serializable
data class SetScoreDto(
    @SerialName("id") val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("set_number") val setNumber: Int,
    @SerialName("user_score") val userScore: Int,
    @SerialName("opponent_score") val opponentScore: Int,
)
