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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted snapshot of the running/paused session so it survives process death. DataStore only
 * ever holds this (plus settings) — never a per-second live tick.
 */
data class ActiveSession(
    val sessionType: String,
    val totalSeconds: Int,
    val running: Boolean,
    /** Absolute wall-clock end; authoritative for a RUNNING session. */
    val endEpochMillis: Long,
    /** Remaining seconds for a PAUSED session. */
    val pausedRemaining: Int
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val onboarded: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONBOARDED] ?: false
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
        dataStore.edit { it[ONBOARDED] = value }
    }

    // --- Active session snapshot ---

    suspend fun saveActiveSession(session: ActiveSession) {
        dataStore.edit { prefs ->
            prefs[ACTIVE_TYPE] = session.sessionType
            prefs[ACTIVE_TOTAL] = session.totalSeconds
            prefs[ACTIVE_RUNNING] = session.running
            prefs[ACTIVE_END] = session.endEpochMillis
            prefs[ACTIVE_PAUSED_REMAINING] = session.pausedRemaining
        }
    }

    suspend fun clearActiveSession() {
        dataStore.edit { prefs ->
            prefs.remove(ACTIVE_TYPE)
            prefs.remove(ACTIVE_TOTAL)
            prefs.remove(ACTIVE_RUNNING)
            prefs.remove(ACTIVE_END)
            prefs.remove(ACTIVE_PAUSED_REMAINING)
        }
    }

    suspend fun getActiveSession(): ActiveSession? {
        val prefs = dataStore.data.first()
        val type = prefs[ACTIVE_TYPE] ?: return null
        return ActiveSession(
            sessionType = type,
            totalSeconds = prefs[ACTIVE_TOTAL] ?: 0,
            running = prefs[ACTIVE_RUNNING] ?: false,
            endEpochMillis = prefs[ACTIVE_END] ?: 0L,
            pausedRemaining = prefs[ACTIVE_PAUSED_REMAINING] ?: 0
        )
    }

    private companion object {
        const val DEFAULT_FOCUS_MINUTES = 25
        const val DEFAULT_BREAK_MINUTES = 5
        const val DEFAULT_DAILY_GOAL = 8

        val ONBOARDED = booleanPreferencesKey("onboarded")
        val FOCUS_PRESET = intPreferencesKey("focus_preset_minutes")
        val BREAK_PRESET = intPreferencesKey("break_preset_minutes")
        val DARK_MODE = booleanPreferencesKey("dark_mode_override")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")

        val ACTIVE_TYPE = stringPreferencesKey("active_session_type")
        val ACTIVE_TOTAL = intPreferencesKey("active_total_seconds")
        val ACTIVE_RUNNING = booleanPreferencesKey("active_running")
        val ACTIVE_END = longPreferencesKey("active_end_epoch_millis")
        val ACTIVE_PAUSED_REMAINING = intPreferencesKey("active_paused_remaining")
    }
}
