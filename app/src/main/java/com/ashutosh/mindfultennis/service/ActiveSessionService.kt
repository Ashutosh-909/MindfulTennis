package com.ashutosh.mindfultennis.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.ashutosh.mindfultennis.data.local.db.dao.SessionDao
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.notification.SessionNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that runs while a tennis session is active.
 * Shows an ongoing notification with elapsed time and an "End Session" action.
 *
 * Uses START_STICKY to survive process death. On restart, it checks Room
 * for an active session and resumes the timer if one exists.
 */
@AndroidEntryPoint
class ActiveSessionService : Service() {

    @Inject
    lateinit var notificationManager: SessionNotificationManager

    @Inject
    lateinit var sessionDao: SessionDao

    @Inject
    lateinit var userPreferences: UserPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    private var currentSessionId: String? = null
    private var sessionStartedAt: Long = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager.createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
        val startedAt = intent?.getLongExtra(EXTRA_STARTED_AT, 0L) ?: 0L

        if (sessionId != null && startedAt > 0L) {
            // Normal start with session info
            startSession(sessionId, startedAt)
        } else {
            // Restarted by system (START_STICKY) or boot receiver — recover from Room
            recoverActiveSession()
        }

        return START_STICKY
    }

    private fun startSession(sessionId: String, startedAt: Long) {
        currentSessionId = sessionId
        sessionStartedAt = startedAt

        val elapsedMs = System.currentTimeMillis() - startedAt
        val notification = notificationManager.buildNotification(sessionId, elapsedMs)
        startForeground(SessionNotificationManager.NOTIFICATION_ID, notification)

        startTimer()
    }

    private fun recoverActiveSession() {
        serviceScope.launch {
            val userId = userPreferences.cachedUserId.first()
            if (userId.isNullOrBlank()) {
                stopSelf()
                return@launch
            }

            val activeSession = sessionDao.getActiveSession(userId)
            if (activeSession != null) {
                startSession(activeSession.id, activeSession.startedAt)
            } else {
                stopSelf()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val sessionId = currentSessionId ?: break
                val elapsedMs = System.currentTimeMillis() - sessionStartedAt
                notificationManager.updateNotification(sessionId, elapsedMs)
                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        notificationManager.cancelNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_STARTED_AT = "extra_started_at"

        private const val UPDATE_INTERVAL_MS = 1_000L

        /**
         * Starts the foreground service for the given active session.
         */
        fun start(context: Context, sessionId: String, startedAt: Long) {
            val intent = Intent(context, ActiveSessionService::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_STARTED_AT, startedAt)
            }
            context.startForegroundService(intent)
        }

        /**
         * Stops the foreground service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, ActiveSessionService::class.java)
            context.stopService(intent)
        }
    }
}
