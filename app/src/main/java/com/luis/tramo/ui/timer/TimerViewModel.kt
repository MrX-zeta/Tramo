package com.luis.tramo.ui.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.UserPreferencesRepository
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
import javax.inject.Inject

data class TimerUiState(
    val sessionType: SessionType = SessionType.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val timeText: String = formatTime(SessionType.FOCUS.durationSeconds),
    val progress: Float = 0f,
    val completedFocusToday: Int = 0
) {
    val isRunning: Boolean get() = status == TimerStatus.RUNNING
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val stateHolder: TimerStateHolder,
    preferences: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<TimerUiState> =
        combine(stateHolder.state, preferences.sessionsToday) { timer, count ->
            timer.toUiState(count)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerUiState()
        )

    init {
        // While idle on a focus session, reflect the current preset so a change made in Settings
        // is visible immediately (the running session is never altered mid-way).
        viewModelScope.launch {
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

    fun onPlayPause() {
        val running = uiState.value.status == TimerStatus.RUNNING
        send(if (running) PomodoroTimerService.ACTION_PAUSE else PomodoroTimerService.ACTION_START)
    }

    fun onSkip() = send(PomodoroTimerService.ACTION_SKIP)

    fun onStop() = send(PomodoroTimerService.ACTION_STOP)

    private fun send(action: String) = PomodoroTimerService.sendAction(context, action)

    private fun TimerState.toUiState(count: Int) = TimerUiState(
        sessionType = sessionType,
        status = status,
        timeText = formatTime(remainingSeconds),
        progress = progress,
        completedFocusToday = count
    )
}
