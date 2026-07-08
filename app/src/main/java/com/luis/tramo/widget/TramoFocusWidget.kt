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
import androidx.compose.ui.unit.DpSize
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
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.flowOf
import kotlin.math.min

class TramoFocusWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

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

    companion object {
        private val SMALL = DpSize(110.dp, 48.dp)
        private val MEDIUM = DpSize(150.dp, 110.dp)
        private val LARGE = DpSize(200.dp, 160.dp)
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
    val isWide = w >= h * 1.6f && h < 110f
    val minSide = min(w, h)

    val ringDpValue = when {
        isWide -> (h - 12f).coerceIn(48f, 72f)
        h < 92f -> minSide - 8f
        h < 132f -> 74f
        else -> 96f
    }.coerceIn(34f, 128f)

    val showStreak = !isWide && h >= 92f
    val showWordmark = !isWide && h >= 132f

    val density = context.resources.displayMetrics.density
    val ringPx = (ringDpValue * density).toInt().coerceAtLeast(1)
    val strokePx = (ringPx * 0.12f).coerceAtLeast(6f)
    val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val arcColor = if (night) 0xFF8FCBD1.toInt() else 0xFF2F5D62.toInt()
    val trackColor = if (night) 0xFF3A4642.toInt() else 0xFFDBE4E3.toInt()
    val ring = focusRingBitmap(ringPx, strokePx, data.progress, arcColor, trackColor)

    // The 2x1 (isWide) uses a slightly smaller ratio so the "0/10" figure breathes inside its
    // modestly-enlarged ring; larger sizes keep the 0.30 ratio and are unchanged.
    val figureSize = (ringDpValue * (if (isWide) 0.27f else 0.30f)).coerceIn(11f, 28f).sp
    val streakIconDp = (ringDpValue * 0.22f).coerceIn(16f, 24f).dp

    if (isWide) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .padding(10.dp)
                .clickable(actionStartActivity(homeIntent(context))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RingWithFigure(ring, ringDpValue, data, figureSize)
            Spacer(GlanceModifier.width(12.dp))
            Column(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_streak_fire),
                        contentDescription = null,
                        modifier = GlanceModifier.size(18.dp)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = data.streak.toString(),
                        style = TextStyle(color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = "Tramo",
                    style = TextStyle(color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                    Image(
                        provider = ImageProvider(R.drawable.ic_streak_fire),
                        contentDescription = null,
                        modifier = GlanceModifier.size(streakIconDp)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        text = data.streak.toString(),
                        style = TextStyle(color = Amber, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
            if (showWordmark) {
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    text = "Tramo",
                    style = TextStyle(color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
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