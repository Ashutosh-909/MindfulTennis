package com.ashutosh.mindfultennis.data.sync

import android.util.Log
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.local.db.dao.FocusPointDao
import com.ashutosh.mindfultennis.data.local.db.dao.OpponentDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SelfRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SessionDao
import com.ashutosh.mindfultennis.data.local.db.dao.SetScoreDao
import com.ashutosh.mindfultennis.data.local.db.entity.SyncStatus
import com.ashutosh.mindfultennis.data.local.db.entity.toDto
import com.ashutosh.mindfultennis.data.local.db.entity.toEntity
import com.ashutosh.mindfultennis.data.remote.SupabaseSessionDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages bidirectional sync between Room (local) and Supabase (remote).
 *
 * Strategy:
 * 1. **Push**: Find all local rows with syncStatus = PENDING, push them to Supabase, mark SYNCED.
 * 2. **Pull**: Fetch all remote rows updated after lastSyncTimestamp, merge into Room (LWW by updatedAt).
 * 3. Update lastSyncTimestamp after successful sync.
 */
@Singleton
class SyncManager @Inject constructor(
    private val sessionDao: SessionDao,
    private val selfRatingDao: SelfRatingDao,
    private val partnerRatingDao: PartnerRatingDao,
    private val setScoreDao: SetScoreDao,
    private val focusPointDao: FocusPointDao,
    private val opponentDao: OpponentDao,
    private val partnerDao: PartnerDao,
    private val remoteDataSource: SupabaseSessionDataSource,
    private val userPreferences: UserPreferences,
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    /**
     * Runs a full sync cycle: push pending local changes, then pull remote updates.
     * @param userId The current user's ID.
     * @return Result indicating success or the first failure encountered.
     */
    suspend fun sync(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Starting sync for user $userId")

            // Push entities with no FK dependencies first
            pushPendingFocusPoints()
            pushPendingOpponents()
            pushPendingPartners()

            // Sessions depend on opponents + partners via FK
            pushPendingSessions()

            // Ratings and scores depend on sessions (and set_scores on opponents) via FK
            pushPendingSelfRatings()
            pushPendingPartnerRatings()
            pushPendingSetScores()

            val lastSync = userPreferences.lastSyncTimestamp.first()
            pullRemoteSessions(userId, lastSync)
            pullRemoteFocusPoints(userId)
            pullRemoteOpponents(userId)
            pullRemotePartners(userId)

            userPreferences.setLastSyncTimestamp(System.currentTimeMillis())
            Log.d(TAG, "Sync completed for user $userId")
            Unit
        }
    }

    // ── Push: Sessions ────────────────────────────────────────────────

    private suspend fun pushPendingSessions() {
        val pending = sessionDao.getBySyncStatus(SyncStatus.PENDING.name)
        for (session in pending) {
            try {
                remoteDataSource.upsertSession(session.toDto())
                sessionDao.updateSyncStatus(session.id, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push session ${session.id}", e)
            }
        }
    }

    // ── Push: Self Ratings ────────────────────────────────────────────

    private suspend fun pushPendingSelfRatings() {
        val pending = selfRatingDao.getBySyncStatus(SyncStatus.PENDING.name)
        if (pending.isEmpty()) return
        // Group by sessionId and push per session
        val grouped = pending.groupBy { it.sessionId }
        for ((sessionId, ratings) in grouped) {
            try {
                remoteDataSource.deleteSelfRatingsForSession(sessionId)
                remoteDataSource.upsertSelfRatings(ratings.map { it.toDto() })
                selfRatingDao.updateSyncStatusForSession(sessionId, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push self ratings for session $sessionId", e)
            }
        }
    }

    // ── Push: Partner Ratings ─────────────────────────────────────────

    private suspend fun pushPendingPartnerRatings() {
        val pending = partnerRatingDao.getBySyncStatus(SyncStatus.PENDING.name)
        if (pending.isEmpty()) return
        val grouped = pending.groupBy { it.sessionId }
        for ((sessionId, ratings) in grouped) {
            try {
                remoteDataSource.deletePartnerRatingsForSession(sessionId)
                remoteDataSource.upsertPartnerRatings(ratings.map { it.toDto() })
                partnerRatingDao.updateSyncStatusForSession(sessionId, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push partner ratings for session $sessionId", e)
            }
        }
    }

    // ── Push: Set Scores ──────────────────────────────────────────────

    private suspend fun pushPendingSetScores() {
        val pending = setScoreDao.getBySyncStatus(SyncStatus.PENDING.name)
        if (pending.isEmpty()) return
        val grouped = pending.groupBy { it.sessionId }
        for ((sessionId, scores) in grouped) {
            try {
                remoteDataSource.deleteSetScoresForSession(sessionId)
                remoteDataSource.upsertSetScores(scores.map { it.toDto() })
                setScoreDao.updateSyncStatusForSession(sessionId, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push set scores for session $sessionId", e)
            }
        }
    }

    // ── Push: Focus Points ────────────────────────────────────────────

    private suspend fun pushPendingFocusPoints() {
        val pending = focusPointDao.getBySyncStatus(SyncStatus.PENDING.name)
        for (fp in pending) {
            try {
                remoteDataSource.upsertFocusPoint(fp.toDto())
                focusPointDao.updateSyncStatus(fp.id, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push focus point ${fp.id}", e)
            }
        }
    }

    // ── Push: Opponents ───────────────────────────────────────────────

    private suspend fun pushPendingOpponents() {
        val pending = opponentDao.getBySyncStatus(SyncStatus.PENDING.name)
        for (opp in pending) {
            try {
                remoteDataSource.upsertOpponent(opp.toDto())
                opponentDao.updateSyncStatus(opp.id, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push opponent ${opp.id}", e)
            }
        }
    }

    // ── Push: Partners ────────────────────────────────────────────────

    private suspend fun pushPendingPartners() {
        val pending = partnerDao.getBySyncStatus(SyncStatus.PENDING.name)
        for (p in pending) {
            try {
                remoteDataSource.upsertPartner(p.toDto())
                partnerDao.updateSyncStatus(p.id, SyncStatus.SYNCED.name)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to push partner ${p.id}", e)
            }
        }
    }

    // ── Pull: Sessions (LWW merge) ───────────────────────────────────

    private suspend fun pullRemoteSessions(userId: String, lastSyncMs: Long) {
        try {
            val remoteSessions = remoteDataSource.getSessionsUpdatedAfter(userId, lastSyncMs)

            // Upsert newly updated sessions (LWW merge)
            val newSessionIds = mutableListOf<String>()
            for (remoteDto in remoteSessions) {
                val local = sessionDao.getById(remoteDto.id)
                if (local == null || remoteDto.updatedAt > local.updatedAt) {
                    sessionDao.upsert(remoteDto.toEntity(SyncStatus.SYNCED))
                }
                newSessionIds.add(remoteDto.id)
            }

            // Also find sessions in Room that are missing ratings/scores
            // (self-healing for previously failed syncs)
            val incompleteIds = sessionDao.getSessionIdsWithoutRatings(userId)
            val allIds = (newSessionIds + incompleteIds).distinct()

            if (allIds.isNotEmpty()) {
                Log.d(TAG, "Pulling ratings/scores for ${allIds.size} sessions " +
                    "(${newSessionIds.size} new, ${incompleteIds.size} incomplete)")
                pullRatingsAndScoresBulk(allIds)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pull remote sessions", e)
        }
    }

    private suspend fun pullRatingsAndScoresBulk(sessionIds: List<String>) {
        try {
            val allSelfRatings = remoteDataSource.getSelfRatingsForSessions(sessionIds)
            if (allSelfRatings.isNotEmpty()) {
                val grouped = allSelfRatings.groupBy { it.sessionId }
                for ((sessionId, ratings) in grouped) {
                    selfRatingDao.deleteForSession(sessionId)
                    selfRatingDao.upsertAll(ratings.map { it.toEntity(SyncStatus.SYNCED) })
                }
            }

            val allPartnerRatings = remoteDataSource.getPartnerRatingsForSessions(sessionIds)
            if (allPartnerRatings.isNotEmpty()) {
                val grouped = allPartnerRatings.groupBy { it.sessionId }
                for ((sessionId, ratings) in grouped) {
                    partnerRatingDao.deleteForSession(sessionId)
                    partnerRatingDao.upsertAll(ratings.map { it.toEntity(SyncStatus.SYNCED) })
                }
            }

            val allSetScores = remoteDataSource.getSetScoresForSessions(sessionIds)
            if (allSetScores.isNotEmpty()) {
                val grouped = allSetScores.groupBy { it.sessionId }
                for ((sessionId, scores) in grouped) {
                    setScoreDao.deleteForSession(sessionId)
                    setScoreDao.upsertAll(scores.map { it.toEntity(SyncStatus.SYNCED) })
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pull ratings/scores in bulk", e)
        }
    }

    // ── Pull: Focus Points ────────────────────────────────────────────

    private suspend fun pullRemoteFocusPoints(userId: String) {
        try {
            val remote = remoteDataSource.getFocusPointsForUser(userId)
            for (dto in remote) {
                val local = focusPointDao.getById(dto.id)
                if (local == null || local.syncStatus != SyncStatus.PENDING.name) {
                    // Accept remote if no pending local change
                    focusPointDao.upsert(dto.toEntity(SyncStatus.SYNCED))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pull remote focus points", e)
        }
    }

    // ── Pull: Opponents ───────────────────────────────────────────────

    private suspend fun pullRemoteOpponents(userId: String) {
        try {
            val remote = remoteDataSource.getOpponentsForUser(userId)
            for (dto in remote) {
                val local = opponentDao.getById(dto.id)
                if (local == null || local.syncStatus != SyncStatus.PENDING.name) {
                    opponentDao.upsert(dto.toEntity(SyncStatus.SYNCED))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pull remote opponents", e)
        }
    }

    // ── Pull: Partners ────────────────────────────────────────────────

    private suspend fun pullRemotePartners(userId: String) {
        try {
            val remote = remoteDataSource.getPartnersForUser(userId)
            for (dto in remote) {
                val local = partnerDao.getById(dto.id)
                if (local == null || local.syncStatus != SyncStatus.PENDING.name) {
                    partnerDao.upsert(dto.toEntity(SyncStatus.SYNCED))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pull remote partners", e)
        }
    }
}
