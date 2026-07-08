package com.luis.tramo.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.luis.tramo.MainActivity
import com.luis.tramo.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlin.math.min

/**
 * Minimal "focus ring" home-screen widget: a progress arc filling to today's completed focus
 * sessions over the daily goal, the "N/M" figure at its centre, the streak and a small wordmark.
 *
 * The load is guarded so [provideContent] is ALWAYS reached with a valid state (a zero [WidgetSnapshot]
 * on any failure), which is what prevents the widget from getting stuck on the loading layout.
 */
class TramoFocusWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun widgetSnapshotProvider(): WidgetSnapshotProvider
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = runCatching {
            EntryPointAccessors
                .fromApplication(context, WidgetEntryPoint::class.java)
                .widgetSnapshotProvider()
                .load()
        }.getOrDefault(WidgetSnapshot())

        provideContent { FocusRingWidget(snapshot) }
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

    // Progressive disclosure: the smallest cell shows just the ring + figure; taller cells add the
    // streak, and the tallest adds the wordmark.
    val showStreak = size.height >= 92.dp
    val showWordmark = size.height >= 132.dp

    val minSide = min(size.width.value, size.height.value)
    val ringDpValue = when {
        !showStreak -> minSide - 8f
        !showWordmark -> 66f
        else -> 88f
    }.coerceIn(34f, 120f)

    val density = context.resources.displayMetrics.density
    val ringPx = (ringDpValue * density).toInt().coerceAtLeast(1)
    val strokePx = (ringPx * 0.11f).coerceAtLeast(6f)
    val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val arcColor = if (night) 0xFF8FCBD1.toInt() else 0xFF2F5D62.toInt() // TramoPineDark / TramoPine
    val trackColor = if (night) 0xFF3A4642.toInt() else 0xFFDBE4E3.toInt() // dim surfaceVariant tone
    val ring = focusRingBitmap(ringPx, strokePx, data.progress, arcColor, trackColor)

    val figureSize = (ringDpValue * 0.24f).coerceIn(13f, 20f).sp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(8.dp)
            .clickable(actionStartActivity(homeIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
        if (showStreak) {
            Spacer(GlanceModifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔥 ", style = TextStyle(color = OnSurface, fontSize = 13.sp))
                Text(
                    text = data.streak.toString(),
                    style = TextStyle(color = Amber, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
        if (showWordmark) {
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = "Tramo",
                style = TextStyle(color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            )
        }
    }
}

/** Draws the dim full-circle track with the brand-teal progress arc on top (12 o'clock start). */
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
    }
