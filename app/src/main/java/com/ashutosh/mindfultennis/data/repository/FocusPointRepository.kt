package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.domain.model.FocusPoint
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for focus point operations.
 */
interface FocusPointRepository {

    fun observeAll(userId: String): Flow<List<FocusPoint>>

    suspend fun getAll(userId: String): List<FocusPoint>

    suspend fun getById(id: String): FocusPoint?

    suspend fun create(focusPoint: FocusPoint): Result<Unit>

    suspend fun delete(focusPointId: String): Result<Unit>

    suspend fun deleteAllForUser(userId: String)

    /**
     * Returns all focus points for the user with their average performance score
     * computed from completed sessions that used each focus point.
     */
    suspend fun getAllWithAverageScore(userId: String): List<FocusPoint>
}
