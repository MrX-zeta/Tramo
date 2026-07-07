package com.luis.tramo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luis.tramo.navigation.TramoNavHost
import com.luis.tramo.ui.theme.TramoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()
            val darkOverride by mainViewModel.darkModeOverride.collectAsStateWithLifecycle()

            TramoTheme(darkTheme = darkOverride ?: isSystemInDarkTheme()) {
                // Wait until the onboarded flag is read to avoid a wrong-screen flash.
                startDestination?.let { destination ->
                    TramoNavHost(startDestination = destination)
                }
            }
        }
    }
}
