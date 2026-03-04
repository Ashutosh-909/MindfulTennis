package com.ashutosh.mindfultennis

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import com.ashutosh.mindfultennis.navigation.NavGraph
import com.ashutosh.mindfultennis.navigation.Route
import com.ashutosh.mindfultennis.notification.SessionNotificationManager
import com.ashutosh.mindfultennis.ui.theme.MindfulTennisTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var supabaseClient: SupabaseClient

    /** Pending "End Session" navigation from notification action, consumed by NavGraph setup. */
    private var pendingEndSessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle the deep link callback from Google OAuth (if the app was cold-started via callback)
        handleDeepLink(intent)

        // Check if launched from "End Session" notification action
        pendingEndSessionId = extractEndSessionId(intent)

        setContent {
            MindfulTennisTheme {
                val navController = rememberNavController()

                val authState by authRepository.authState.collectAsStateWithLifecycle(
                    initialValue = AuthState.Loading
                )

                val isAuthenticated = authState is AuthState.Authenticated

                // Navigate to end_session if launched from notification action
                val endSessionId = pendingEndSessionId
                if (endSessionId != null && isAuthenticated) {
                    pendingEndSessionId = null
                    androidx.compose.runtime.LaunchedEffect(endSessionId) {
                        navController.navigate(Route.EndSession(endSessionId).route) {
                            popUpTo(Route.Home.route) { inclusive = false }
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavGraph(
                        navController = navController,
                        isAuthenticated = isAuthenticated,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle the deep link callback from Google OAuth (if the app was already running)
        handleDeepLink(intent)

        // Handle "End Session" notification action when app is already running
        val sessionId = extractEndSessionId(intent)
        if (sessionId != null) {
            pendingEndSessionId = sessionId
            // Recreate to trigger the LaunchedEffect
            recreate()
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val scheme = uri.scheme ?: return
        // Only handle our custom scheme callback
        if (scheme == "com.ashutosh.mindfultennis") {
            supabaseClient.handleDeeplinks(intent)
        }
    }

    /**
     * Extracts the session ID from an "End Session" notification action intent.
     */
    private fun extractEndSessionId(intent: Intent?): String? {
        if (intent?.action != SessionNotificationManager.ACTION_END_SESSION) return null
        return intent.getStringExtra(SessionNotificationManager.EXTRA_SESSION_ID)
    }
}