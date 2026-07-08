package com.luis.tramo.widget

import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.data.session.SessionRepository
import com.luis.tramo.data.session.computeCurrentStreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
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

    /** True once today's sessions meet or exceed the goal — the ring turns amber to signal it. */
    val goalReached: Boolean get() = dailyGoal > 0 && sessionsToday >= dailyGoal

    companion object {
        const val DEFAULT_DAILY_GOAL = 8
    }
}

/**
 * The widget's data from the same repositories the app uses. All computation lives in the domain
 * layer ([SessionRepository], [computeCurrentStreak]); this only reads.
 *
 * Exposed as a REACTIVE [Flow]: with `provideContent`, Glance keeps the widget's composition alive
 * and merely recomposes it, so a value read once (before `provideContent`) would freeze — a later
 * `updateAll` only recomposes the content lambda and never re-runs the read. By collecting this
 * flow inside the composition, every change (a completed session, a new daily goal) re-emits and
 * repaints the widget in real time while the process is alive.
 */
@Singleton
class WidgetSnapshotProvider @Inject constructor(
    private val sessions: SessionRepository,
    private val preferences: UserPreferencesRepository
) {
    fun snapshotFlow(): Flow<WidgetSnapshot> {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return combine(
            sessions.focusCountSince(todayStart),
            preferences.dailyGoal,
            sessions.focusDayStamps()
        ) { count, goal, dayStamps ->
            WidgetSnapshot(
                sessionsToday = count,
                dailyGoal = goal.coerceAtLeast(1),
                streak = computeCurrentStreak(dayStamps, LocalDate.now())
            )
        }
    }
}
