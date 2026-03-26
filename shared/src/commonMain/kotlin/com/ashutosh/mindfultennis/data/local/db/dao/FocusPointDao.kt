package com.ashutosh.mindfultennis.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ashutosh.mindfultennis.data.local.db.entity.FocusPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(focusPoint: FocusPointEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(focusPoints: List<FocusPointEntity>)

    @Query("SELECT * FROM focus_points WHERE user_id = :userId AND sync_status != 'PENDING_DELETE' ORDER BY created_at DESC")
    fun observeAllForUser(userId: String): Flow<List<FocusPointEntity>>

    @Query("SELECT * FROM focus_points WHERE user_id = :userId AND sync_status != 'PENDING_DELETE' ORDER BY created_at DESC")
    suspend fun getAllForUser(userId: String): List<FocusPointEntity>

    @Query("SELECT * FROM focus_points WHERE id = :id")
    suspend fun getById(id: String): FocusPointEntity?

    @Query("SELECT * FROM focus_points WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: String): List<FocusPointEntity>

    @Query("UPDATE focus_points SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM focus_points WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM focus_points WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
