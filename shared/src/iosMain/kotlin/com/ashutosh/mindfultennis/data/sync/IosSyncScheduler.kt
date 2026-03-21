@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ashutosh.mindfultennis.data.sync

import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateByAddingTimeInterval

class IosSyncScheduler : BackgroundSyncScheduler {

    companion object {
        const val TASK_IDENTIFIER = "com.ashutosh.mindfultennis.sync"
    }

    override fun schedulePeriodic() {
        val request = BGAppRefreshTaskRequest(identifier = TASK_IDENTIFIER)
        request.earliestBeginDate = NSDate().dateByAddingTimeInterval(15.0 * 60.0)
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
        } catch (_: Exception) {
            // BGTaskScheduler may not be available in all contexts
        }
    }

    override fun cancel() {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(TASK_IDENTIFIER)
    }
}
