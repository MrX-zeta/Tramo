package com.luis.tramo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.luis.tramo.ui.onboarding.OnboardingScreen
import com.luis.tramo.ui.timer.TimerScreen

@Composable
fun TramoNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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
            TimerScreen()
        }
    }
}
