package com.ashutosh.mindfultennis.data.sync

import co.touchlab.kermit.Logger
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.local.db.dao.FocusPointDao
import com.ashutosh.mindfultennis.data.local.db.dao.OpponentDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SelfRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SessionDao
import com.ashutosh.mindfultennis.data.local.db.dao.SetScoreDao
import com.ashutosh.mindfultennis.data.local.db.entity.SyncStatus
import com.ashutosh.mindfultennis.data.local.db.entity.toEntity
import com.ashutosh.mindfultennis.data.remote.SupabaseSessionDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Performs a one-time initial sync that pulls ALL data from Supabase
 * into the local Room database. This runs on first login (or when the
 * local DB is empty) so the user sees their full history immediately.
 */
class InitialSyncManager(
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
        private const val TAG = "InitialSyncManager"
    }

    /**
     * Pulls ALL data from Supabase for the given user and upserts it
     * into Room with [SyncStatus.SYNCED]. After completion, sets
     * hasCompletedInitialSync = true and updates lastSyncTimestamp.
     */
    suspend fun performInitialSync(userId: String): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            Logger.d(TAG) { "Starting initial sync for user $userId" }

            // Pull standalone entities first (no FK dependencies)
            pullAllFocusPoints(userId)
            pullAllOpponents(userId)
            pullAllPartners(userId)

            // Pull sessions (depend on opponents/partners via FK)
            val sessionIds = pullAllSessions(userId)

            // Pull ratings and scores for all sessions
            if (sessionIds.isNotEmpty()) {
                Logger.d(TAG) { "Pulling ratings/scores for ${sessionIds.size} sessions" }
                pullRatingsAndScoresForSessions(sessionIds)
            }

            // Mark initial sync as complete
            userPreferences.setHasCompletedInitialSync(true)
            userPreferences.setLastSyncTimestamp(
                kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            )

            Logger.d(TAG) { "Initial sync completed for user $userId" }
        }
    }

    private suspend fun pullAllFocusPoints(userId: String) {
        try {
            val remote = remoteDataSource.getFocusPointsForUser(userId)
            Logger.d(TAG) { "Pulled ${remote.size} focus points" }
            for (dto in remote) {
                focusPointDao.upsert(dto.toEntity(SyncStatus.SYNCED))
            }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull focus points" }
            throw e
        }
    }

    private suspend fun pullAllOpponents(userId: String) {
        try {
            val remote = remoteDataSource.getOpponentsForUser(userId)
            Logger.d(TAG) { "Pulled ${remote.size} opponents" }
            for (dto in remote) {
                opponentDao.upsert(dto.toEntity(SyncStatus.SYNCED))
            }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull opponents" }
            throw e
        }
    }

    private suspend fun pullAllPartners(userId: String) {
        try {
            val remote = remoteDataSource.getPartnersForUser(userId)
            Logger.d(TAG) { "Pulled ${remote.size} partners" }
            for (dto in remote) {
                partnerDao.upsert(dto.toEntity(SyncStatus.SYNCED))
            }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull partners" }
            throw e
        }
    }

    private suspend fun pullAllSessions(userId: String): List<String> {
        try {
            val remote = remoteDataSource.getSessionsForUser(userId)
            Logger.d(TAG) { "Pulled ${remote.size} sessions" }
            for (dto in remote) {
                sessionDao.upsert(dto.toEntity(SyncStatus.SYNCED))
            }
            return remote.map { it.id }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull sessions" }
            throw e
        }
    }

    private suspend fun pullRatingsAndScoresForSessions(sessionIds: List<String>) {
        try {
            val allSelfRatings = remoteDataSource.getSelfRatingsForSessions(sessionIds)
            if (allSelfRatings.isNotEmpty()) {
                val grouped = allSelfRatings.groupBy { it.sessionId }
                for ((sessionId, ratings) in grouped) {
                    selfRatingDao.deleteForSession(sessionId)
                    selfRatingDao.upsertAll(ratings.map { it.toEntity(SyncStatus.SYNCED) })
                }
            }
            Logger.d(TAG) { "Pulled ${allSelfRatings.size} self ratings" }

            val allPartnerRatings = remoteDataSource.getPartnerRatingsForSessions(sessionIds)
            if (allPartnerRatings.isNotEmpty()) {
                val grouped = allPartnerRatings.groupBy { it.sessionId }
                for ((sessionId, ratings) in grouped) {
                    partnerRatingDao.deleteForSession(sessionId)
                    partnerRatingDao.upsertAll(ratings.map { it.toEntity(SyncStatus.SYNCED) })
                }
            }
            Logger.d(TAG) { "Pulled ${allPartnerRatings.size} partner ratings" }

            val allSetScores = remoteDataSource.getSetScoresForSessions(sessionIds)
            if (allSetScores.isNotEmpty()) {
                val grouped = allSetScores.groupBy { it.sessionId }
                for ((sessionId, scores) in grouped) {
                    setScoreDao.deleteForSession(sessionId)
                    setScoreDao.upsertAll(scores.map { it.toEntity(SyncStatus.SYNCED) })
                }
            }
            Logger.d(TAG) { "Pulled ${allSetScores.size} set scores" }
        } catch (e: Exception) {
            Logger.w(TAG, e) { "Failed to pull ratings/scores" }
            throw e
        }
    }
}
