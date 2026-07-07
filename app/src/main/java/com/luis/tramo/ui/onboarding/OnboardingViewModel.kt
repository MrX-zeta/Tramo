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
     * Marks onboarding as complete. [onDone] runs after the flag is persisted so the
     * caller can navigate to the Timer screen.
     */
    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.setOnboarded(true)
            onDone()
        }
    }
}
