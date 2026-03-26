package com.ashutosh.mindfultennis.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.ashutosh.mindfultennis.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // ── Inserts / Updates ─────────────────────────────────────────────

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Upsert
    suspend fun upsertAll(sessions: List<SessionEntity>)

    @Update
    suspend fun update(session: SessionEntity)

    // ── Queries ───────────────────────────────────────────────────────

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeById(sessionId: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND sync_status != 'PENDING_DELETE' ORDER BY started_at DESC")
    fun observeAllForUser(userId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND sync_status != 'PENDING_DELETE' ORDER BY started_at DESC")
    suspend fun getAllForUser(userId: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND is_active = 1 AND sync_status != 'PENDING_DELETE' LIMIT 1")
    suspend fun getActiveSession(userId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND is_active = 1 AND sync_status != 'PENDING_DELETE' LIMIT 1")
    fun observeActiveSession(userId: String): Flow<SessionEntity?>

    @Query(
        """
        SELECT * FROM sessions
        WHERE user_id = :userId
          AND started_at >= :fromMs
          AND started_at <= :toMs
          AND sync_status != 'PENDING_DELETE'
        ORDER BY started_at DESC
        """
    )
    fun observeSessionsInRange(userId: String, fromMs: Long, toMs: Long): Flow<List<SessionEntity>>

    @Query(
        """
        SELECT * FROM sessions
        WHERE user_id = :userId
          AND started_at >= :fromMs
          AND started_at <= :toMs
          AND sync_status != 'PENDING_DELETE'
        ORDER BY started_at DESC
        """
    )
    suspend fun getSessionsInRange(userId: String, fromMs: Long, toMs: Long): List<SessionEntity>

    // ── Incomplete data detection ─────────────────────────────────────

    @Query(
        """
        SELECT s.id FROM sessions s
        WHERE s.user_id = :userId
          AND s.is_active = 0
          AND NOT EXISTS (SELECT 1 FROM self_ratings sr WHERE sr.session_id = s.id)
        """
    )
    suspend fun getSessionIdsWithoutRatings(userId: String): List<String>

    // ── Sync ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM sessions WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: String): List<SessionEntity>

    @Query("UPDATE sessions SET sync_status = :status WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: String, status: String)

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND updated_at > :afterMs")
    suspend fun getUpdatedAfter(userId: String, afterMs: Long): List<SessionEntity>

    // ── Focus Point Performance ────────────────────────────────────────

    /**
     * Returns the average overall_score from completed sessions whose
     * focus_note contains the given text. Returns null if no matching
     * sessions have scores.
     */
    @Query(
        """
        SELECT AVG(overall_score) FROM sessions
        WHERE user_id = :userId
          AND is_active = 0
          AND overall_score IS NOT NULL
          AND focus_note LIKE '%' || :focusText || '%'
        """
    )
    suspend fun getAverageScoreForFocusText(userId: String, focusText: String): Double?

    // ── Delete ────────────────────────────────────────────────────────

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: String)

    @Query("DELETE FROM sessions WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
