package com.ashutosh.mindfultennis.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the `set_scores` table.
 */
@Entity(
    tableName = "set_scores",
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
data class SetScoreEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "set_number")
    val setNumber: Int,

    @ColumnInfo(name = "user_score")
    val userScore: Int,

    @ColumnInfo(name = "opponent_score")
    val opponentScore: Int,

    @ColumnInfo(name = "opponent_id")
    val opponentId: String? = null,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = SyncStatus.PENDING.name,
)
