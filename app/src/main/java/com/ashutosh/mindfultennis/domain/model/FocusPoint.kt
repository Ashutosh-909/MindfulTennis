package com.ashutosh.mindfultennis.domain.model

/**
 * Domain model for a focus point that the user wants to work on.
 */
data class FocusPoint(
    val id: String,
    val userId: String,
    val text: String,
    val category: String? = null,
    val createdAt: Long,
)
