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

/** Focus session count for a single local calendar day (yyyy-MM-dd), for the heatmap. */
data class DayCount(
    val day: String,
    val count: Int
)

/** Focus session count for a single local month (yyyy-MM), for the monthly-activity chart. */
data class MonthCount(
    val month: String,
    val count: Int
)

/** Focus seconds summed for a single local calendar day, for the per-day report chart. */
data class DailyFocus(
    val day: String,
    val totalSeconds: Int
)
