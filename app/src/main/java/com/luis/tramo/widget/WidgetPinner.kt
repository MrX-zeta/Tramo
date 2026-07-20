package com.luis.tramo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Outcome of a pin request, so the caller can decide what (if anything) to tell the user. */
enum class PinResult {
    /** The system's pin dialog was launched; the user still has to confirm it. */
    REQUESTED,

    /** At least one instance of the widget is already on the home screen; nothing was launched. */
    ALREADY_PINNED,

    /** The launcher does not support pinning; the user has to add the widget manually. */
    UNSUPPORTED
}

/**
 * Requests the launcher to pin the Tramo focus-ring widget to the home screen, to bypass poor widget
 * discovery on custom ROMs (MIUI/HyperOS, etc.). Driven from Settings, on explicit user action —
 * never fired unprompted.
 */
class WidgetPinner @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val manager: AppWidgetManager?
        get() = context.getSystemService(AppWidgetManager::class.java)

    private val provider: ComponentName
        get() = ComponentName(context, TramoWidgetReceiver::class.java)

    /** False on launchers without pin support — show a manual hint instead of a dead button. */
    fun isPinSupported(): Boolean = manager?.isRequestPinAppWidgetSupported == true

    /**
     * True when the widget already lives on the home screen. The ids of placed instances are the
     * only reliable signal: the pin dialog itself gives no feedback, so this is what keeps a second
     * tap from adding a duplicate.
     */
    fun isPinned(): Boolean = (manager?.getAppWidgetIds(provider)?.size ?: 0) > 0

    fun requestPin(): PinResult {
        val manager = manager ?: return PinResult.UNSUPPORTED
        if (!manager.isRequestPinAppWidgetSupported) return PinResult.UNSUPPORTED
        if (isPinned()) return PinResult.ALREADY_PINNED
        manager.requestPinAppWidget(provider, null, null)
        return PinResult.REQUESTED
    }
}
