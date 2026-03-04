package com.ashutosh.mindfultennis.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ashutosh.mindfultennis.data.local.db.entity.SelfRatingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SelfRatingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(ratings: List<SelfRatingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: SelfRatingEntity)

    @Query("SELECT * FROM self_ratings WHERE session_id = :sessionId")
    suspend fun getForSession(sessionId: String): List<SelfRatingEntity>

    @Query("SELECT * FROM self_ratings WHERE session_id = :sessionId")
    fun observeForSession(sessionId: String): Flow<List<SelfRatingEntity>>

    @Query("SELECT * FROM self_ratings WHERE session_id IN (:sessionIds)")
    suspend fun getForSessions(sessionIds: List<String>): List<SelfRatingEntity>

    @Query("SELECT * FROM self_ratings WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: String): List<SelfRatingEntity>

    @Query("UPDATE self_ratings SET sync_status = :status WHERE session_id = :sessionId")
    suspend fun updateSyncStatusForSession(sessionId: String, status: String)

    @Query("DELETE FROM self_ratings WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
