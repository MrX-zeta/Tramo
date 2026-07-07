package com.luis.tramo.timer

enum class TimerStatus { IDLE, RUNNING, PAUSED, FINISHED }

/**
 * Snapshot of the running timer, shared by the service, the UI and the Glance widget.
 */
data class TimerState(
    val sessionType: SessionType = SessionType.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val remainingSeconds: Int = SessionType.FOCUS.durationSeconds,
    val totalSeconds: Int = SessionType.FOCUS.durationSeconds
) {
    /** 0f..1f fraction of the session already elapsed. */
    val progress: Float
        get() = if (totalSeconds <= 0) 0f else 1f - remainingSeconds.toFloat() / totalSeconds

    val isRunning: Boolean get() = status == TimerStatus.RUNNING

    companion object {
        fun forSession(type: SessionType) = TimerState(
            sessionType = type,
            status = TimerStatus.IDLE,
            remainingSeconds = type.durationSeconds,
            totalSeconds = type.durationSeconds
        )
    }
}

/** Formats a seconds value as mm:ss. */
fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}
