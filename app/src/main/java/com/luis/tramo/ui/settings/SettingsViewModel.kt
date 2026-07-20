package com.luis.tramo.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.data.session.SessionRepository
import com.luis.tramo.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // Summary (Room-derived).
    val taskCount: Int = 0,
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    // Timer durations.
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    // Preferences.
    val darkOverride: Boolean? = null,
    val keepScreenOn: Boolean = false,
    val autoStartBreaks: Boolean = true,
    val autoStartNextFocus: Boolean = false,
    val soundVibration: Boolean = true,
    val dailyGoal: Int = 8,
    val languageTag: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val widgetUpdater: com.luis.tramo.widget.WidgetUpdater,
    private val widgetPinner: com.luis.tramo.widget.WidgetPinner,
    taskRepository: TaskRepository,
    sessionRepository: SessionRepository
) : ViewModel() {

    private data class Summary(val tasks: Int, val sessions: Int, val minutes: Int)
    private data class TimerCfg(val focus: Int, val breakM: Int, val longBreak: Int, val sessionsBeforeLong: Int)
    private data class Toggles(val dark: Boolean?, val keep: Boolean, val autoBreaks: Boolean, val autoFocus: Boolean, val sound: Boolean)
    private data class Misc(val dailyGoal: Int, val language: String)

    private val summaryFlow = combine(
        taskRepository.taskCount(),
        sessionRepository.totalFocusCount(),
        sessionRepository.totalFocusSeconds()
    ) { tasks, sessions, seconds -> Summary(tasks, sessions, seconds / 60) }

    private val timerFlow = combine(
        preferences.focusPresetMinutes,
        preferences.breakPresetMinutes,
        preferences.longBreakMinutes,
        preferences.sessionsBeforeLongBreak
    ) { focus, breakM, longBreak, before -> TimerCfg(focus, breakM, longBreak, before) }

    private val togglesFlow = combine(
        preferences.darkModeOverride,
        preferences.keepScreenOn,
        preferences.autoStartBreaks,
        preferences.autoStartNextFocus,
        preferences.soundVibrationOnFinish
    ) { dark, keep, autoBreaks, autoFocus, sound -> Toggles(dark, keep, autoBreaks, autoFocus, sound) }

    private val miscFlow = combine(
        preferences.dailyGoal,
        preferences.languageTag
    ) { goal, language -> Misc(goal, language) }

    val uiState: StateFlow<SettingsUiState> = combine(
        summaryFlow, timerFlow, togglesFlow, miscFlow
    ) { summary, timer, toggles, misc ->
        SettingsUiState(
            taskCount = summary.tasks,
            totalSessions = summary.sessions,
            totalMinutes = summary.minutes,
            focusMinutes = timer.focus,
            breakMinutes = timer.breakM,
            longBreakMinutes = timer.longBreak,
            sessionsBeforeLongBreak = timer.sessionsBeforeLong,
            darkOverride = toggles.dark,
            keepScreenOn = toggles.keep,
            autoStartBreaks = toggles.autoBreaks,
            autoStartNextFocus = toggles.autoFocus,
            soundVibration = toggles.sound,
            dailyGoal = misc.dailyGoal,
            languageTag = misc.language
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    // --- Every setter writes to DataStore immediately (no Save button). ---

    fun setFocusPreset(minutes: Int) = viewModelScope.launch { preferences.setFocusPreset(minutes) }

    fun setBreakPreset(minutes: Int) = viewModelScope.launch {
        preferences.setBreakPreset(minutes)
        // Invariant: long break >= short break. Auto-raise long break if this made it invalid.
        val currentLong = preferences.longBreakMinutes.first()
        if (currentLong < minutes) {
            preferences.setLongBreakMinutes(lowestLongBreakAtLeast(minutes))
        }
    }

    fun setLongBreak(minutes: Int) = viewModelScope.launch {
        // Never persist a long break below the short break (the UI also disables those chips).
        val shortBreak = preferences.breakPresetMinutes.first()
        preferences.setLongBreakMinutes(maxOf(minutes, shortBreak))
    }

    private fun lowestLongBreakAtLeast(minutes: Int): Int =
        LONG_BREAK_OPTIONS.firstOrNull { it >= minutes } ?: LONG_BREAK_OPTIONS.last()
    fun setDarkOverride(value: Boolean?) = viewModelScope.launch { preferences.setDarkModeOverride(value) }
    fun setKeepScreenOn(value: Boolean) = viewModelScope.launch { preferences.setKeepScreenOn(value) }
    fun setAutoStartBreaks(value: Boolean) = viewModelScope.launch { preferences.setAutoStartBreaks(value) }
    fun setAutoStartNextFocus(value: Boolean) = viewModelScope.launch { preferences.setAutoStartNextFocus(value) }
    fun setSoundVibration(value: Boolean) = viewModelScope.launch { preferences.setSoundVibrationOnFinish(value) }

    fun setDailyGoal(goal: Int) = viewModelScope.launch {
        preferences.setDailyGoal(goal.coerceIn(MIN_GOAL, MAX_GOAL))
        // Fire-and-forget: WidgetUpdater debounces the burst and pushes one final, MIUI-safe update.
        widgetUpdater.refresh()
    }

    fun setSessionsBeforeLongBreak(count: Int) = viewModelScope.launch {
        preferences.setSessionsBeforeLongBreak(count.coerceIn(MIN_SESSIONS_BEFORE_LONG, MAX_SESSIONS_BEFORE_LONG))
    }

    /** Queried on composition: a launcher without pin support gets a manual hint, not a button. */
    fun isWidgetPinSupported(): Boolean = widgetPinner.isPinSupported()

    /** Launches the system pin dialog, or reports that the widget is already placed. */
    fun pinWidget(): com.luis.tramo.widget.PinResult = widgetPinner.requestPin()

    fun setLanguage(tag: String) {
        val locales = if (tag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
        viewModelScope.launch { preferences.setLanguageTag(tag) }
    }

    companion object {
        const val MIN_GOAL = 1
        const val MAX_GOAL = 12
        const val MIN_SESSIONS_BEFORE_LONG = 2
        const val MAX_SESSIONS_BEFORE_LONG = 6

        val FOCUS_OPTIONS = listOf(5, 10, 25)
        val BREAK_OPTIONS = listOf(5, 10, 15)
        val LONG_BREAK_OPTIONS = listOf(15, 20, 30)
    }
}
