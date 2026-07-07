package com.luis.tramo.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.session.DailyFocus
import com.luis.tramo.data.session.DayCount
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

enum class ReportRange(val days: Long) { WEEKLY(7), MONTHLY(30) }

/** One day cell of the activity heatmap. [level] is a 0..4 intensity bucket. */
data class HeatmapCell(val date: LocalDate, val count: Int, val level: Int)

data class HeatmapUiState(
    val cells: List<HeatmapCell> = emptyList(),
    val totalCompletions: Int = 0,
    val activeDays: Int = 0
)

data class ReportUiState(
    val range: ReportRange = ReportRange.WEEKLY,
    /** One focus-minutes value per day across the selected range (oldest → today). */
    val dailyMinutes: List<Int> = emptyList(),
    /** Non-blank x-axis labels for [dailyMinutes]: weekday narrows (weekly) or day-of-month (monthly). */
    val dailyLabels: List<String> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    private val _range = MutableStateFlow(ReportRange.WEEKLY)
    val range: StateFlow<ReportRange> = _range.asStateFlow()

    val uiState: StateFlow<ReportUiState> = _range.flatMapLatest { range ->
        repository.focusMinutesByDay(startOfDay(range.days - 1)).map { daily ->
            val days = range.days.toInt()
            ReportUiState(
                range = range,
                dailyMinutes = toDailyMinutes(daily, days),
                dailyLabels = buildDailyLabels(days)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    // 12-week window starting on the Monday 11 weeks before the current week.
    private val heatmapStartMonday: LocalDate =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(11)

    /** Heatmap cells + summary KPIs. The cell/level computation runs off the main thread. */
    val heatmapState: StateFlow<HeatmapUiState> = combine(
        repository.focusCountsByDay(heatmapStartMonday.atStartOfDay(zone).toInstant().toEpochMilli()),
        repository.totalFocusCount(),
        repository.focusDayStamps()
    ) { dayCounts, total, stamps ->
        HeatmapUiState(
            cells = buildCells(dayCounts),
            totalCompletions = total,
            activeDays = stamps.size
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatmapUiState())

    fun selectRange(range: ReportRange) {
        _range.value = range
    }

    /**
     * Bucket-count-aware x-axis labels — always non-blank (Vico rejects blank formatter output).
     * Weekly → localized weekday narrows; monthly → day-of-month numbers (the axis thins them).
     */
    private fun buildDailyLabels(days: Int): List<String> {
        val today = LocalDate.now()
        val locale = Locale.getDefault()
        return (0 until days).map { i ->
            val date = today.minusDays((days - 1 - i).toLong())
            if (days <= 7) {
                date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
            } else {
                date.dayOfMonth.toString()
            }
        }
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

    /** Epoch millis for the start of the day [daysAgo] days before today (local time). */
    private fun startOfDay(daysAgo: Long): Long =
        LocalDate.now().minusDays(daysAgo).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Builds one focus-minutes value per day, oldest → today, for a [days]-long window. */
    private fun toDailyMinutes(rows: List<DailyFocus>, days: Int): List<Int> {
        val today = LocalDate.now()
        val buckets = IntArray(days)
        rows.forEach { row ->
            val date = runCatching { LocalDate.parse(row.day) }.getOrNull() ?: return@forEach
            val daysAgo = ChronoUnit.DAYS.between(date, today).toInt()
            if (daysAgo in 0 until days) buckets[days - 1 - daysAgo] = row.totalSeconds / 60
        }
        return buckets.toList()
    }

    private companion object {
        const val WEEKS = 12
    }
}
