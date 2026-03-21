package com.ashutosh.mindfultennis.data.sync

interface BackgroundSyncScheduler {
    fun schedulePeriodic()
    fun cancel()
}
