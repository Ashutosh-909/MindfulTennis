package com.ashutosh.mindfultennis.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun NavGraph(
    navController: NavHostController,
    isAuthenticated: Boolean,
    modifier: Modifier = Modifier,
) {
    val startDestination = if (isAuthenticated) Route.Home.route else Route.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Route.Login.route) {
            // TODO: LoginScreen (Milestone 2)
        }

        composable(Route.Home.route) {
            // TODO: HomeScreen (Milestone 4)
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
