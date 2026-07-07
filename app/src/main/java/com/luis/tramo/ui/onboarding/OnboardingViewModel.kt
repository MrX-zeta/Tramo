package com.luis.tramo.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.widget.WidgetPinner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val widgetPinner: WidgetPinner
) : ViewModel() {

    /**
     * Marks onboarding as complete, requests pinning the home-screen widget, then runs [onDone]
     * so the caller can navigate to the Timer screen.
     */
    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setOnboarded(true)
            widgetPinner.requestPin()
            onDone()
        }
    }
}
