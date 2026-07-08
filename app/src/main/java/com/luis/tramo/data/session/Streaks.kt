package com.luis.tramo.data.session

import java.time.LocalDate

/**
 * The current focus streak: consecutive days (ending on [today], or on yesterday when today has no
 * session yet) that have at least one completed focus session. Pure domain logic, shared by the
 * Timer screen and the home-screen widget so the count is computed in one place, never in the UI.
 */
fun computeCurrentStreak(dayStamps: List<String>, today: LocalDate): Int {
    val days = dayStamps.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
    if (days.isEmpty()) return 0
    var cursor = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (cursor in days) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}
