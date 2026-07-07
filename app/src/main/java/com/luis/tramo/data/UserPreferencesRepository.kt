package com.luis.tramo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val onboarded: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONBOARDED] ?: false
    }

    /** Completed focus sessions for the current day; resets automatically at midnight. */
    val sessionsToday: Flow<Int> = dataStore.data.map { prefs ->
        countForToday(prefs)
    }

    suspend fun setOnboarded(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONBOARDED] = value
        }
    }

    /** Increments today's completed-focus counter, rolling over to 1 on a new day. */
    suspend fun incrementFocusSessions(): Int {
        var newValue = 0
        dataStore.edit { prefs ->
            newValue = countForToday(prefs) + 1
            prefs[SESSIONS_COUNT] = newValue
            prefs[SESSIONS_DATE] = LocalDate.now().toEpochDay()
        }
        return newValue
    }

    suspend fun sessionsTodayValue(): Int = sessionsToday.first()

    private fun countForToday(prefs: Preferences): Int {
        val storedDay = prefs[SESSIONS_DATE] ?: -1L
        return if (storedDay == LocalDate.now().toEpochDay()) prefs[SESSIONS_COUNT] ?: 0 else 0
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val SESSIONS_COUNT = intPreferencesKey("sessions_today")
        val SESSIONS_DATE = longPreferencesKey("sessions_date")
    }
}
