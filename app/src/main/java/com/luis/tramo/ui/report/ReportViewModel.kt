package com.luis.tramo.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.session.HourlyFocus
import com.luis.tramo.data.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class ReportRange(val days: Long) { WEEKLY(7), MONTHLY(30) }

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

    fun selectRange(range: ReportRange) {
        _range.value = range
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
}
