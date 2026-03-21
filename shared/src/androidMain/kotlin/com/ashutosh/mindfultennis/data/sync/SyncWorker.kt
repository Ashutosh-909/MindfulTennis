package com.ashutosh.mindfultennis.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val syncManager: SyncManager by inject()
    private val userPreferences: UserPreferences by inject()

    override suspend fun doWork(): Result {
        val userId = userPreferences.cachedUserId.first()
        if (userId.isNullOrBlank()) {
            return Result.success()
        }

        return when {
            syncManager.sync(userId).isSuccess -> Result.success()
            runAttemptCount < 3 -> Result.retry()
            else -> Result.failure()
        }
    }
}
