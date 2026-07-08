package com.luis.tramo.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luis.tramo.ui.components.ScreenEntrance
import com.luis.tramo.ui.components.rememberReduceMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R
import com.luis.tramo.ui.theme.Spacing
import com.luis.tramo.ui.theme.TabularFigures

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    highlight: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()
    var visible by remember { mutableStateOf(false) }
    val goalPulse = remember { Animatable(0f) }
    LaunchedEffect(highlight) {
        if (highlight == "daily_goal") {
            goalPulse.snapTo(0f)
            // Let the card's entrance animation settle before drawing the eye to the goal row.
            kotlinx.coroutines.delay(500)
            repeat(3) {
                goalPulse.animateTo(1f, tween(220))
                goalPulse.animateTo(0f, tween(220))
            }
        }
    }
    LaunchedEffect(Unit) { visible = true }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Header(onBack = onBack)
            ScreenEntrance(index = 0, visible = visible, reduceMotion = reduceMotion) {
                SummaryCard(tasks = state.taskCount, sessions = state.totalSessions, minutes = state.totalMinutes)
            }
            ScreenEntrance(index = 1, visible = visible, reduceMotion = reduceMotion) {
                TimerSettingsCard(state = state, viewModel = viewModel)
            }
            ScreenEntrance(index = 2, visible = visible, reduceMotion = reduceMotion) {
                PreferencesCard(
                    state = state,
                    viewModel = viewModel,
                    onOpenLanguage = { showLanguageDialog = true },
                    goalHighlight = goalPulse.value
                )
            }
        }
    }

    if (showLanguageDialog) {
        LocalePickerSheet(
            currentTag = state.languageTag,
            onSelect = {
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.settings_close))
        }
    }
}

@Composable
private fun SummaryCard(tasks: Int, sessions: Int, minutes: Int) {
    ElevatedCard(shape = RoundedCornerShape(Spacing.xl)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xl, horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryColumn(tasks, stringResource(R.string.settings_summary_tasks))
            SummaryColumn(sessions, stringResource(R.string.settings_summary_sessions))
            SummaryColumn(minutes, stringResource(R.string.settings_summary_minutes))
        }
    }
}

@Composable
private fun SummaryColumn(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium.merge(TabularFigures),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(
    eyebrow: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.padding(start = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
        ElevatedCard(shape = RoundedCornerShape(Spacing.lg)) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                content = content
            )
        }
    }
}

@Composable
private fun TimerSettingsCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard(
        eyebrow = stringResource(R.string.settings_timer_eyebrow),
        icon = Icons.Filled.Timer
    ) {
        ChipRow(
            dotColor = MaterialTheme.colorScheme.primary,
            label = stringResource(R.string.settings_focus_minutes),
            options = SettingsViewModel.FOCUS_OPTIONS,
            selected = state.focusMinutes,
            onSelect = viewModel::setFocusPreset
        )
        ChipRow(
            dotColor = MaterialTheme.colorScheme.secondary,
            label = stringResource(R.string.settings_break_minutes),
            options = SettingsViewModel.BREAK_OPTIONS,
            selected = state.breakMinutes,
            onSelect = viewModel::setBreakPreset
        )
        ChipRow(
            dotColor = MaterialTheme.colorScheme.tertiary,
            label = stringResource(R.string.settings_long_break),
            options = SettingsViewModel.LONG_BREAK_OPTIONS,
            selected = state.longBreakMinutes,
            onSelect = viewModel::setLongBreak,
            // Long break must be >= the short break; dim/disable any lower value.
            minEnabled = state.breakMinutes
        )
    }
}

