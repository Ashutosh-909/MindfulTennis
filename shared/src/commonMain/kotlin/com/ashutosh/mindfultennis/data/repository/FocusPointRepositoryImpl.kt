package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.data.local.db.dao.FocusPointDao
import com.ashutosh.mindfultennis.data.local.db.dao.SessionDao
import com.ashutosh.mindfultennis.data.local.db.entity.SyncStatus
import com.ashutosh.mindfultennis.data.local.db.entity.toDomain
import com.ashutosh.mindfultennis.data.local.db.entity.toDto
import com.ashutosh.mindfultennis.data.local.db.entity.toEntity
import com.ashutosh.mindfultennis.data.remote.SupabaseSessionDataSource
import com.ashutosh.mindfultennis.domain.model.FocusPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FocusPointRepositoryImpl(
    private val focusPointDao: FocusPointDao,
    private val sessionDao: SessionDao,
    private val remoteDataSource: SupabaseSessionDataSource,
) : FocusPointRepository {

    override fun observeAll(userId: String): Flow<List<FocusPoint>> =
        focusPointDao.observeAllForUser(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(userId: String): List<FocusPoint> = withContext(Dispatchers.Default) {
        focusPointDao.getAllForUser(userId).map { it.toDomain() }
    }

    override suspend fun getById(id: String): FocusPoint? = withContext(Dispatchers.Default) {
        focusPointDao.getById(id)?.toDomain()
    }

    override suspend fun create(focusPoint: FocusPoint): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val entity = focusPoint.toEntity(SyncStatus.PENDING)
            focusPointDao.upsert(entity)
            try {
                remoteDataSource.upsertFocusPoint(entity.toDto())
                focusPointDao.updateSyncStatus(focusPoint.id, SyncStatus.SYNCED.name)
            } catch (_: Exception) {
                // Will be synced later
            }
        }
    }

    override suspend fun delete(focusPointId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            focusPointDao.deleteById(focusPointId)
            try {
                remoteDataSource.deleteFocusPoint(focusPointId)
            } catch (_: Exception) {
                // Best-effort remote delete
            }
        }
    }

    override suspend fun deleteAllForUser(userId: String) = withContext(Dispatchers.Default) {
        focusPointDao.deleteAllForUser(userId)
    }

    override suspend fun getAllWithAverageScore(userId: String): List<FocusPoint> =
        withContext(Dispatchers.Default) {
            val entities = focusPointDao.getAllForUser(userId)
            entities
                .groupBy { it.text.trim().lowercase() }
                .map { (_, dupes) -> dupes.maxBy { it.createdAt } }
                .map { entity ->
                    val avgScore = sessionDao.getAverageScoreForFocusText(userId, entity.text)
                    entity.toDomain().copy(averageScore = avgScore?.toInt())
                }
        }
}
