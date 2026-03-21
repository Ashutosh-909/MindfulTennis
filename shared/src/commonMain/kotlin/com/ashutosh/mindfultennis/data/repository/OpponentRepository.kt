package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.domain.model.Opponent
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for opponent operations.
 */
interface OpponentRepository {

    fun observeAll(userId: String): Flow<List<Opponent>>

    suspend fun getAll(userId: String): List<Opponent>

    suspend fun getById(id: String): Opponent?

    suspend fun create(opponent: Opponent): Result<Unit>

    suspend fun delete(opponentId: String): Result<Unit>

    suspend fun deleteAllForUser(userId: String)
}
