package com.luis.tramo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.luis.tramo.ui.onboarding.OnboardingScreen
import com.luis.tramo.ui.report.ReportScreen
import com.luis.tramo.ui.settings.SettingsScreen
import com.luis.tramo.ui.tasks.TaskListScreen
import com.luis.tramo.ui.timer.TimerScreen

@Composable
fun TramoNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // A single shared NavigationBar, reused by each top-level screen's Scaffold.
    val bottomBar: @Composable () -> Unit = {
        TramoBottomBar(currentRoute) { destination ->
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val openSettings: () -> Unit = { navController.navigate(TramoDestinations.SETTINGS) }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(TramoDestinations.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(TramoDestinations.TIMER) {
                        popUpTo(TramoDestinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(TramoDestinations.TIMER) {
            TimerScreen(bottomBar = bottomBar, onOpenSettings = openSettings)
        }
        composable(TramoDestinations.TASKS) {
            TaskListScreen(bottomBar = bottomBar, onOpenSettings = openSettings)
        }
        composable(TramoDestinations.REPORT) {
            ReportScreen(bottomBar = bottomBar, onOpenSettings = openSettings)
        }
        composable(
            route = "${TramoDestinations.SETTINGS}?highlight={highlight}",
            arguments = listOf(
                androidx.navigation.navArgument("highlight") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entry ->
            SettingsScreen(
                onBack = { navController.popBackStack() },
                highlight = entry.arguments?.getString("highlight")
            )
        }
    }
}
