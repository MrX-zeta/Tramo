package com.luis.tramo.data.session

import com.luis.tramo.timer.SessionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val dao: SessionDao
) {
    suspend fun record(type: SessionType, durationSeconds: Int, completedAt: Long) =
        dao.insert(SessionRecordEntity(type = type.name, durationSeconds = durationSeconds, completedAt = completedAt))

    /** One-shot count of today's completed focus sessions (local day), for a snapshot read. */
    suspend fun focusCountToday(): Int =
        dao.countFocusSince(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())

    /** One-shot current focus streak (see [computeCurrentStreak]), for a snapshot read. */
    suspend fun currentStreak(): Int = computeCurrentStreak(dao.focusDayStampsOnce(), LocalDate.now())

    fun focusSecondsSince(startMillis: Long): Flow<Int> = dao.focusSecondsSince(startMillis)
    fun breakSecondsSince(startMillis: Long): Flow<Int> = dao.breakSecondsSince(startMillis)
    fun focusCountSince(startMillis: Long): Flow<Int> = dao.focusCountSince(startMillis)
    suspend fun countFocusSince(startMillis: Long): Int = dao.countFocusSince(startMillis)
    fun focusMinutesByDay(startMillis: Long): Flow<List<DailyFocus>> = dao.focusMinutesByDay(startMillis)
    fun focusDayStamps(): Flow<List<String>> = dao.focusDayStamps()
    fun focusCountsByDay(startMillis: Long): Flow<List<DayCount>> = dao.focusCountsByDay(startMillis)
    fun totalFocusCount(): Flow<Int> = dao.totalFocusCount()
    fun totalFocusSeconds(): Flow<Int> = dao.totalFocusSeconds()
    fun focusCountsByMonth(startMillis: Long): Flow<List<MonthCount>> = dao.focusCountsByMonth(startMillis)
    fun longestStreak(): Flow<Int> = dao.longestStreak()
}
