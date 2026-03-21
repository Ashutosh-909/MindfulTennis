package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.domain.model.Partner
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for partner operations.
 */
interface PartnerRepository {

    fun observeAll(userId: String): Flow<List<Partner>>

    suspend fun getAll(userId: String): List<Partner>

    suspend fun getById(id: String): Partner?

    suspend fun create(partner: Partner): Result<Unit>

    suspend fun delete(partnerId: String): Result<Unit>

    suspend fun deleteAllForUser(userId: String)
}
