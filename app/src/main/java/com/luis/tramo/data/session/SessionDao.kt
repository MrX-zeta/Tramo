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
}
