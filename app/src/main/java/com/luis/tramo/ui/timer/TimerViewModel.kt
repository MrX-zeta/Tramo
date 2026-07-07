package com.luis.tramo.ui.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.data.session.SessionRepository
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
import javax.inject.Inject

data class TaskPreview(val emoji: String, val title: String)

data class TimerUiState(
    val sessionType: SessionType = SessionType.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val timeText: String = formatTime(SessionType.FOCUS.durationSeconds),
    val progress: Float = 0f,
    val completedFocusToday: Int = 0,
    val streak: Int = 0,
    val todaysTasks: List<TaskPreview> = emptyList()
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
            taskRepository.activeTasks()
        ) { timer, count, dayStamps, tasks ->
            timer.toUiState(
                count = count,
                streak = computeStreak(dayStamps),
                tasks = tasks.take(TASK_PREVIEW_LIMIT).map { it.toPreview() }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerUiState()
        )

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

    private fun TimerState.toUiState(count: Int, streak: Int, tasks: List<TaskPreview>) = TimerUiState(
        sessionType = sessionType,
        status = status,
        timeText = formatTime(remainingSeconds),
        progress = progress,
        completedFocusToday = count,
        streak = streak,
        todaysTasks = tasks
    )

    private fun TaskEntity.toPreview() = TaskPreview(emoji = iconEmoji, title = title)

    /** Consecutive days (up to today, or yesterday if today is empty) with a focus session. */
    private fun computeStreak(dayStamps: List<String>): Int {
        val days = dayStamps.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        if (days.isEmpty()) return 0
        val today = LocalDate.now()
        var cursor = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private companion object {
        const val TASK_PREVIEW_LIMIT = 3
    }
}
