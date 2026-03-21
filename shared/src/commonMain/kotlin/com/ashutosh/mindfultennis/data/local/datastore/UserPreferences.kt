package com.ashutosh.mindfultennis.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages user preferences stored in DataStore.
 * Includes last sync timestamp, cached user ID, and UI filter state.
 */
class UserPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        private val KEY_CACHED_USER_ID = stringPreferencesKey("cached_user_id")
        private val KEY_DURATION_FILTER = stringPreferencesKey("duration_filter")
        private val KEY_SELECTED_OPPONENT_IDS = stringPreferencesKey("selected_opponent_ids")
        private val KEY_ASPECT_DURATION_FILTER = stringPreferencesKey("aspect_duration_filter")
        private val KEY_ASPECT_RATING_TYPE = stringPreferencesKey("aspect_rating_type")
    }

    // ── Last Sync Timestamp ───────────────────────────────────────────

    val lastSyncTimestamp: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_TIMESTAMP] ?: 0L
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    // ── Cached User ID ────────────────────────────────────────────────

    val cachedUserId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_CACHED_USER_ID]
    }

    suspend fun setCachedUserId(userId: String?) {
        dataStore.edit { prefs ->
            if (userId != null) {
                prefs[KEY_CACHED_USER_ID] = userId
            } else {
                prefs.remove(KEY_CACHED_USER_ID)
            }
        }
    }

    // ── Duration Filter ───────────────────────────────────────────────

    val durationFilter: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_DURATION_FILTER] ?: "ONE_MONTH"
    }

    suspend fun setDurationFilter(filter: String) {
        dataStore.edit { prefs ->
            prefs[KEY_DURATION_FILTER] = filter
        }
    }

    // ── Selected Opponent IDs (comma-separated) ───────────────────────

    val selectedOpponentIds: Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_SELECTED_OPPONENT_IDS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    suspend fun setSelectedOpponentIds(ids: Set<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_SELECTED_OPPONENT_IDS] = ids.joinToString(",")
        }
    }

    // ── Aspect Duration Filter ────────────────────────────────────────

    val aspectDurationFilter: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_ASPECT_DURATION_FILTER] ?: "ONE_MONTH"
    }

    suspend fun setAspectDurationFilter(filter: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ASPECT_DURATION_FILTER] = filter
        }
    }

    // ── Aspect Rating Type ────────────────────────────────────────────

    val aspectRatingType: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_ASPECT_RATING_TYPE] ?: "SELF"
    }

    suspend fun setAspectRatingType(ratingType: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ASPECT_RATING_TYPE] = ratingType
        }
    }

    // ── Clear All ─────────────────────────────────────────────────────

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
