package com.luis.tramo.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.luis.tramo.R

/** The three bottom-navigation destinations. Settings is intentionally NOT here — it's a top-bar action. */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    TIMER(TramoDestinations.TIMER, R.string.nav_timer, Icons.Filled.Timer),
    TASKS(TramoDestinations.TASKS, R.string.timer_open_tasks, Icons.AutoMirrored.Filled.List),
    REPORT(TramoDestinations.REPORT, R.string.timer_open_report, Icons.Filled.BarChart)
}

@Composable
fun TramoBottomBar(
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                label = { Text(stringResource(destination.labelRes)) }
            )
        }
    }
}

/** Shared LargeTopAppBar for the three main screens, with the gear (Settings) action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TramoLargeTopBar(
    @StringRes titleRes: Int,
    onOpenSettings: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    LargeTopAppBar(
        title = { Text(stringResource(titleRes)) },
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
            }
        },
        scrollBehavior = scrollBehavior
    )
}
