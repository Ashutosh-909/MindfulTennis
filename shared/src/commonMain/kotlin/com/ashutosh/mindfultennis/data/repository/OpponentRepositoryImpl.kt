package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.data.local.db.dao.OpponentDao
import com.ashutosh.mindfultennis.data.local.db.entity.SyncStatus
import com.ashutosh.mindfultennis.data.local.db.entity.toDomain
import com.ashutosh.mindfultennis.data.local.db.entity.toDto
import com.ashutosh.mindfultennis.data.local.db.entity.toEntity
import com.ashutosh.mindfultennis.data.remote.SupabaseSessionDataSource
import com.ashutosh.mindfultennis.domain.model.Opponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OpponentRepositoryImpl(
    private val opponentDao: OpponentDao,
    private val remoteDataSource: SupabaseSessionDataSource,
) : OpponentRepository {

    override fun observeAll(userId: String): Flow<List<Opponent>> =
        opponentDao.observeAllForUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(userId: String): List<Opponent> = withContext(Dispatchers.Default) {
        opponentDao.getAllForUser(userId).map { it.toDomain() }
    }

    override suspend fun getById(id: String): Opponent? = withContext(Dispatchers.Default) {
        opponentDao.getById(id)?.toDomain()
    }

    override suspend fun create(opponent: Opponent): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val entity = opponent.toEntity(SyncStatus.PENDING)
            opponentDao.upsert(entity)
            try {
                remoteDataSource.upsertOpponent(entity.toDto())
                opponentDao.updateSyncStatus(opponent.id, SyncStatus.SYNCED.name)
            } catch (_: Exception) {
                // Will be synced later
            }
        }
    }

    override suspend fun delete(opponentId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            opponentDao.deleteById(opponentId)
            try {
                remoteDataSource.deleteOpponent(opponentId)
            } catch (_: Exception) {
                // Best-effort remote delete
            }
        }
    }

    override suspend fun deleteAllForUser(userId: String) = withContext(Dispatchers.Default) {
        opponentDao.deleteAllForUser(userId)
    }
}
