package com.ashutosh.mindfultennis.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ashutosh.mindfultennis.MainActivity
import com.ashutosh.mindfultennis.R
import com.ashutosh.mindfultennis.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the ongoing notification shown while a tennis session is active.
 * Handles notification channel creation, building, updating, and cancelling.
 */
@Singleton
class SessionNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Creates the notification channel for active session notifications.
     * Safe to call multiple times — the system ignores if the channel already exists.
     */
    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = CHANNEL_DESCRIPTION
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Builds an ongoing foreground notification for the active session.
     *
     * @param sessionId The ID of the active session.
     * @param elapsedMs How long the session has been running in milliseconds.
     * @return A built [android.app.Notification].
     */
    fun buildNotification(
        sessionId: String,
        elapsedMs: Long,
    ): android.app.Notification {
        // Tap notification → open app to Home
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_CONTENT,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "End Session" action → deep-link to end_session/{sessionId}
        val endSessionIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_END_SESSION,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = ACTION_END_SESSION
                putExtra(EXTRA_SESSION_ID, sessionId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // "Cancel Session" action → discard the session
        val cancelSessionIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_CANCEL_SESSION,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = ACTION_CANCEL_SESSION
                putExtra(EXTRA_SESSION_ID, sessionId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val elapsedText = DateTimeUtils.formatDuration(elapsedMs)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Session in progress")
            .setContentText("Elapsed: $elapsedText")
            .setSmallIcon(R.drawable.ic_notification_session)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_notification_stop,
                "End Session",
                endSessionIntent,
            )
            .addAction(
                R.drawable.ic_notification_stop,
                "Cancel",
                cancelSessionIntent,
            )
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Updates the existing notification with new elapsed time.
     */
    fun updateNotification(sessionId: String, elapsedMs: Long) {
        val notification = buildNotification(sessionId, elapsedMs)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancels the active session notification.
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "active_session"
        const val CHANNEL_NAME = "Active Session"
        const val CHANNEL_DESCRIPTION = "Shows an ongoing notification while a tennis session is active"
        const val NOTIFICATION_ID = 1001

        const val ACTION_END_SESSION = "com.ashutosh.mindfultennis.ACTION_END_SESSION"
        const val ACTION_CANCEL_SESSION = "com.ashutosh.mindfultennis.ACTION_CANCEL_SESSION"
        const val EXTRA_SESSION_ID = "extra_session_id"

        private const val REQUEST_CODE_CONTENT = 100
        private const val REQUEST_CODE_END_SESSION = 101
        private const val REQUEST_CODE_CANCEL_SESSION = 102
    }
}
