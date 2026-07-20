package com.luis.tramo.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /**
     * Marks onboarding as complete, then runs [onDone] so the caller can navigate to the Timer
     * screen. Pinning the widget is deliberately not done here: firing the system pin dialog
     * unprompted right after onboarding is intrusive. It now lives in Settings, where the user asks
     * for it.
     */
    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setOnboarded(true)
            onDone()
        }
    }
}