@Composable
private fun ChipRow(
    dotColor: Color,
    label: String,
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    minEnabled: Int = 0
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(Modifier.width(Spacing.md))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    enabled = option >= minEnabled,
                    onClick = { onSelect(option) },
                    label = { Text(option.toString(), style = TabularFigures.merge(MaterialTheme.typography.labelLarge)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
private fun PreferencesCard(
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onOpenLanguage: () -> Unit,
    goalHighlight: Float = 0f
) {
    val context = LocalContext.current
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val systemDark = isSystemInDarkTheme()

    SectionCard(
        eyebrow = stringResource(R.string.settings_prefs_eyebrow),
        icon = Icons.Filled.Tune
    ) {
        PreferenceRow(
            icon = Icons.Filled.Notifications,
            title = stringResource(R.string.settings_notifications_title),
            subtitle = stringResource(R.string.settings_notifications_subtitle),
            onClick = {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                context.startActivity(intent)
            }
        ) {
            ValueChevron(
                value = stringResource(
                    if (notificationsEnabled) R.string.settings_notifications_enabled
                    else R.string.settings_notifications_disabled
                )
            )
        }

        PreferenceRow(
            icon = Icons.Filled.Language,
            title = stringResource(R.string.settings_language),
            subtitle = stringResource(R.string.settings_language_subtitle),
            onClick = onOpenLanguage
        ) {
            ValueChevron(value = localeLabelFor(state.languageTag))
        }

        PreferenceRow(
            icon = Icons.Filled.DarkMode,
            title = stringResource(R.string.settings_dark_mode),
            subtitle = stringResource(R.string.settings_dark_subtitle)
        ) {
            Switch(
                checked = state.darkOverride ?: systemDark,
                onCheckedChange = { viewModel.setDarkOverride(it) }
            )
        }

        PreferenceRow(
            icon = Icons.Filled.Smartphone,
            title = stringResource(R.string.settings_keep_screen_title),
            subtitle = stringResource(R.string.settings_keep_screen_subtitle)
        ) {
            Switch(checked = state.keepScreenOn, onCheckedChange = { viewModel.setKeepScreenOn(it) })
        }

        PreferenceRow(
            icon = Icons.Filled.Flag,
            title = stringResource(R.string.settings_daily_goal),
            subtitle = stringResource(R.string.settings_daily_goal_subtitle),
            highlight = goalHighlight
        ) {
            Stepper(
                value = state.dailyGoal,
                onDecrement = { viewModel.setDailyGoal(state.dailyGoal - 1) },
                onIncrement = { viewModel.setDailyGoal(state.dailyGoal + 1) }
            )
        }

        PreferenceRow(
            icon = Icons.Filled.Coffee,
            title = stringResource(R.string.settings_auto_breaks_title),
            subtitle = null
        ) {
            Switch(checked = state.autoStartBreaks, onCheckedChange = { viewModel.setAutoStartBreaks(it) })
        }

        PreferenceRow(
            icon = Icons.Filled.PlayArrow,
            title = stringResource(R.string.settings_auto_focus_title),
            subtitle = null
        ) {
            Switch(checked = state.autoStartNextFocus, onCheckedChange = { viewModel.setAutoStartNextFocus(it) })
        }

        PreferenceRow(
            icon = Icons.Filled.Repeat,
            title = stringResource(R.string.settings_sessions_before_long_title),
            subtitle = null
        ) {
            Stepper(
                value = state.sessionsBeforeLongBreak,
                onDecrement = { viewModel.setSessionsBeforeLongBreak(state.sessionsBeforeLongBreak - 1) },
                onIncrement = { viewModel.setSessionsBeforeLongBreak(state.sessionsBeforeLongBreak + 1) }
            )
        }

        PreferenceRow(
            icon = Icons.Filled.VolumeUp,
            title = stringResource(R.string.settings_sound_title),
            subtitle = null
        ) {
            Switch(checked = state.soundVibration, onCheckedChange = { viewModel.setSoundVibration(it) })
        }
    }
}

@Composable
private fun PreferenceRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null,
    highlight: Float = 0f,
    trailing: @Composable () -> Unit
) {
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind {
                if (highlight > 0f) {
                    val r = Spacing.sm.toPx()
                    drawRoundRect(
                        color = highlightColor,
                        alpha = highlight.coerceIn(0f, 1f),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
            }
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        trailing()
    }
}

@Composable
private fun ValueChevron(value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Stepper(value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconButton(onClick = onDecrement, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = null)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium.merge(TabularFigures),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = Spacing.md)
        )
        OutlinedIconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
}
