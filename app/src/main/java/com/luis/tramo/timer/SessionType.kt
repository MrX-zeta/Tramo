package com.luis.tramo.timer

import androidx.annotation.StringRes
import com.luis.tramo.R

/**
 * The three Pomodoro session types with their default durations (in seconds).
 */
enum class SessionType(
    val durationSeconds: Int,
    @param:StringRes val labelRes: Int
) {
    FOCUS(25 * 60, R.string.session_focus),
    SHORT_BREAK(5 * 60, R.string.session_short_break),
    LONG_BREAK(15 * 60, R.string.session_long_break);

    val isBreak: Boolean get() = this != FOCUS
}

/** Number of focus sessions before a long break replaces the short one. */
const val SESSIONS_PER_LONG_BREAK = 4
