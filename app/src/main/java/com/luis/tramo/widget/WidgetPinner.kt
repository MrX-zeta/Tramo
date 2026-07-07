package com.luis.tramo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Requests the launcher to pin the Pomodoro widget to the home screen. Called right after
 * onboarding to bypass poor widget discovery on custom ROMs (MIUI/HyperOS, etc.).
 */
class WidgetPinner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun requestPin() {
        val manager = context.getSystemService(AppWidgetManager::class.java) ?: return
        if (!manager.isRequestPinAppWidgetSupported) return
        val provider = ComponentName(context, PomodoroWidgetReceiver::class.java)
        manager.requestPinAppWidget(provider, null, null)
    }
}
