package com.luis.tramo.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.session.DailyFocus
import com.luis.tramo.data.session.DayCount
import com.luis.tramo.data.session.MonthCount
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
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    val activeDays: Int = 0,
    /** X-axis: 3-letter month abbreviation at each of the 12 week-columns' month boundaries. */
    val columnLabels: List<String> = emptyList()
)

/** Today's focus summary (card 1). */
data class TodayUiState(
    val focusSeconds: Int = 0,
    val breakSeconds: Int = 0
)

/** The Semanal/Mensual overview: range stats + per-day chart (card 2). */
data class ReportUiState(
    val range: ReportRange = ReportRange.WEEKLY,
    val rangeFocusSeconds: Int = 0,
    val rangeSessionCount: Int = 0,
    val dailyMinutes: List<Int> = emptyList(),
    val dailyLabels: List<String> = emptyList()
) {
    val avgSessionSeconds: Int get() = if (rangeSessionCount > 0) rangeFocusSeconds / rangeSessionCount else 0
}

/** Monthly sessions-per-month chart (card 5). */
data class MonthlyUiState(
    val counts: List<Int> = emptyList(),
    val labels: List<String> = emptyList()
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

    val todayState: StateFlow<TodayUiState> = combine(
        repository.focusSecondsSince(todayStart),
        repository.breakSecondsSince(todayStart)
    ) { focus, breaks -> TodayUiState(focus, breaks) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    val uiState: StateFlow<ReportUiState> = _range.flatMapLatest { range ->
        val start = startOfDay(range.days - 1)
        combine(
            repository.focusMinutesByDay(start),
            repository.focusSecondsSince(start),
            repository.focusCountSince(start)
        ) { daily, focusSeconds, count ->
            val days = range.days.toInt()
            ReportUiState(
                range = range,
                rangeFocusSeconds = focusSeconds,
                rangeSessionCount = count,
                dailyMinutes = toDailyMinutes(daily, days),
                dailyLabels = buildDailyLabels(days)
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    // 10-week window starting on the Monday 9 weeks before the current week — a recent view that
    // keeps the current period prominent (the 12-month bar chart covers the long-term trend).
    private val heatmapStartMonday: LocalDate =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(9)

    val heatmapState: StateFlow<HeatmapUiState> = combine(
        repository.focusCountsByDay(heatmapStartMonday.atStartOfDay(zone).toInstant().toEpochMilli()),
        repository.totalFocusCount(),
        repository.focusDayStamps()
    ) { dayCounts, total, stamps ->
        HeatmapUiState(
            cells = buildCells(dayCounts),
            totalCompletions = total,
            activeDays = stamps.size,
            columnLabels = buildHeatmapColumnLabels()
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatmapUiState())

    private val monthsWindowStart: Long =
        LocalDate.now().withDayOfMonth(1).minusMonths((MONTHS - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

    val monthlyState: StateFlow<MonthlyUiState> =
        repository.focusCountsByMonth(monthsWindowStart)
            .map { rows -> buildMonthly(rows) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyUiState())

    fun selectRange(range: ReportRange) {
        _range.value = range
    }

    /** Bucket-count-aware x-axis labels — always non-blank (Vico rejects blank formatter output). */
    private fun buildDailyLabels(days: Int): List<String> {
        val today = LocalDate.now()
        val locale = Locale.getDefault()
        return (0 until days).map { i ->
            val date = today.minusDays((days - 1 - i).toLong())
            if (days <= 7) date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) else date.dayOfMonth.toString()
        }
    }

    /** Last [MONTHS] months (oldest → this month) of focus-session counts + short month labels. */
    private fun buildMonthly(rows: List<MonthCount>): MonthlyUiState {
        val countByMonth = rows.associate { it.month to it.count }
        val keyFormat = DateTimeFormatter.ofPattern("yyyy-MM")
        val locale = Locale.getDefault()
        val thisMonth = YearMonth.now()
        val counts = ArrayList<Int>(MONTHS)
        val labels = ArrayList<String>(MONTHS)
        for (i in (MONTHS - 1) downTo 0) {
            val ym = thisMonth.minusMonths(i.toLong())
            counts.add(countByMonth[ym.format(keyFormat)] ?: 0)
            labels.add(monthShort(ym.month, locale))
        }
        return MonthlyUiState(counts, labels)
    }

    /** 3-letter localized month abbreviation without a trailing period (e.g. "abr", "jul"). */
    private fun monthShort(month: java.time.Month, locale: Locale): String =
        month.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")

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

    /** 3-letter month abbreviation for each of the 12 heatmap week-columns, at month boundaries only. */
    private fun buildHeatmapColumnLabels(): List<String> {
        val locale = Locale.getDefault()
        var lastMonth = -1
        return (0 until WEEKS).map { week ->
            val date = heatmapStartMonday.plusWeeks(week.toLong())
            if (date.monthValue != lastMonth) {
                lastMonth = date.monthValue
                monthShort(date.month, locale)
            } else {
                ""
            }
        }
    }

    private fun levelFor(count: Int): Int = when {
        count <= 0 -> 0
        count == 1 -> 1
        count <= 3 -> 2
        count <= 5 -> 3
        else -> 4
    }

    private fun startOfDay(daysAgo: Long): Long =
        LocalDate.now().minusDays(daysAgo).atStartOfDay(zone).toInstant().toEpochMilli()

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
        const val WEEKS = 10
        const val MONTHS = 6
    }
}
