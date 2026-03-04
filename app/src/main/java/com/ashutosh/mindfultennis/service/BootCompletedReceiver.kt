package com.ashutosh.mindfultennis.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ashutosh.mindfultennis.data.local.db.dao.SessionDao
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives BOOT_COMPLETED broadcast and restarts [ActiveSessionService]
 * if an active session exists in Room.
 *
 * This ensures the foreground service notification is restored after a device reboot.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var sessionDao: SessionDao

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = userPreferences.cachedUserId.first()
                if (userId.isNullOrBlank()) return@launch

                val activeSession = sessionDao.getActiveSession(userId)
                if (activeSession != null) {
                    ActiveSessionService.start(
                        context = context,
                        sessionId = activeSession.id,
                        startedAt = activeSession.startedAt,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
