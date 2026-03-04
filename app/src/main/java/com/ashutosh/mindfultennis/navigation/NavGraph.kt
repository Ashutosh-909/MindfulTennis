package com.ashutosh.mindfultennis.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ashutosh.mindfultennis.ui.endsession.EndSessionScreen
import com.ashutosh.mindfultennis.ui.endsession.EndSessionViewModel
import com.ashutosh.mindfultennis.ui.home.HomeScreen
import com.ashutosh.mindfultennis.ui.home.HomeViewModel
import com.ashutosh.mindfultennis.ui.login.LoginScreen
import com.ashutosh.mindfultennis.ui.login.LoginViewModel
import com.ashutosh.mindfultennis.ui.sessions.SessionDetailScreen
import com.ashutosh.mindfultennis.ui.sessions.SessionDetailViewModel
import com.ashutosh.mindfultennis.ui.sessions.SessionsListScreen
import com.ashutosh.mindfultennis.ui.sessions.SessionsListViewModel
import com.ashutosh.mindfultennis.ui.startsession.StartSessionScreen
import com.ashutosh.mindfultennis.ui.startsession.StartSessionViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    isAuthenticated: Boolean,
    modifier: Modifier = Modifier,
) {
    val startDestination = if (isAuthenticated) Route.Home.route else Route.Login.route

    // Auth guard: redirect to login if unauthenticated, or to home if authenticated
    LaunchedEffect(isAuthenticated) {
        val currentRoute = navController.currentDestination?.route
        if (!isAuthenticated && currentRoute != Route.Login.route) {
            navController.navigate(Route.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (isAuthenticated && currentRoute == Route.Login.route) {
            navController.navigate(Route.Home.route) {
                popUpTo(Route.Login.route) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Route.Login.route) {
            val viewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = viewModel,
                onSignedIn = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onStartSessionClicked = {
                    navController.navigate(Route.StartSession.route)
                },
                onEndSessionClicked = { sessionId ->
                    navController.navigate(Route.EndSession(sessionId).route)
                },
                onShowSessionsClicked = {
                    navController.navigate(Route.SessionsList.route)
                },
            )
        }

        composable(Route.StartSession.route) {
            val viewModel: StartSessionViewModel = hiltViewModel()
            StartSessionScreen(
                viewModel = viewModel,
                onSessionStarted = {
                    navController.popBackStack(Route.Home.route, inclusive = false)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Route.EndSession.ROUTE_PATTERN,
            arguments = listOf(
                navArgument(Route.EndSession.ARG_SESSION_ID) {
                    type = NavType.StringType
                }
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Route.EndSession.ARG_SESSION_ID) ?: return@composable
            val viewModel: EndSessionViewModel = hiltViewModel()
            EndSessionScreen(
                viewModel = viewModel,
                onSessionSubmitted = {
                    navController.popBackStack(Route.Home.route, inclusive = false)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Route.SessionsList.route) {
            val viewModel: SessionsListViewModel = hiltViewModel()
            SessionsListScreen(
                viewModel = viewModel,
                onSessionClicked = { sessionId ->
                    navController.navigate(Route.SessionDetail(sessionId).route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Route.SessionDetail.ROUTE_PATTERN,
            arguments = listOf(
                navArgument(Route.SessionDetail.ARG_SESSION_ID) {
                    type = NavType.StringType
                }
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Route.SessionDetail.ARG_SESSION_ID) ?: return@composable
            val viewModel: SessionDetailViewModel = hiltViewModel()
            SessionDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
