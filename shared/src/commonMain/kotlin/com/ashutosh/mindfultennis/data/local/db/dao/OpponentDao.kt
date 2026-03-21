package com.ashutosh.mindfultennis.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ashutosh.mindfultennis.data.local.db.entity.OpponentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpponentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(opponent: OpponentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(opponents: List<OpponentEntity>)

    @Query("SELECT * FROM opponents WHERE user_id = :userId ORDER BY name ASC")
    fun observeAllForUser(userId: String): Flow<List<OpponentEntity>>

    @Query("SELECT * FROM opponents WHERE user_id = :userId ORDER BY name ASC")
    suspend fun getAllForUser(userId: String): List<OpponentEntity>

    @Query("SELECT * FROM opponents WHERE id = :id")
    suspend fun getById(id: String): OpponentEntity?

    @Query("SELECT * FROM opponents WHERE sync_status = :status")
    suspend fun getBySyncStatus(status: String): List<OpponentEntity>

    @Query("UPDATE opponents SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM opponents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM opponents WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)
}
