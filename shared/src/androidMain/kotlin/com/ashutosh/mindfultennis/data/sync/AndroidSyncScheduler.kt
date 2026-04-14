package com.ashutosh.mindfultennis.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AndroidSyncScheduler(private val context: Context) : BackgroundSyncScheduler {

    override fun schedulePeriodic() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilNext1_30am(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        // UPDATE replaces any existing schedule so re-installs always land at 1:30 AM
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "mindful_tennis_sync",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork("mindful_tennis_sync")
    }

    private fun delayUntilNext1_30am(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If 1:30 AM has already passed today, target tomorrow
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
