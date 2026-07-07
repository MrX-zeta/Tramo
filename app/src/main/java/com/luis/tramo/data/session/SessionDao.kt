package com.luis.tramo.data.session

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(record: SessionRecordEntity)

    /** Focus seconds grouped by local hour-of-day since [startMillis]. */
    @Query(
        """
        SELECT CAST(strftime('%H', completedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hour,
               COALESCE(SUM(durationSeconds), 0) AS totalSeconds
        FROM session_records
        WHERE type = 'FOCUS' AND completedAt >= :startMillis
        GROUP BY hour
        ORDER BY hour
        """
    )
    fun focusIntensityByHour(startMillis: Long): Flow<List<HourlyFocus>>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM session_records WHERE type = 'FOCUS' AND completedAt >= :startMillis")
    fun focusSecondsSince(startMillis: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM session_records WHERE type != 'FOCUS' AND completedAt >= :startMillis")
    fun breakSecondsSince(startMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM session_records WHERE type = 'FOCUS' AND completedAt >= :startMillis")
    fun focusCountSince(startMillis: Long): Flow<Int>

    /** One-shot focus count since [startMillis] (used by the service for the long-break cadence). */
    @Query("SELECT COUNT(*) FROM session_records WHERE type = 'FOCUS' AND completedAt >= :startMillis")
    suspend fun countFocusSince(startMillis: Long): Int

    /** Focus seconds per local day since [startMillis], for the per-day report chart. */
    @Query(
        """
        SELECT date(completedAt / 1000, 'unixepoch', 'localtime') AS day,
               COALESCE(SUM(durationSeconds), 0) AS totalSeconds
        FROM session_records
        WHERE type = 'FOCUS' AND completedAt >= :startMillis
        GROUP BY day
        """
    )
    fun focusMinutesByDay(startMillis: Long): Flow<List<DailyFocus>>

    /** Distinct local calendar days (yyyy-MM-dd) that have at least one focus session. */
    @Query(
        """
        SELECT DISTINCT strftime('%Y-%m-%d', completedAt / 1000, 'unixepoch', 'localtime')
        FROM session_records
        WHERE type = 'FOCUS'
        """
    )
    fun focusDayStamps(): Flow<List<String>>

    /** Focus session counts per local day since [startMillis], for the activity heatmap. */
    @Query(
        """
        SELECT date(completedAt / 1000, 'unixepoch', 'localtime') AS day, COUNT(*) AS count
        FROM session_records
        WHERE type = 'FOCUS' AND completedAt >= :startMillis
        GROUP BY day
        """
    )
    fun focusCountsByDay(startMillis: Long): Flow<List<DayCount>>

    @Query("SELECT COUNT(*) FROM session_records WHERE type = 'FOCUS'")
    fun totalFocusCount(): Flow<Int>

    /**
     * Longest all-time streak of consecutive focus days, computed with a recursive CTE:
     * `days` = distinct focus days; `streak` walks forward from each day that starts a run
     * (its previous day is absent), accumulating the run length; the answer is the max length.
     */
    @Query(
        """
        WITH RECURSIVE days AS (
            SELECT DISTINCT date(completedAt / 1000, 'unixepoch', 'localtime') AS d
            FROM session_records WHERE type = 'FOCUS'
        ),
        streak AS (
            SELECT d, 1 AS len FROM days
            WHERE date(d, '-1 day') NOT IN (SELECT d FROM days)
            UNION ALL
            SELECT date(s.d, '+1 day'), s.len + 1
            FROM streak s JOIN days ON days.d = date(s.d, '+1 day')
        )
        SELECT COALESCE(MAX(len), 0) FROM streak
        """
    )
    fun longestStreak(): Flow<Int>
}
