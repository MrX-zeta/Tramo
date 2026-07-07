package com.luis.tramo.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val onboarded: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONBOARDED] ?: false
    }

    suspend fun setOnboarded(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONBOARDED] = value
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }
}
