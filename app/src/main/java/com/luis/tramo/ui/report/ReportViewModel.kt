package com.luis.tramo.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.session.DayCount
import com.luis.tramo.data.session.HourlyFocus
import com.luis.tramo.data.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class ReportRange(val days: Long) { WEEKLY(7), MONTHLY(30) }

/** One day cell of the activity heatmap. [level] is a 0..4 intensity bucket. */
data class HeatmapCell(val date: LocalDate, val count: Int, val level: Int)

data class HeatmapUiState(
    val cells: List<HeatmapCell> = emptyList(),
    val totalCompletions: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

data class ReportUiState(
    val range: ReportRange = ReportRange.WEEKLY,
    val focusSeconds: Int = 0,
    val breakSeconds: Int = 0,
    val sessionCount: Int = 0,
    val avgSessionSeconds: Int = 0,
    /** 24 buckets (hour 0..23) of focus minutes for the selected range. */
    val hourlyMinutes: List<Int> = List(24) { 0 }
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val todayStart: Long = startOfDay(0)

    private val _range = MutableStateFlow(ReportRange.WEEKLY)
    val range: StateFlow<ReportRange> = _range.asStateFlow()

    private val hourlyFlow = _range.flatMapLatest { range ->
        repository.focusIntensityByHour(startOfDay(range.days - 1))
    }

    val uiState: StateFlow<ReportUiState> = combine(
        _range,
        repository.focusSecondsSince(todayStart),
        repository.breakSecondsSince(todayStart),
        repository.focusCountSince(todayStart),
        hourlyFlow
    ) { range, focus, breaks, count, hourly ->
        ReportUiState(
            range = range,
            focusSeconds = focus,
            breakSeconds = breaks,
            sessionCount = count,
            avgSessionSeconds = if (count > 0) focus / count else 0,
            hourlyMinutes = toHourlyMinutes(hourly)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    // 12-week window starting on the Monday 11 weeks before the current week.
    private val heatmapStartMonday: LocalDate =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(11)

    /**
     * Heatmap cells + lifetime stats. The cell/level (color) computation runs off the main
     * thread via [flowOn] on [Dispatchers.Default].
     */
    val heatmapState: StateFlow<HeatmapUiState> = combine(
        repository.focusCountsByDay(heatmapStartMonday.atStartOfDay(zone).toInstant().toEpochMilli()),
        repository.totalFocusCount(),
        repository.longestStreak(),
        repository.focusDayStamps()
    ) { dayCounts, total, longest, stamps ->
        HeatmapUiState(
            cells = buildCells(dayCounts),
            totalCompletions = total,
            currentStreak = computeCurrentStreak(stamps),
            longestStreak = longest
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatmapUiState())

    fun selectRange(range: ReportRange) {
        _range.value = range
    }

    /** Builds the 84 cells ordered weekday-major so a 12-column grid renders 7 rows × 12 weeks. */
    private fun buildCells(dayCounts: List<DayCount>): List<HeatmapCell> {
        val countByDay = dayCounts.mapNotNull { row ->
            runCatching { LocalDate.parse(row.day) to row.count }.getOrNull()
        }.toMap()
        val cells = ArrayList<HeatmapCell>(WEEKS * 7)
        for (weekday in 0 until 7) {
            for (week in 0 until WEEKS) {
                val date = heatmapStartMonday.plusWeeks(week.toLong()).plusDays(weekday.toLong())
                val count = countByDay[date] ?: 0
                cells.add(HeatmapCell(date, count, levelFor(count)))
            }
        }
        return cells
    }

    private fun levelFor(count: Int): Int = when {
        count <= 0 -> 0
        count == 1 -> 1
        count <= 3 -> 2
        count <= 5 -> 3
        else -> 4
    }

    private fun computeCurrentStreak(dayStamps: List<String>): Int {
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

    /** Epoch millis for the start of the day [daysAgo] days before today (local time). */
    private fun startOfDay(daysAgo: Long): Long =
        LocalDate.now().minusDays(daysAgo).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun toHourlyMinutes(rows: List<HourlyFocus>): List<Int> {
        val buckets = IntArray(24)
        rows.forEach { row ->
            if (row.hour in 0..23) buckets[row.hour] = row.totalSeconds / 60
        }
        return buckets.toList()
    }

    private companion object {
        const val WEEKS = 12
    }
}
