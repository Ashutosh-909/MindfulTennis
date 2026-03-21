package com.ashutosh.mindfultennis.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.time.Duration.Companion.days

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
    fun startEpochMs(timeZoneId: String = TimeZone.currentSystemDefault().id): Long {
        val now = Clock.System.now()
        val tz = TimeZone.of(timeZoneId)
        val start = when (this) {
            ONE_WEEK -> now.minus(7.days)
            ONE_MONTH -> now.minus(DateTimePeriod(months = 1), tz)
            THREE_MONTHS -> now.minus(DateTimePeriod(months = 3), tz)
            SIX_MONTHS -> now.minus(DateTimePeriod(months = 6), tz)
            ONE_YEAR -> now.minus(DateTimePeriod(years = 1), tz)
        }
        return start.toEpochMilliseconds()
    }
}
