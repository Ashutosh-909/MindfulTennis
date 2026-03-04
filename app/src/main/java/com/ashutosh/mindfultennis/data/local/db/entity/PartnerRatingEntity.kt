package com.ashutosh.mindfultennis.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `partner_ratings` table.
 */
@Entity(
    tableName = "partner_ratings",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("session_id")],
)
data class PartnerRatingEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    val aspect: String,

    val rating: Int,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SyncStatus.PENDING.name,
)
