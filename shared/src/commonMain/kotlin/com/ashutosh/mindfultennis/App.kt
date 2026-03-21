package com.ashutosh.mindfultennis

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.rememberNavController
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import com.ashutosh.mindfultennis.navigation.NavGraph
import com.ashutosh.mindfultennis.ui.theme.MindfulTennisTheme
import org.koin.compose.koinInject

@Composable
fun App() {
    MindfulTennisTheme {
        val navController = rememberNavController()
        val authRepository = koinInject<AuthRepository>()
        val authState by authRepository.authState.collectAsState(
            initial = AuthState.Loading
        )
        val isAuthenticated = authState is AuthState.Authenticated

        Surface(modifier = Modifier.fillMaxSize()) {
            NavGraph(
                navController = navController,
                isAuthenticated = isAuthenticated,
                pendingCancelSessionId = null,
            )
        }
    }
}
