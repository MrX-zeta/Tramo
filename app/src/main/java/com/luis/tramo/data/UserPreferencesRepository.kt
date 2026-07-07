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

    /** Long-break length in minutes. Default 15. */
    val longBreakMinutes: Flow<Int> = dataStore.data.map { it[LONG_BREAK] ?: DEFAULT_LONG_BREAK_MINUTES }

    /** Completed focus sessions before a long break replaces the short one. Default 4. */
    val sessionsBeforeLongBreak: Flow<Int> = dataStore.data.map { it[SESSIONS_BEFORE_LONG] ?: DEFAULT_SESSIONS_BEFORE_LONG }

    /** Explicit dark-mode override; null means follow the system setting. */
    val darkModeOverride: Flow<Boolean?> = dataStore.data.map { it[DARK_MODE] }

    /** Daily focus-session goal. Default 8. */
    val dailyGoal: Flow<Int> = dataStore.data.map { it[DAILY_GOAL] ?: DEFAULT_DAILY_GOAL }

    /** Keep the screen on while a session runs. Default off. */
    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[KEEP_SCREEN_ON] ?: false }

    /** Auto-start the break after a focus session ends. Default on. */
    val autoStartBreaks: Flow<Boolean> = dataStore.data.map { it[AUTO_START_BREAKS] ?: true }

    /** Auto-start the next focus session after a break ends. Default off. */
    val autoStartNextFocus: Flow<Boolean> = dataStore.data.map { it[AUTO_START_FOCUS] ?: false }

    /** Play sound + vibration on cycle completion. Default on. */
    val soundVibrationOnFinish: Flow<Boolean> = dataStore.data.map { it[SOUND_VIBRATION] ?: true }

    /** BCP-47 language tag; empty means follow the system locale. */
    val languageTag: Flow<String> = dataStore.data.map { it[LANGUAGE_TAG] ?: "" }

    suspend fun setFocusPreset(minutes: Int) {
        dataStore.edit { it[FOCUS_PRESET] = minutes }
    }

    suspend fun setBreakPreset(minutes: Int) {
        dataStore.edit { it[BREAK_PRESET] = minutes }
    }

    suspend fun setLongBreakMinutes(minutes: Int) {
        dataStore.edit { it[LONG_BREAK] = minutes }
    }

    suspend fun setSessionsBeforeLongBreak(count: Int) {
        dataStore.edit { it[SESSIONS_BEFORE_LONG] = count }
    }

    suspend fun setKeepScreenOn(value: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_ON] = value }
    }

    suspend fun setAutoStartBreaks(value: Boolean) {
        dataStore.edit { it[AUTO_START_BREAKS] = value }
    }

    suspend fun setAutoStartNextFocus(value: Boolean) {
        dataStore.edit { it[AUTO_START_FOCUS] = value }
    }

    suspend fun setSoundVibrationOnFinish(value: Boolean) {
        dataStore.edit { it[SOUND_VIBRATION] = value }
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
        const val DEFAULT_LONG_BREAK_MINUTES = 15
        const val DEFAULT_SESSIONS_BEFORE_LONG = 4
        const val DEFAULT_DAILY_GOAL = 8

        val ONBOARDED = booleanPreferencesKey("onboarded")
        val FOCUS_PRESET = intPreferencesKey("focus_preset_minutes")
        val BREAK_PRESET = intPreferencesKey("break_preset_minutes")
        val LONG_BREAK = intPreferencesKey("long_break_minutes")
        val SESSIONS_BEFORE_LONG = intPreferencesKey("sessions_before_long_break")
        val DARK_MODE = booleanPreferencesKey("dark_mode_override")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val AUTO_START_BREAKS = booleanPreferencesKey("auto_start_breaks")
        val AUTO_START_FOCUS = booleanPreferencesKey("auto_start_next_focus")
        val SOUND_VIBRATION = booleanPreferencesKey("sound_vibration_on_finish")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")

        val ACTIVE_TYPE = stringPreferencesKey("active_session_type")
        val ACTIVE_TOTAL = intPreferencesKey("active_total_seconds")
        val ACTIVE_RUNNING = booleanPreferencesKey("active_running")
        val ACTIVE_END = longPreferencesKey("active_end_epoch_millis")
        val ACTIVE_PAUSED_REMAINING = intPreferencesKey("active_paused_remaining")
    }
}
