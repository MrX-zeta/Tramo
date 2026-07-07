package com.luis.tramo.data.session

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One completed session (focus or break), used to build the daily/weekly/monthly report. */
@Entity(tableName = "session_records")
data class SessionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,          // SessionType.name
    val durationSeconds: Int,
    val completedAt: Long      // epoch millis
)

/** Aggregated focus seconds for one hour-of-day bucket (0..23). */
data class HourlyFocus(
    val hour: Int,
    val totalSeconds: Int
)

/** Focus session count for a single local calendar day (yyyy-MM-dd), for the heatmap. */
data class DayCount(
    val day: String,
    val count: Int
)

/** Focus seconds summed for a single local calendar day, for the per-day report chart. */
data class DailyFocus(
    val day: String,
    val totalSeconds: Int
)
