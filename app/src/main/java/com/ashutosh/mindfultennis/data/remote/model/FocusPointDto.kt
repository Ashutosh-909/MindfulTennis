package com.ashutosh.mindfultennis.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for the `focus_points` table.
 */
@Serializable
data class FocusPointDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("text") val text: String,
    @SerialName("category") val category: String? = null,
    @SerialName("created_at") val createdAt: Long,
)
