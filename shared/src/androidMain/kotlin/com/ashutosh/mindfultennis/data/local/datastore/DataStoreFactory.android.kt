package com.ashutosh.mindfultennis.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

fun createDataStore(context: Context): DataStore<Preferences> = context.dataStore
