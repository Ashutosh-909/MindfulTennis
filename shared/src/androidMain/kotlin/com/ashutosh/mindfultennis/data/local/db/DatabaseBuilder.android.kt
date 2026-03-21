package com.ashutosh.mindfultennis.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<MindfulDatabase> {
    val dbFile = context.getDatabasePath(MindfulDatabase.DATABASE_NAME)
    return Room.databaseBuilder<MindfulDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
}
