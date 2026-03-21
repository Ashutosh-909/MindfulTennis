package com.ashutosh.mindfultennis.data.local.db

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSHomeDirectory

fun getDatabaseBuilder(): RoomDatabase.Builder<MindfulDatabase> {
    val dbFilePath = NSHomeDirectory() + "/Documents/${MindfulDatabase.DATABASE_NAME}"
    return Room.databaseBuilder<MindfulDatabase>(
        name = dbFilePath,
    )
}
