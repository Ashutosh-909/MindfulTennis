package com.ashutosh.mindfultennis.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AndroidSyncScheduler(private val context: Context) : BackgroundSyncScheduler {

    override fun schedulePeriodic() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "mindful_tennis_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork("mindful_tennis_sync")
    }
}
