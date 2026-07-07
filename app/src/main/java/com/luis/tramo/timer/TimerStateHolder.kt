package com.luis.tramo.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide single source of truth for the timer. Written by [PomodoroTimerService],
 * observed by the UI ([com.luis.tramo.ui.timer.TimerViewModel]) and read as a snapshot by
 * the Glance widget.
 */
@Singleton
class TimerStateHolder @Inject constructor() {

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    fun set(state: TimerState) {
        _state.value = state
    }

    fun update(transform: (TimerState) -> TimerState) {
        _state.value = transform(_state.value)
    }
}
