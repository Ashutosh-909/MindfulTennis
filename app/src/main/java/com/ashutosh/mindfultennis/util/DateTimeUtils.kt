package com.ashutosh.mindfultennis.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Utility functions for date and time formatting.
 */
object DateTimeUtils {

    /**
     * Formats epoch millis to a human-readable date string.
     * e.g. "Mar 4, 2026"
     */
    fun formatDate(epochMs: Long, timeZoneId: String = TimeZone.getDefault().id): String {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(Date(epochMs))
    }

    /**
     * Formats epoch millis to a time string.
     * e.g. "3:45 PM"
     */
    fun formatTime(epochMs: Long, timeZoneId: String = TimeZone.getDefault().id): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone(timeZoneId)
        return sdf.format(Date(epochMs))
    }

    /**
     * Formats a duration in millis to a human-readable string.
     * e.g. "1h 27m"
     */
    fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    /**
     * Formats a date range (start to end time on the same day).
     * e.g. "Mar 4, 3:45 – 5:12 PM"
     */
    fun formatSessionDateRange(
        startMs: Long,
        endMs: Long,
        timeZoneId: String = TimeZone.getDefault().id,
    ): String {
        val datePart = SimpleDateFormat("MMM d", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timeZoneId)
        }.format(Date(startMs))

        val startTime = SimpleDateFormat("h:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timeZoneId)
        }.format(Date(startMs))

        val endTime = SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(timeZoneId)
        }.format(Date(endMs))

        return "$datePart, $startTime – $endTime"
    }
}
