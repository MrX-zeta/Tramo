package com.luis.tramo.widget

import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.data.session.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** The three numbers the focus-ring widget renders. Its default is a valid empty/zero state. */
data class WidgetSnapshot(
    val sessionsToday: Int = 0,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val streak: Int = 0
) {
    /** Ring fill fraction, clamped to 0..1 (guarded against a zero goal). */
    val progress: Float get() = if (dailyGoal > 0) (sessionsToday.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f

    companion object {
        const val DEFAULT_DAILY_GOAL = 8
    }
}

/**
 * Snapshot read of the widget's data from the same repositories the app uses. All computation lives
 * in the domain layer ([SessionRepository]); this only reads. Each query resolves immediately, so
 * the widget never blocks waiting for an unbounded flow.
 */
@Singleton
class WidgetSnapshotProvider @Inject constructor(
    private val sessions: SessionRepository,
    private val preferences: UserPreferencesRepository
) {
    suspend fun load(): WidgetSnapshot = WidgetSnapshot(
        sessionsToday = sessions.focusCountToday(),
        dailyGoal = preferences.dailyGoal.first().coerceAtLeast(1),
        streak = sessions.currentStreak()
    )
}
