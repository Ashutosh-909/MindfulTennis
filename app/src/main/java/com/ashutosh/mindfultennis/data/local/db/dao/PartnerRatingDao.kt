package com.ashutosh.mindfultennis.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ashutosh.mindfultennis.data.local.db.entity.PartnerRatingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerRatingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(ratings: List<PartnerRatingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rating: PartnerRatingEntity)

    @Query("SELECT * FROM partner_ratings WHERE session_id = :sessionId")
    suspend fun getForSession(sessionId: String): List<PartnerRatingEntity>

    @Query("SELECT * FROM partner_ratings WHERE session_id = :sessionId")
    fun observeForSession(sessionId: String): Flow<List<PartnerRatingEntity>>

    @Query("SELECT * FROM partner_ratings WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: String): List<PartnerRatingEntity>

    @Query("UPDATE partner_ratings SET sync_status = :status WHERE session_id = :sessionId")
    suspend fun updateSyncStatusForSession(sessionId: String, status: String)

    @Query("SELECT * FROM partner_ratings WHERE session_id IN (:sessionIds)")
    suspend fun getForSessions(sessionIds: List<String>): List<PartnerRatingEntity>

    @Query("DELETE FROM partner_ratings WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
