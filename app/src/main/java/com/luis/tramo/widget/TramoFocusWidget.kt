package com.luis.tramo.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.luis.tramo.MainActivity
import com.luis.tramo.R
import com.luis.tramo.navigation.TramoDestinations
import com.luis.tramo.util.formatDuration
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.flowOf
import kotlin.math.min

class TramoFocusWidget : GlanceAppWidget() {
    // Exact (not Responsive) so LocalSize is the widget's REAL size and the layout adapts to any
    // size the user drags to. Responsive only ever reports one of a handful of fixed buckets, so a
    // wide-short shape could snap to a tall bucket and render a stacked layout that didn't fit. The
    // reactive data flow below is independent of this. The resize range is bounded in
    // tramo_focus_widget_info.xml so the widget can't be stretched into an empty giant square.
    override val sizeMode = SizeMode.Exact

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun widgetSnapshotProvider(): WidgetSnapshotProvider
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val provider = runCatching {
            EntryPointAccessors
                .fromApplication(context, WidgetEntryPoint::class.java)
                .widgetSnapshotProvider()
        }.getOrNull()
        provideContent {
            // Collect INSIDE the composition so recomposition re-reads live data. Reading before
            // provideContent would freeze the value: once the widget's session is active (e.g. after
            // tapping the ring), a later updateAll only recomposes this lambda and never re-runs the
            // read. remember keeps a single flow instance so collectAsState doesn't restart forever.
            val flow = remember { provider?.snapshotFlow() ?: flowOf(WidgetSnapshot()) }
            val snapshot by flow.collectAsState(initial = WidgetSnapshot())
            FocusRingWidget(snapshot)
        }
    }
}

private val OnSurface = ColorProvider(day = Color(0xFF1A2224), night = Color(0xFFE1E6E5))
private val Muted = ColorProvider(day = Color(0xFF3F4A4B), night = Color(0xFFBEC8C7))
private val Amber = ColorProvider(day = Color(0xFFE8B75D), night = Color(0xFFE8B75D))

