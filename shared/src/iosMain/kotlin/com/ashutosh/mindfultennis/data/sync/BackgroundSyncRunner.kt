package com.ashutosh.mindfultennis.data.sync

import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Called by the iOS BGTaskScheduler handler in iOSApp.swift.
 * Runs the sync and signals completion via [onComplete].
 */
object BackgroundSyncRunner : KoinComponent {

    private val syncManager: SyncManager by inject()
    private val userPreferences: UserPreferences by inject()

    fun runSync(onComplete: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            val userId = userPreferences.cachedUserId.first()
            val success = if (!userId.isNullOrBlank()) {
                syncManager.sync(userId).isSuccess
            } else {
                false
            }
            onComplete(success)
        }
    }
}
