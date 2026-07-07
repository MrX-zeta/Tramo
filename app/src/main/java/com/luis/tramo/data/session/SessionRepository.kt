package com.luis.tramo.data.session

import com.luis.tramo.timer.SessionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val dao: SessionDao
) {
    suspend fun record(type: SessionType, durationSeconds: Int, completedAt: Long) =
        dao.insert(SessionRecordEntity(type = type.name, durationSeconds = durationSeconds, completedAt = completedAt))

    fun focusIntensityByHour(startMillis: Long): Flow<List<HourlyFocus>> = dao.focusIntensityByHour(startMillis)
    fun focusSecondsSince(startMillis: Long): Flow<Int> = dao.focusSecondsSince(startMillis)
    fun breakSecondsSince(startMillis: Long): Flow<Int> = dao.breakSecondsSince(startMillis)
    fun focusCountSince(startMillis: Long): Flow<Int> = dao.focusCountSince(startMillis)
}
