package com.ashutosh.mindfultennis.domain.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Duration filter options for the dashboard charts and stats.
 */
enum class DurationFilter(val label: String) {
    ONE_WEEK("1W"),
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ONE_YEAR("1Y");

    /**
     * Returns the start epoch millis for this filter duration from now.
     */
    fun startEpochMs(timeZoneId: String = TimeZone.getDefault().id): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
        when (this) {
            ONE_WEEK -> cal.add(Calendar.DAY_OF_YEAR, -7)
            ONE_MONTH -> cal.add(Calendar.MONTH, -1)
            THREE_MONTHS -> cal.add(Calendar.MONTH, -3)
            SIX_MONTHS -> cal.add(Calendar.MONTH, -6)
            ONE_YEAR -> cal.add(Calendar.YEAR, -1)
        }
        return cal.timeInMillis
    }
}
