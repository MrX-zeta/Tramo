package com.luis.tramo

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luis.tramo.data.DatabaseSeeder
import com.luis.tramo.navigation.TramoNavHost
import com.luis.tramo.ui.theme.TramoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var seeder: DatabaseSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeSeed()
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

    /** Debug-only DB seed, triggered via `adb ... --ez seed true`; ignored on release builds. */
    private fun maybeSeed() {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable && intent.getBooleanExtra("seed", false)) {
            lifecycleScope.launch {
                seeder.seed()
                Toast.makeText(this@MainActivity, "DB seeded", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
