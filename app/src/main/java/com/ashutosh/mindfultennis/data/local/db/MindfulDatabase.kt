package com.ashutosh.mindfultennis.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ashutosh.mindfultennis.data.local.db.dao.FocusPointDao
import com.ashutosh.mindfultennis.data.local.db.dao.OpponentDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerDao
import com.ashutosh.mindfultennis.data.local.db.dao.PartnerRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SelfRatingDao
import com.ashutosh.mindfultennis.data.local.db.dao.SessionDao
import com.ashutosh.mindfultennis.data.local.db.dao.SetScoreDao
import com.ashutosh.mindfultennis.data.local.db.entity.FocusPointEntity
import com.ashutosh.mindfultennis.data.local.db.entity.OpponentEntity
import com.ashutosh.mindfultennis.data.local.db.entity.PartnerEntity
import com.ashutosh.mindfultennis.data.local.db.entity.PartnerRatingEntity
import com.ashutosh.mindfultennis.data.local.db.entity.SelfRatingEntity
import com.ashutosh.mindfultennis.data.local.db.entity.SessionEntity
import com.ashutosh.mindfultennis.data.local.db.entity.SetScoreEntity

@Database(
    entities = [
        SessionEntity::class,
        SelfRatingEntity::class,
        PartnerRatingEntity::class,
        FocusPointEntity::class,
        OpponentEntity::class,
        PartnerEntity::class,
        SetScoreEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MindfulDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun selfRatingDao(): SelfRatingDao
    abstract fun partnerRatingDao(): PartnerRatingDao
    abstract fun focusPointDao(): FocusPointDao
    abstract fun opponentDao(): OpponentDao
    abstract fun partnerDao(): PartnerDao
    abstract fun setScoreDao(): SetScoreDao

    companion object {
        const val DATABASE_NAME = "mindful_tennis_db"
    }
}
