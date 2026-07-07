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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Validation problems surfaced on the timer text fields. */
enum class FieldError { INVALID_NUMBER, FOCUS_BELOW_BREAK }

data class SettingsUiState(
    val focusInput: String = "",
    val breakInput: String = "",
    val focusError: FieldError? = null,
    val breakError: FieldError? = null,
    val darkOverride: Boolean? = null,
    val dailyGoal: Int = 8,
    val languageTag: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: UserPreferencesRepository
) : ViewModel() {

    // Editable field state, seeded once from the persisted values.
    private val focusInput = MutableStateFlow("")
    private val breakInput = MutableStateFlow("")

    val uiState: StateFlow<SettingsUiState> = combine(
        focusInput,
        breakInput,
        preferences.darkModeOverride,
        preferences.dailyGoal,
        preferences.languageTag
    ) { focus, breaks, dark, goal, language ->
        val (focusError, breakError) = validate(focus, breaks)
        SettingsUiState(
            focusInput = focus,
            breakInput = breaks,
            focusError = focusError,
            breakError = breakError,
            darkOverride = dark,
            dailyGoal = goal,
            languageTag = language
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        viewModelScope.launch {
            focusInput.value = preferences.focusPresetMinutes.first().toString()
            breakInput.value = preferences.breakPresetMinutes.first().toString()
        }
    }

    fun onFocusChange(value: String) {
        focusInput.value = value.filter { it.isDigit() }.take(3)
        persistIfValid()
    }

    fun onBreakChange(value: String) {
        breakInput.value = value.filter { it.isDigit() }.take(3)
        persistIfValid()
    }

    /** Persists both values to DataStore only when the whole form is valid. */
    private fun persistIfValid() {
        val focus = focusInput.value.toIntOrNull()
        val breaks = breakInput.value.toIntOrNull()
        if (focus != null && breaks != null && focus > 0 && breaks > 0 && focus >= breaks) {
            viewModelScope.launch {
                preferences.setFocusPreset(focus)
                preferences.setBreakPreset(breaks)
            }
        }
    }

    private fun validate(focus: String, breaks: String): Pair<FieldError?, FieldError?> {
        val f = focus.toIntOrNull()
        val b = breaks.toIntOrNull()
        val breakError = if (b == null || b <= 0) FieldError.INVALID_NUMBER else null
        val focusError = when {
            f == null || f <= 0 -> FieldError.INVALID_NUMBER
            breakError == null && f < b!! -> FieldError.FOCUS_BELOW_BREAK
            else -> null
        }
        return focusError to breakError
    }

    // Every setter below writes to DataStore immediately — there is no Save button.

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
    }
}
