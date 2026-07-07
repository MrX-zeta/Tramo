package com.luis.tramo.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.luis.tramo.timer.TimerState
import com.luis.tramo.timer.TimerStatus
import com.luis.tramo.timer.formatTime
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.luis.tramo.timer.TimerStateHolder

/**
 * Home-screen widget showing the ongoing timer. Resizable via [SizeMode.Responsive]: a compact
 * layout for small cells and a richer one (session label) for larger cells.
 */
class PomodoroWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(SMALL, MEDIUM, LARGE)
    )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun timerStateHolder(): TimerStateHolder
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val holder = EntryPointAccessors
            .fromApplication(context, WidgetEntryPoint::class.java)
            .timerStateHolder()
        // Snapshot: the service triggers updateAll() on each change to re-render.
        val snapshot = holder.state.value
        provideContent {
            GlanceTheme {
                WidgetContent(snapshot)
            }
        }
    }

    companion object {
        private val SMALL = DpSize(120.dp, 48.dp)
        private val MEDIUM = DpSize(180.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 180.dp)
    }
}

@Composable
private fun WidgetContent(state: TimerState) {
    val context = LocalContext.current
    val size = LocalSize.current
    val compact = size.height < 90.dp
    val onSurface = ColorProvider(GlanceTheme.colors.onSurface.getColor(context))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!compact) {
            Text(
                text = context.getString(state.sessionType.labelRes),
                style = TextStyle(color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            )
        }
        Text(
            text = formatTime(state.remainingSeconds),
            style = TextStyle(
                color = onSurface,
                fontSize = (if (compact) 22 else 32).sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (!compact) {
            Text(
                text = statusLabel(state.status),
                style = TextStyle(color = onSurface, fontSize = 12.sp)
            )
        }
    }
}

private fun statusLabel(status: TimerStatus): String = when (status) {
    TimerStatus.RUNNING -> "Running"
    TimerStatus.PAUSED -> "Paused"
    TimerStatus.FINISHED -> "Done"
    TimerStatus.IDLE -> "Ready"
}
