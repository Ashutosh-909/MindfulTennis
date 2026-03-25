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
import com.ashutosh.mindfultennis.data.local.db.entity.toEntity
import com.ashutosh.mindfultennis.data.remote.SupabaseSessionDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performs a full pull of ALL user data from Supabase into Room on first login per device.
 * After a successful pull, sets [UserPreferences.hasCompletedInitialSync] to true.
 */
@Singleton
class InitialSyncManager @Inject constructor(
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
     * Pulls ALL data for [userId] from Supabase into Room.
     * Marks all inserted rows as SYNCED.
     * Sets hasCompletedInitialSync = true on success.
     */
    suspend fun fullPull(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "Starting full pull for user $userId")

            // 1. Pull focus points, opponents, partners (no FK dependencies)
            pullFocusPoints(userId)
            pullOpponents(userId)
            pullPartners(userId)

            // 2. Pull all sessions
            val sessions = remoteDataSource.getSessionsForUser(userId)
            Log.d(TAG, "Pulled ${sessions.size} sessions")
            for (dto in sessions) {
                sessionDao.upsert(dto.toEntity(SyncStatus.SYNCED))
            }

            // 3. Pull ratings and scores for all sessions
            val sessionIds = sessions.map { it.id }
            if (sessionIds.isNotEmpty()) {
                pullRatingsAndScores(sessionIds)
            }

            // 4. Mark initial sync as complete
            userPreferences.setHasCompletedInitialSync(true)
            userPreferences.setLastSyncTimestamp(System.currentTimeMillis())
            Log.d(TAG, "Full pull completed for user $userId")
            Unit
        }
    }

    private suspend fun pullFocusPoints(userId: String) {
        val remote = remoteDataSource.getFocusPointsForUser(userId)
        Log.d(TAG, "Pulled ${remote.size} focus points")
        for (dto in remote) {
            focusPointDao.upsert(dto.toEntity(SyncStatus.SYNCED))
        }
    }

    private suspend fun pullOpponents(userId: String) {
        val remote = remoteDataSource.getOpponentsForUser(userId)
        Log.d(TAG, "Pulled ${remote.size} opponents")
        for (dto in remote) {
            opponentDao.upsert(dto.toEntity(SyncStatus.SYNCED))
        }
    }

    private suspend fun pullPartners(userId: String) {
        val remote = remoteDataSource.getPartnersForUser(userId)
        Log.d(TAG, "Pulled ${remote.size} partners")
        for (dto in remote) {
            partnerDao.upsert(dto.toEntity(SyncStatus.SYNCED))
        }
    }

    private suspend fun pullRatingsAndScores(sessionIds: List<String>) {
        val selfRatings = remoteDataSource.getSelfRatingsForSessions(sessionIds)
        Log.d(TAG, "Pulled ${selfRatings.size} self ratings")
        for (dto in selfRatings) {
            selfRatingDao.upsert(dto.toEntity(SyncStatus.SYNCED))
        }

        val partnerRatings = remoteDataSource.getPartnerRatingsForSessions(sessionIds)
        Log.d(TAG, "Pulled ${partnerRatings.size} partner ratings")
        for (dto in partnerRatings) {
            partnerRatingDao.upsert(dto.toEntity(SyncStatus.SYNCED))
        }

        val setScores = remoteDataSource.getSetScoresForSessions(sessionIds)
        Log.d(TAG, "Pulled ${setScores.size} set scores")
        for (dto in setScores) {
            setScoreDao.upsert(dto.toEntity(SyncStatus.SYNCED))
        }
    }
}
