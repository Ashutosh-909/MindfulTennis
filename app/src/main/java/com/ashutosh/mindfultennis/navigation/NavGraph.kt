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
import com.ashutosh.mindfultennis.ui.home.HomeScreen
import com.ashutosh.mindfultennis.ui.home.HomeViewModel
import com.ashutosh.mindfultennis.ui.login.LoginScreen
import com.ashutosh.mindfultennis.ui.login.LoginViewModel

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
            // TODO: StartSessionScreen (Milestone 5)
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
            // TODO: EndSessionScreen (Milestone 5)
        }

        composable(Route.SessionsList.route) {
            // TODO: SessionsListScreen (Milestone 7)
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
            // TODO: SessionDetailScreen (Milestone 7)
        }
    }
}
