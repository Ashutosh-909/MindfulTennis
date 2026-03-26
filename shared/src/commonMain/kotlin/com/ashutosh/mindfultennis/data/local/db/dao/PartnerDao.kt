package com.ashutosh.mindfultennis.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ashutosh.mindfultennis.data.local.db.entity.PartnerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(partner: PartnerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(partners: List<PartnerEntity>)

    @Query("SELECT * FROM partners WHERE user_id = :userId AND sync_status != 'PENDING_DELETE' ORDER BY name ASC")
    fun observeAllForUser(userId: String): Flow<List<PartnerEntity>>

    @Query("SELECT * FROM partners WHERE user_id = :userId AND sync_status != 'PENDING_DELETE' ORDER BY name ASC")
    suspend fun getAllForUser(userId: String): List<PartnerEntity>

    @Query("SELECT * FROM partners WHERE id = :id")
    suspend fun getById(id: String): PartnerEntity?

    @Query("SELECT * FROM partners WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: String): List<PartnerEntity>

    @Query("UPDATE partners SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM partners WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM partners WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
