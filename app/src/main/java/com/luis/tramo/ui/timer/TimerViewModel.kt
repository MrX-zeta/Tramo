package com.luis.tramo.ui.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.data.session.SessionRepository
import com.luis.tramo.data.session.computeCurrentStreak
import com.luis.tramo.data.task.TaskEntity
import com.luis.tramo.data.task.TaskRepository
import com.luis.tramo.timer.PomodoroTimerService
import com.luis.tramo.timer.SessionType
import com.luis.tramo.timer.TimerState
import com.luis.tramo.timer.TimerStateHolder
import com.luis.tramo.timer.TimerStatus
import com.luis.tramo.timer.formatTime
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class TaskPreview(val emoji: String, val title: String)

/** Payload for the daily-goal celebration: the goal hit, sessions done today, and current streak. */
data class CelebrationInfo(val goal: Int, val sessions: Int, val streak: Int)

data class TimerUiState(
    val sessionType: SessionType = SessionType.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val timeText: String = formatTime(SessionType.FOCUS.durationSeconds),
    val progress: Float = 0f,
    val completedFocusToday: Int = 0,
    val streak: Int = 0,
    val todaysTasks: List<TaskPreview> = emptyList(),
    /** 7 flags for the current week (locale order) — true where a focus session was completed. */
    val weekDots: List<Boolean> = List(7) { false },
    /** True only when the "keep screen on" preference is enabled AND a session is running. */
    val keepScreenOn: Boolean = false
) {
    val isRunning: Boolean get() = status == TimerStatus.RUNNING
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val stateHolder: TimerStateHolder,
    private val preferences: UserPreferencesRepository,
    sessionRepository: SessionRepository,
    taskRepository: TaskRepository
) : ViewModel() {

    // Local-midnight boundary; "today" is derived from Room timestamps, not an in-memory counter.
    private val todayStart: Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val uiState: StateFlow<TimerUiState> =
        combine(
            stateHolder.state,
            sessionRepository.focusCountSince(todayStart),
            sessionRepository.focusDayStamps(),
            taskRepository.activeTasks(),
            preferences.keepScreenOn
        ) { timer, count, dayStamps, tasks, keepScreenOn ->
            timer.toUiState(
                count = count,
                streak = computeCurrentStreak(dayStamps, LocalDate.now()),
                tasks = tasks.take(TASK_PREVIEW_LIMIT).map { it.toPreview() },
                weekDots = computeWeekDots(dayStamps),
                keepScreenOn = keepScreenOn && timer.status == TimerStatus.RUNNING
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerUiState()
        )

    /**
     * Non-null when today's focus count has reached the goal and it hasn't been celebrated yet
     * today. Reactive, so it fires whether the goal-reaching session finishes with the app open OR
     * the user simply returns to the app afterwards; [markGoalCelebrated] flips it off for the day.
     */
    val celebration: StateFlow<CelebrationInfo?> =
        combine(
            sessionRepository.focusCountSince(todayStart),
            preferences.dailyGoal,
            sessionRepository.focusDayStamps(),
            preferences.lastGoalCelebratedDate
        ) { count, goal, dayStamps, celebratedDate ->
            if (goal > 0 && count >= goal && celebratedDate != LocalDate.now().toString()) {
                CelebrationInfo(goal = goal, sessions = count, streak = computeCurrentStreak(dayStamps, LocalDate.now()))
            } else {
                null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Records that today's celebration was shown, so it never repeats the same day. */
    fun markGoalCelebrated() {
        viewModelScope.launch { preferences.setLastGoalCelebratedDate(LocalDate.now().toString()) }
    }

    init {
        viewModelScope.launch {
            restoreFromSnapshot()
            // While idle on a focus session, reflect the current preset so a change made in Settings
            // is visible immediately (the running session is never altered mid-way).
            preferences.focusPresetMinutes.collect { minutes ->
                val current = stateHolder.state.value
                val seconds = minutes * 60
                if (current.status == TimerStatus.IDLE &&
                    current.sessionType == SessionType.FOCUS &&
                    current.totalSeconds != seconds
                ) {
                    stateHolder.set(
                        TimerState.forSession(SessionType.FOCUS)
                            .copy(remainingSeconds = seconds, totalSeconds = seconds)
                    )
                }
            }
        }
    }

    /** On cold start, rebuild the running/paused session from the persisted absolute timestamp. */
    private suspend fun restoreFromSnapshot() {
        val active = preferences.getActiveSession() ?: return
        val type = runCatching { SessionType.valueOf(active.sessionType) }.getOrNull() ?: return
        if (active.running) {
            val remaining = ((active.endEpochMillis - System.currentTimeMillis() + 999) / 1000)
                .toInt().coerceAtLeast(0)
            stateHolder.set(TimerState(type, TimerStatus.RUNNING, remaining, active.totalSeconds))
            // The service resumes ticking (or finalizes if the deadline already passed).
            PomodoroTimerService.requestRestore(context)
        } else {
            stateHolder.set(TimerState(type, TimerStatus.PAUSED, active.pausedRemaining, active.totalSeconds))
        }
    }

    fun onPlayPause() {
        val running = uiState.value.status == TimerStatus.RUNNING
        send(if (running) PomodoroTimerService.ACTION_PAUSE else PomodoroTimerService.ACTION_START)
    }

    fun onSkip() = send(PomodoroTimerService.ACTION_SKIP)

    fun onStop() = send(PomodoroTimerService.ACTION_STOP)

    private fun send(action: String) = PomodoroTimerService.sendAction(context, action)

    private fun TimerState.toUiState(
        count: Int,
        streak: Int,
        tasks: List<TaskPreview>,
        weekDots: List<Boolean>,
        keepScreenOn: Boolean
    ) = TimerUiState(
        sessionType = sessionType,
        status = status,
        timeText = formatTime(remainingSeconds),
        progress = progress,
        completedFocusToday = count,
        streak = streak,
        todaysTasks = tasks,
        weekDots = weekDots,
        keepScreenOn = keepScreenOn
    )

    /** Which of the current week's 7 days (locale first-day order) have a completed focus session. */
    private fun computeWeekDots(dayStamps: List<String>): List<Boolean> {
        val days = dayStamps.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        return (0L until 7L).map { weekStart.plusDays(it) in days }
    }

    private fun TaskEntity.toPreview() = TaskPreview(emoji = iconEmoji, title = title)

    private companion object {
        const val TASK_PREVIEW_LIMIT = 3
    }
}
