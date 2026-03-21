package com.ashutosh.mindfultennis.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ashutosh.mindfultennis.data.local.db.entity.SetScoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetScoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(scores: List<SetScoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(score: SetScoreEntity)

    @Query("SELECT * FROM set_scores WHERE session_id = :sessionId ORDER BY set_number ASC")
    suspend fun getForSession(sessionId: String): List<SetScoreEntity>

    @Query("SELECT * FROM set_scores WHERE session_id = :sessionId ORDER BY set_number ASC")
    fun observeForSession(sessionId: String): Flow<List<SetScoreEntity>>

    @Query("SELECT * FROM set_scores WHERE session_id IN (:sessionIds) ORDER BY set_number ASC")
    suspend fun getForSessions(sessionIds: List<String>): List<SetScoreEntity>

    @Query("SELECT * FROM set_scores WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: String): List<SetScoreEntity>

    @Query("UPDATE set_scores SET sync_status = :status WHERE session_id = :sessionId")
    suspend fun updateSyncStatusForSession(sessionId: String, status: String)

    @Query("DELETE FROM set_scores WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
