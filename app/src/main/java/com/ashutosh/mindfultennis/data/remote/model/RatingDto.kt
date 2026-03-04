package com.ashutosh.mindfultennis.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for the `self_ratings` table.
 */
@Serializable
data class SelfRatingDto(
    @SerialName("id") val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("aspect") val aspect: String,
    @SerialName("rating") val rating: Int,
)

/**
 * Supabase DTO for the `partner_ratings` table.
 */
@Serializable
data class PartnerRatingDto(
    @SerialName("id") val id: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("aspect") val aspect: String,
    @SerialName("rating") val rating: Int,
)
