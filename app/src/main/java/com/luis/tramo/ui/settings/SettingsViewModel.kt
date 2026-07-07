package com.luis.tramo.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val focusPreset: Int = 25,
    val darkOverride: Boolean? = null,
    val dailyGoal: Int = 8,
    val languageTag: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.focusPresetMinutes,
        preferences.darkModeOverride,
        preferences.dailyGoal,
        preferences.languageTag
    ) { preset, dark, goal, language ->
        SettingsUiState(preset, dark, goal, language)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    // Every setter writes to DataStore immediately — there is no Save button.

    fun setFocusPreset(minutes: Int) = viewModelScope.launch {
        preferences.setFocusPreset(minutes)
    }

    fun setDarkOverride(value: Boolean?) = viewModelScope.launch {
        preferences.setDarkModeOverride(value)
    }

    fun setDailyGoal(goal: Int) = viewModelScope.launch {
        preferences.setDailyGoal(goal.coerceIn(MIN_GOAL, MAX_GOAL))
    }

    fun setLanguage(tag: String) {
        viewModelScope.launch { preferences.setLanguageTag(tag) }
        applyLocale(tag)
    }

    /** Applies the per-app locale on Android 13+; on older versions the choice is only persisted. */
    private fun applyLocale(tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
            localeManager.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        }
    }

    companion object {
        const val MIN_GOAL = 1
        const val MAX_GOAL = 16
        val FOCUS_PRESETS = listOf(15, 25, 50)
    }
}
