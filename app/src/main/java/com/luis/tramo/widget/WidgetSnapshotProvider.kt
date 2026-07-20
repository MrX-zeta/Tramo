package com.luis.tramo.widget

import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.data.session.DayCount
import com.luis.tramo.data.session.SessionRepository
import com.luis.tramo.data.session.computeCurrentStreak
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** The numbers the focus-ring widget renders. Its default is a valid empty/zero state. */
data class WidgetSnapshot(
    val sessionsToday: Int = 0,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val streak: Int = 0,
    /** Focus seconds accumulated today; only the extra-large widget shows it. */
    val focusSecondsToday: Int = 0,
    /** Focus-session counts for the current week, Monday → Sunday. Size 7, or empty when unread. */
    val weekCounts: List<Int> = emptyList(),
    /** Localized day initials aligned with [weekCounts] (Monday → Sunday). Empty when unread. */
    val weekLabels: List<String> = emptyList()
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
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        // Anchor the strip to the current calendar week (Monday → Sunday) so the initials always read
        // in week order (e.g. fr "L M M J V S D") instead of a rolling window starting mid-week.
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val weekStart = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        // dailyGoal + languageTag pre-combined into one flow so the outer combine stays at 5 args
        // (its typed-overload limit). The language comes from the value the user picked in Settings,
        // persisted in DataStore — the widget context's own locale isn't a reliable source.
        val configFlow = combine(preferences.dailyGoal, preferences.languageTag) { goal, tag -> goal to tag }
        return combine(
            sessions.focusCountSince(todayStart),
            configFlow,
            sessions.focusDayStamps(),
            sessions.focusSecondsSince(todayStart),
            sessions.focusCountsByDay(weekStart)
        ) { count, config, dayStamps, focusSeconds, weekRows ->
            val (goal, languageTag) = config
            WidgetSnapshot(
                sessionsToday = count,
                dailyGoal = goal.coerceAtLeast(1),
                streak = computeCurrentStreak(dayStamps, today),
                focusSecondsToday = focusSeconds,
                weekCounts = weekCountsFromMonday(weekRows, monday),
                weekLabels = weekDayInitials(monday, localeFor(languageTag))
            )
        }
    }

    /** Session counts into 7 slots, Monday → Sunday of the current week; future days stay 0. */
    private fun weekCountsFromMonday(rows: List<DayCount>, monday: LocalDate): List<Int> {
        val buckets = IntArray(7)
        rows.forEach { row ->
            val date = runCatching { LocalDate.parse(row.day) }.getOrNull() ?: return@forEach
            val index = ChronoUnit.DAYS.between(monday, date).toInt()
            if (index in 0..6) buckets[index] = row.count
        }
        return buckets.toList()
    }

    /** Blank tag = follow the system locale; otherwise the explicit BCP-47 tag the user chose. */
    private fun localeFor(tag: String): Locale =
        if (tag.isBlank()) Locale.getDefault() else Locale.forLanguageTag(tag)

    /**
     * The 7 day initials for the current week, Monday → Sunday, in the narrow localized form
     * (en "M T W…", es "L M M J V S D", fr "L M M J V S D", ja "月 火 水…"), so every supported
     * language works with no per-language string resources.
     */
    private fun weekDayInitials(monday: LocalDate, locale: Locale): List<String> =
        (0..6).map { i ->
            val day = monday.plusDays(i.toLong()).dayOfWeek
            val narrow = day.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
            // Spanish narrows miércoles as "X" (to differ from martes' "M"); the user prefers "M".
            if (locale.language == "es" && day == DayOfWeek.WEDNESDAY) "M" else narrow
        }
}
