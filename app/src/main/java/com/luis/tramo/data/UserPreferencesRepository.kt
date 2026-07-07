package com.luis.tramo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    // --- Settings ---

    /** Custom focus session length in minutes. Default 25. */
    val focusPresetMinutes: Flow<Int> = dataStore.data.map { it[FOCUS_PRESET] ?: DEFAULT_FOCUS_MINUTES }

    /** Custom (short) break length in minutes. Default 5. */
    val breakPresetMinutes: Flow<Int> = dataStore.data.map { it[BREAK_PRESET] ?: DEFAULT_BREAK_MINUTES }

    /** Explicit dark-mode override; null means follow the system setting. */
    val darkModeOverride: Flow<Boolean?> = dataStore.data.map { it[DARK_MODE] }

    /** Daily focus-session goal. Default 8. */
    val dailyGoal: Flow<Int> = dataStore.data.map { it[DAILY_GOAL] ?: DEFAULT_DAILY_GOAL }

    /** BCP-47 language tag; empty means follow the system locale. */
    val languageTag: Flow<String> = dataStore.data.map { it[LANGUAGE_TAG] ?: "" }

    suspend fun setFocusPreset(minutes: Int) {
        dataStore.edit { it[FOCUS_PRESET] = minutes }
    }

    suspend fun setBreakPreset(minutes: Int) {
        dataStore.edit { it[BREAK_PRESET] = minutes }
    }

    suspend fun setDarkModeOverride(value: Boolean?) {
        dataStore.edit { prefs ->
            if (value == null) prefs.remove(DARK_MODE) else prefs[DARK_MODE] = value
        }
    }

    suspend fun setDailyGoal(goal: Int) {
        dataStore.edit { it[DAILY_GOAL] = goal }
    }

    suspend fun setLanguageTag(tag: String) {
        dataStore.edit { it[LANGUAGE_TAG] = tag }
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
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_BREAK_MINUTES = 5
        const val DEFAULT_DAILY_GOAL = 8

        val ONBOARDED = booleanPreferencesKey("onboarded")
        val SESSIONS_COUNT = intPreferencesKey("sessions_today")
        val SESSIONS_DATE = longPreferencesKey("sessions_date")
        val FOCUS_PRESET = intPreferencesKey("focus_preset_minutes")
        val BREAK_PRESET = intPreferencesKey("break_preset_minutes")
        val DARK_MODE = booleanPreferencesKey("dark_mode_override")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
    }
}
