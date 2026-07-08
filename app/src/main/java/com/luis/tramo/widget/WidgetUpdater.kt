package com.luis.tramo.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point of truth for repainting the home-screen widget. Callers fire [refresh]
 * (fire-and-forget) after each change; here bursts are collapsed with a [debounce] and a single
 * [updateAll] is emitted with the final state.
 *
 * The debounce matters because MIUI/HyperOS (and the AppWidgetHost in general) rate-limits how
 * often a widget update is actually applied: several updateAll calls in quick succession — e.g.
 * nudging the daily goal 8 → 5 with rapid stepper taps — get mostly discarded, and sometimes an
 * intermediate state wins, so the widget trails the real value by one. Waiting ~250ms for the
 * burst to settle and pushing exactly one, well-spaced update sidesteps that limit. The work runs
 * on an application scope, so it still completes after the user leaves Settings.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    init {
        scope.launch { collectRequests() }
    }

    @OptIn(FlowPreview::class)
    private suspend fun collectRequests() {
        requests.debounce(250).collect {
            // Never let the exception escape: a failed updateAll must not kill the collector and
            // freeze all future updates.
            try {
                TramoFocusWidget().updateAll(context)
            } catch (e: Exception) {
                Log.w("WidgetUpdater", "updateAll failed", e)
            }
        }
    }

    /** Request a repaint. Non-blocking; collapses successive calls into a single update. */
    fun refresh() {
        requests.tryEmit(Unit)
    }
}
