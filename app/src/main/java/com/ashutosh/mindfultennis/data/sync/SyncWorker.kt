package com.ashutosh.mindfultennis.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that periodically syncs local Room data with Supabase.
 * Runs every 15 minutes when a network connection is available.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: SyncManager,
    private val userPreferences: UserPreferences,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "mindful_tennis_sync"

        /**
         * Enqueues the periodic sync worker with network constraint.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest,
            )

            Log.d(TAG, "Periodic sync worker enqueued (15 min interval)")
        }

        /**
         * Cancels the periodic sync worker.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Periodic sync worker cancelled")
        }
    }

    override suspend fun doWork(): Result {
        val userId = userPreferences.cachedUserId.first()
        if (userId.isNullOrBlank()) {
            Log.d(TAG, "No cached user ID, skipping sync")
            return Result.success()
        }

        return when {
            syncManager.sync(userId).isSuccess -> {
                Log.d(TAG, "Sync succeeded")
                Result.success()
            }
            runAttemptCount < 3 -> {
                Log.w(TAG, "Sync failed, will retry (attempt $runAttemptCount)")
                Result.retry()
            }
            else -> {
                Log.e(TAG, "Sync failed after $runAttemptCount attempts")
                Result.failure()
            }
        }
    }
}
