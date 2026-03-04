package com.ashutosh.mindfultennis.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ashutosh.mindfultennis.data.local.db.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    // ── Inserts / Updates ─────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<SessionEntity>)

    @Update
    suspend fun update(session: SessionEntity)

    // ── Queries ───────────────────────────────────────────────────────

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getById(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeById(sessionId: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE user_id = :userId ORDER BY started_at DESC")
    fun observeAllForUser(userId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE user_id = :userId ORDER BY started_at DESC")
    suspend fun getAllForUser(userId: String): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND is_active = 1 LIMIT 1")
    suspend fun getActiveSession(userId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND is_active = 1 LIMIT 1")
    fun observeActiveSession(userId: String): Flow<SessionEntity?>

    @Query(
        """
        SELECT * FROM sessions 
        WHERE user_id = :userId 
          AND started_at >= :fromMs 
          AND started_at <= :toMs 
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
        ORDER BY started_at DESC
        """
    )
    suspend fun getSessionsInRange(userId: String, fromMs: Long, toMs: Long): List<SessionEntity>

    // ── Sync ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM sessions WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: String): List<SessionEntity>

    @Query("UPDATE sessions SET sync_status = :status WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: String, status: String)

    @Query("SELECT * FROM sessions WHERE user_id = :userId AND updated_at > :afterMs")
    suspend fun getUpdatedAfter(userId: String, afterMs: Long): List<SessionEntity>

    // ── Delete ────────────────────────────────────────────────────────

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: String)

    @Query("DELETE FROM sessions WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
