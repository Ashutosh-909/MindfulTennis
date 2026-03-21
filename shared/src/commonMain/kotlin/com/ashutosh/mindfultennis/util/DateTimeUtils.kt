package com.ashutosh.mindfultennis.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Utility functions for date and time formatting.
 * Uses kotlinx-datetime for multiplatform compatibility.
 */
object DateTimeUtils {

    private val MONTH_NAMES = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    /**
     * Formats epoch millis to a human-readable date string.
     * e.g. "Mar 4, 2026"
     */
    fun formatDate(epochMs: Long, timeZoneId: String = TimeZone.currentSystemDefault().id): String {
        val instant = Instant.fromEpochMilliseconds(epochMs)
        val tz = TimeZone.of(timeZoneId)
        val dt = instant.toLocalDateTime(tz)
        return "${MONTH_NAMES[dt.monthNumber - 1]} ${dt.dayOfMonth}, ${dt.year}"
    }

    /**
     * Formats epoch millis to a short date without year.
     * e.g. "Mar 4" -- used in chart axis labels and card headers.
     */
    fun formatShortDate(epochMs: Long, timeZoneId: String = TimeZone.currentSystemDefault().id): String {
        val instant = Instant.fromEpochMilliseconds(epochMs)
        val tz = TimeZone.of(timeZoneId)
        val dt = instant.toLocalDateTime(tz)
        return "${MONTH_NAMES[dt.monthNumber - 1]} ${dt.dayOfMonth}"
    }

    /**
     * Formats epoch millis to a time string.
     * e.g. "3:45 PM"
     */
    fun formatTime(epochMs: Long, timeZoneId: String = TimeZone.currentSystemDefault().id): String {
        val instant = Instant.fromEpochMilliseconds(epochMs)
        val tz = TimeZone.of(timeZoneId)
        val dt = instant.toLocalDateTime(tz)
        val hour24 = dt.hour
        val minute = dt.minute
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return "$hour12:${minute.toString().padStart(2, '0')} $amPm"
    }

    /**
     * Formats a duration in millis to a human-readable string.
     * e.g. "1h 27m"
     */
    fun formatDuration(durationMs: Long): String {
        val totalMinutes = durationMs / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    /**
     * Formats a date range (start to end time on the same day).
     * e.g. "Mar 4, 3:45 - 5:12 PM"
     */
    fun formatSessionDateRange(
        startMs: Long,
        endMs: Long,
        timeZoneId: String = TimeZone.currentSystemDefault().id,
    ): String {
        val tz = TimeZone.of(timeZoneId)

        val startInstant = Instant.fromEpochMilliseconds(startMs)
        val startDt = startInstant.toLocalDateTime(tz)

        val endInstant = Instant.fromEpochMilliseconds(endMs)
        val endDt = endInstant.toLocalDateTime(tz)

        val datePart = "${MONTH_NAMES[startDt.monthNumber - 1]} ${startDt.dayOfMonth}"

        val startHour24 = startDt.hour
        val startHour12 = when {
            startHour24 == 0 -> 12
            startHour24 > 12 -> startHour24 - 12
            else -> startHour24
        }
        val startTime = "$startHour12:${startDt.minute.toString().padStart(2, '0')}"

        val endHour24 = endDt.hour
        val endAmPm = if (endHour24 < 12) "AM" else "PM"
        val endHour12 = when {
            endHour24 == 0 -> 12
            endHour24 > 12 -> endHour24 - 12
            else -> endHour24
        }
        val endTime = "$endHour12:${endDt.minute.toString().padStart(2, '0')} $endAmPm"

        return "$datePart, $startTime \u2013 $endTime"
    }
}
