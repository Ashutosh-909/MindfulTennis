@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ashutosh.mindfultennis.data.sync

import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

class IosSyncScheduler : BackgroundSyncScheduler {

    companion object {
        // Must exactly match BGTaskSchedulerPermittedIdentifiers in Info.plist
        const val TASK_IDENTIFIER = "com.nextjedi.mindful-tennis.sync"
    }

    override fun schedulePeriodic() {
        val request = BGAppRefreshTaskRequest(identifier = TASK_IDENTIFIER)
        request.earliestBeginDate = nextOccurrenceOf1_30am()
        try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error = null)
        } catch (_: Exception) {
            // BGTaskScheduler may not be available in all contexts
        }
    }

    override fun cancel() {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(TASK_IDENTIFIER)
    }

    private fun nextOccurrenceOf1_30am(): NSDate {
        val calendar = NSCalendar.currentCalendar
        val now = NSDate()

        // Build a date for today at 01:30:00
        val parts = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
            fromDate = now,
        )
        parts.hour = 1
        parts.minute = 30
        parts.second = 0

        val todayAt1_30 = calendar.dateFromComponents(parts) ?: return now

        // If 1:30 AM has already passed today, add one day
        return if (todayAt1_30.compare(now) < 0) {
            calendar.dateByAddingUnit(
                unit = NSCalendarUnitDay,
                value = 1L,
                toDate = todayAt1_30,
                options = 0u,
            ) ?: now
        } else {
            todayAt1_30
        }
    }
}
