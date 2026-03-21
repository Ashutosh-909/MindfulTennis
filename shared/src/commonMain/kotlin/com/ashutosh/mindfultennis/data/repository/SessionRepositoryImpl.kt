package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.data.local.db.dao.PartnerRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SelfRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SessionDao
import com.ashutosh.mindfultennis.data.local.db.dao.SetScoreDao
import com.ashutosh.mindfultennis.data.local.db.entity.SyncStatus
import com.ashutosh.mindfultennis.data.local.db.entity.toDomain
import com.ashutosh.mindfultennis.data.local.db.entity.toDto
import com.ashutosh.mindfultennis.data.local.db.entity.toEntity
import com.ashutosh.mindfultennis.data.local.db.entity.toPartnerRatingEntity
import com.ashutosh.mindfultennis.data.local.db.entity.toSelfRatingEntity
import com.ashutosh.mindfultennis.data.remote.SupabaseSessionDataSource
import com.ashutosh.mindfultennis.domain.model.Rating
import com.ashutosh.mindfultennis.domain.model.Session
import com.ashutosh.mindfultennis.domain.model.SetScore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SessionRepositoryImpl(
    private val sessionDao: SessionDao,
    private val selfRatingDao: SelfRatingDao,
    private val partnerRatingDao: PartnerRatingDao,
    private val setScoreDao: SetScoreDao,
    private val remoteDataSource: SupabaseSessionDataSource,
) : SessionRepository {

    // ── Session CRUD ──────────────────────────────────────────────────

    override suspend fun createSession(session: Session): Result<Session> = withContext(Dispatchers.Default) {
        runCatching {
            val entity = session.toEntity(SyncStatus.PENDING)
            sessionDao.upsert(entity)
            // Best-effort push to Supabase
            try {
                remoteDataSource.upsertSession(entity.toDto())
                sessionDao.updateSyncStatus(session.id, SyncStatus.SYNCED.name)
            } catch (_: Exception) {
                // Will be synced later by SyncWorker
            }
            session
        }
    }

    override suspend fun updateSession(session: Session): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val entity = session.toEntity(SyncStatus.PENDING)
            sessionDao.upsert(entity)
            try {
                remoteDataSource.upsertSession(entity.toDto())
                sessionDao.updateSyncStatus(session.id, SyncStatus.SYNCED.name)
            } catch (_: Exception) {
                // Will be synced later
            }
        }
    }

    override suspend fun getSession(sessionId: String): Result<Session?> = withContext(Dispatchers.Default) {
        runCatching {
            sessionDao.getById(sessionId)?.toDomain()
        }
    }

    override fun observeSession(sessionId: String): Flow<Session?> =
        sessionDao.observeById(sessionId).map { it?.toDomain() }

    override fun observeAllSessions(userId: String): Flow<List<Session>> =
        sessionDao.observeAllForUser(userId).map { list -> list.map { it.toDomain() } }

    override fun observeSessionsInRange(
        userId: String,
        fromMs: Long,
        toMs: Long,
    ): Flow<List<Session>> =
        sessionDao.observeSessionsInRange(userId, fromMs, toMs).map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveSession(userId: String): Session? = withContext(Dispatchers.Default) {
        sessionDao.getActiveSession(userId)?.toDomain()
    }

    override fun observeActiveSession(userId: String): Flow<Session?> =
        sessionDao.observeActiveSession(userId).map { it?.toDomain() }

    override suspend fun endSession(
        sessionId: String,
        endedAt: Long,
        notes: String?,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val existing = sessionDao.getById(sessionId)
                ?: throw IllegalStateException("Session $sessionId not found")
            val updated = existing.copy(
                endedAt = endedAt,
                notes = notes ?: existing.notes,
                isActive = false,
                updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
                syncStatus = SyncStatus.PENDING.name,
            )
            sessionDao.upsert(updated)
            try {
                remoteDataSource.upsertSession(updated.toDto())
                sessionDao.updateSyncStatus(sessionId, SyncStatus.SYNCED.name)
            } catch (_: Exception) {
                // Will be synced later
            }
        }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            sessionDao.deleteById(sessionId)
            try {
                remoteDataSource.deleteSession(sessionId)
            } catch (_: Exception) {
                // Best-effort remote delete
            }
        }
    }

    // ── Self Ratings ──────────────────────────────────────────────────

    override suspend fun saveSelfRatings(
        sessionId: String,
        ratings: List<Rating>,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val entities = ratings.map { it.toSelfRatingEntity(SyncStatus.PENDING) }
            selfRatingDao.deleteForSession(sessionId)
            selfRatingDao.upsertAll(entities)
            try {
                remoteDataSource.deleteSelfRatingsForSession(sessionId)
                remoteDataSource.upsertSelfRatings(entities.map { it.toDto() })
                selfRatingDao.updateSyncStatusForSession(sessionId, SyncStatus.SYNCED.name)
            } catch (_: Exception) {
                // Will be synced later
            }
        }
    }

    override suspend fun getSelfRatings(sessionId: String): Result<List<Rating>> =
        withContext(Dispatchers.Default) {
            runCatching {
                selfRatingDao.getForSession(sessionId).map { it.toDomain() }
            }
        }

    override fun observeSelfRatings(sessionId: String): Flow<List<Rating>> =
        selfRatingDao.observeForSession(sessionId).map { list -> list.map { it.toDomain() } }

    // ── Partner Ratings ───────────────────────────────────────────────

    override suspend fun savePartnerRatings(
        sessionId: String,
        ratings: List<Rating>,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val entities = ratings.map { it.toPartnerRatingEntity(SyncStatus.PENDING) }
            partnerRatingDao.deleteForSession(sessionId)
            partnerRatingDao.upsertAll(entities)
            try {
                remoteDataSource.deletePartnerRatingsForSession(sessionId)
                remoteDataSource.upsertPartnerRatings(entities.map { it.toDto() })
                partnerRatingDao.updateSyncStatusForSession(sessionId, SyncStatus.SYNCED.name)
            } catch (_: Exception) {
                // Will be synced later
            }
        }
    }

    override suspend fun getPartnerRatings(sessionId: String): Result<List<Rating>> =
        withContext(Dispatchers.Default) {
            runCatching {
                partnerRatingDao.getForSession(sessionId).map { it.toDomain() }
            }
        }

    override fun observePartnerRatings(sessionId: String): Flow<List<Rating>> =
        partnerRatingDao.observeForSession(sessionId).map { list -> list.map { it.toDomain() } }

    // ── Set Scores ────────────────────────────────────────────────────

    override suspend fun saveSetScores(
        sessionId: String,
        scores: List<SetScore>,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val entities = scores.map { it.toEntity(SyncStatus.PENDING) }
            setScoreDao.deleteForSession(sessionId)
            setScoreDao.upsertAll(entities)
            try {
                remoteDataSource.deleteSetScoresForSession(sessionId)
                remoteDataSource.upsertSetScores(entities.map { it.toDto() })
                setScoreDao.updateSyncStatusForSession(sessionId, SyncStatus.SYNCED.name)
            } catch (_: Exception) {
                // Will be synced later
            }
        }
    }

    override suspend fun getSetScores(sessionId: String): Result<List<SetScore>> =
        withContext(Dispatchers.Default) {
            runCatching {
                setScoreDao.getForSession(sessionId).map { it.toDomain() }
            }
        }

    override fun observeSetScores(sessionId: String): Flow<List<SetScore>> =
        setScoreDao.observeForSession(sessionId).map { list -> list.map { it.toDomain() } }

    // ── Batch queries (for dashboard) ─────────────────────────────────

    override suspend fun getSelfRatingsForSessions(
        sessionIds: List<String>,
    ): Result<List<Rating>> = withContext(Dispatchers.Default) {
        runCatching {
            if (sessionIds.isEmpty()) return@runCatching emptyList()
            selfRatingDao.getForSessions(sessionIds).map { it.toDomain() }
        }
    }

    override suspend fun getPartnerRatingsForSessions(
        sessionIds: List<String>,
    ): Result<List<Rating>> = withContext(Dispatchers.Default) {
        runCatching {
            if (sessionIds.isEmpty()) return@runCatching emptyList()
            partnerRatingDao.getForSessions(sessionIds).map { it.toDomain() }
        }
    }

    override suspend fun getSetScoresForSessions(
        sessionIds: List<String>,
    ): Result<List<SetScore>> = withContext(Dispatchers.Default) {
        runCatching {
            if (sessionIds.isEmpty()) return@runCatching emptyList()
            setScoreDao.getForSessions(sessionIds).map { it.toDomain() }
        }
    }

    // ── Bulk / Cleanup ────────────────────────────────────────────────

    override suspend fun deleteAllForUser(userId: String) = withContext(Dispatchers.Default) {
        sessionDao.deleteAllForUser(userId)
    }
}
