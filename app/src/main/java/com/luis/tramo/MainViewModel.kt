package com.luis.tramo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.UserPreferencesRepository
import com.luis.tramo.navigation.TramoDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /** null while the flag is being read; then the route to use as the nav start destination. */
    val startDestination: StateFlow<String?> = preferencesRepository.onboarded
        .map { onboarded ->
            if (onboarded) TramoDestinations.TIMER else TramoDestinations.ONBOARDING
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** Explicit dark-mode override; null means follow the system setting. */
    val darkModeOverride: StateFlow<Boolean?> = preferencesRepository.darkModeOverride
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}