@androidx.compose.runtime.Composable
private fun FocusRingWidget(data: WidgetSnapshot) {
    val context = LocalContext.current
    val size = LocalSize.current

    val w = size.width.value
    val h = size.height.value
    // Wide-and-short → the landscape strip (ring + streak/time on the left, week bars on the right).
    val isWide = w >= h * 1.5f && h < 116f
    val minSide = min(w, h)

    val ringDpValue = when {
        // Subtract the vertical padding (12dp each side) plus a little slack so the ring never
        // touches the widget's rounded edge and gets clipped.
        isWide -> (h - 28f).coerceIn(40f, 72f)
        h < 96f -> minSide - 8f
        h < 150f -> 72f
        else -> 88f
    }.coerceIn(34f, 120f)

    // Stacked layout reveals rows as height allows; the wide layout always shows everything.
    val showStreak = !isWide && h >= 92f
    val showWeek = !isWide && h >= 132f
    val hasWeek = data.weekCounts.size == 7

    val density = context.resources.displayMetrics.density
    val ringPx = (ringDpValue * density).toInt().coerceAtLeast(1)
    val strokePx = (ringPx * 0.12f).coerceAtLeast(6f)
    val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val arcColor = when {
        // Goal met: amber (same accent as the streak and the in-app celebration). Deeper in light
        // mode for contrast on the pale widget background, brighter in dark mode.
        data.goalReached && night -> 0xFFE8B75D.toInt()
        data.goalReached -> 0xFFC0862B.toInt()
        night -> 0xFF8FCBD1.toInt()
        else -> 0xFF2F5D62.toInt()
    }
    val trackColor = if (night) 0xFF3A4642.toInt() else 0xFFDBE4E3.toInt()
    val ring = focusRingBitmap(ringPx, strokePx, data.progress, arcColor, trackColor)

    // The wide strip uses a slightly smaller ratio so the "0/8" figure breathes inside its ring.
    val figureSize = (ringDpValue * (if (isWide) 0.28f else 0.30f)).coerceIn(11f, 26f).sp
    val streakIconDp = (ringDpValue * 0.22f).coerceIn(16f, 22f).dp

    if (isWide) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .padding(12.dp)
                .clickable(actionStartActivity(homeIntent(context))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RingWithFigure(ring, ringDpValue, data, figureSize)
            Spacer(GlanceModifier.width(12.dp))
            // Left group: the streak with today's focus time beneath it.
            Column(verticalAlignment = Alignment.CenterVertically) {
                StreakRow(data.streak, streakIconDp)
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = focusLabel(data.focusSecondsToday),
                    style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                )
            }
            if (hasWeek) {
                // Push the week bars to the right edge so they read as their own column.
                Spacer(GlanceModifier.defaultWeight())
                WeekStrip(
                    counts = data.weekCounts,
                    night = night,
                    maxBarHeightDp = (h - 26f).coerceIn(24f, 56f),
                    barWidthDp = 6f,
                    gapDp = 4f
                )
            }
        }
    } else {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .padding(8.dp)
                .clickable(actionStartActivity(homeIntent(context))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RingWithFigure(ring, ringDpValue, data, figureSize)
            if (showStreak) {
                Spacer(GlanceModifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StreakRow(data.streak, streakIconDp)
                    // Today's focus time rides alongside the streak: the ring says how many
                    // sessions, this says how long. Muted, split by a dot so the two never blur.
                    Spacer(GlanceModifier.width(8.dp))
                    Text(text = "·", style = TextStyle(color = Muted, fontSize = 16.sp))
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        text = focusLabel(data.focusSecondsToday),
                        style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }
            if (showWeek && hasWeek) {
                // The week strip replaces the old "Tramo" wordmark: information, not decoration.
                Spacer(GlanceModifier.height(10.dp))
                WeekStrip(
                    counts = data.weekCounts,
                    night = night,
                    maxBarHeightDp = 24f,
                    barWidthDp = 8f,
                    gapDp = 5f
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StreakRow(streak: Int, iconDp: androidx.compose.ui.unit.Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            provider = ImageProvider(R.drawable.ic_streak_fire),
            contentDescription = null,
            modifier = GlanceModifier.size(iconDp)
        )
        Spacer(GlanceModifier.width(4.dp))
        Text(
            text = streak.toString(),
            style = TextStyle(color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@androidx.compose.runtime.Composable
private fun RingWithFigure(ring: Bitmap, ringDpValue: Float, data: WidgetSnapshot, figureSize: TextUnit) {
    Box(contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(ring),
            contentDescription = null,
            modifier = GlanceModifier.size(ringDpValue.dp)
        )
        Text(
            text = "${data.sessionsToday}/${data.dailyGoal}",
            style = TextStyle(color = OnSurface, fontSize = figureSize, fontWeight = FontWeight.Bold)
        )
    }
}

/**
 * The last 7 focus days as a tidy row of rounded bars, oldest → today. Bar height tracks that day's
 * session count (all normalised to the busiest day), so it reads as a small activity chart. Built
 * from native Box shapes (not a bitmap) so MIUI/HyperOS — which caches widget ImageViews — repaints
 * it reliably. Coloured in pine-teal; amber stays reserved for the streak/progress per the palette.
 */
@androidx.compose.runtime.Composable
private fun WeekStrip(
    counts: List<Int>,
    night: Boolean,
    maxBarHeightDp: Float,
    barWidthDp: Float,
    gapDp: Float
) {
    val norm = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(verticalAlignment = Alignment.Bottom) {
        counts.forEachIndexed { index, count ->
            if (index > 0) Spacer(GlanceModifier.width(gapDp.dp))
            val frac = count.toFloat() / norm
            // Empty days keep a short stub so the baseline stays visible.
            val barHeight = (frac * maxBarHeightDp).coerceIn(4f, maxBarHeightDp)
            Box(
                modifier = GlanceModifier
                    .width(barWidthDp.dp)
                    .height(barHeight.dp)
                    .cornerRadius(3.dp)
                    .background(androidx.glance.unit.ColorProvider(weekBarColor(count, night)))
            ) {}
        }
    }
}

/** Track when idle; two pine shades for light/heavy days — a calm echo of the report heatmap. */
private fun weekBarColor(count: Int, night: Boolean): Color {
    val pine = if (night) Color(0xFF8FCBD1) else Color(0xFF2F5D62)
    // Track raised off the background so idle days stay legible; light-day pine kept fairly strong.
    val track = if (night) Color(0xFF5C6A66) else Color(0xFFB7C5C2)
    return when {
        count <= 0 -> track
        count <= 2 -> pine.copy(alpha = 0.65f)
        else -> pine
    }
}

/**
 * Today's focus time. Uses an explicit "min" below an hour ("0 min", "45 min") so it never reads as
 * a distance; an hour or more falls back to the app's shared "1h 45m" formatting.
 */
private fun focusLabel(seconds: Int): String {
    val minutes = seconds / 60
    return if (minutes < 60) "$minutes min" else formatDuration(seconds)
}

private fun focusRingBitmap(
    diameterPx: Int,
    strokePx: Float,
    progress: Float,
    arcColor: Int,
    trackColor: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(diameterPx, diameterPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val inset = strokePx / 2f + 1f
    val rect = RectF(inset, inset, diameterPx - inset, diameterPx - inset)
    val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        color = trackColor
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawArc(rect, 0f, 360f, false, track)
    val sweep = progress.coerceIn(0f, 1f) * 360f
    if (sweep > 0f) {
        val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            color = arcColor
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(rect, -90f, sweep, false, arc)
    }
    return bitmap
}

private fun homeIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(MainActivity.EXTRA_DESTINATION, TramoDestinations.SETTINGS)
        putExtra(MainActivity.EXTRA_HIGHLIGHT, "daily_goal")
    }
