package com.luis.tramo

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.luis.tramo.data.DatabaseSeeder
import com.luis.tramo.navigation.TramoDestinations
import com.luis.tramo.navigation.TramoNavHost
import com.luis.tramo.ui.theme.TramoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var seeder: DatabaseSeeder

    // Latest launch intent as Compose state, so a widget tap navigates on both a cold start and
    // onNewIntent (app already running). Reading the Activity's `intent` directly wouldn't recompose.
    private val launchIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeSeed()
        enableEdgeToEdge()
        launchIntent.value = intent
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()
            val darkOverride by mainViewModel.darkModeOverride.collectAsStateWithLifecycle()
            TramoTheme(darkTheme = darkOverride ?: isSystemInDarkTheme()) {
                startDestination?.let { destination ->
                    val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    val debugScreen = if (debuggable) intent.getStringExtra("screen") else null
                    val navController = rememberNavController()
                    TramoNavHost(
                        startDestination = debugScreen ?: destination,
                        navController = navController
                    )
                    val current by launchIntent
                    LaunchedEffect(current, destination) {
                        val widgetDestination = current?.getStringExtra(EXTRA_DESTINATION)
                        val highlight = current?.getStringExtra(EXTRA_HIGHLIGHT)
                        if (widgetDestination == TramoDestinations.SETTINGS) {
                            val route = if (highlight != null) {
                                "${TramoDestinations.SETTINGS}?highlight=$highlight"
                            } else TramoDestinations.SETTINGS
                            navController.navigate(route) { launchSingleTop = true }
                            current?.removeExtra(EXTRA_DESTINATION)
                            current?.removeExtra(EXTRA_HIGHLIGHT)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchIntent.value = intent
    }

    private fun maybeSeed() {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable && intent.getBooleanExtra("seed", false)) {
            lifecycleScope.launch {
                seeder.seed()
                Toast.makeText(this@MainActivity, "DB seeded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "tramo_destination"
        const val EXTRA_HIGHLIGHT = "tramo_highlight"
    }
}