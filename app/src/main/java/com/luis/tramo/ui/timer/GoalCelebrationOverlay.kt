package com.luis.tramo.ui.timer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.tramo.R
import com.luis.tramo.ui.theme.TramoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private val CelebrationAmber = Color(0xFFE8B75D)

private class Mote(
    val angle: Float,
    val size: Float,
    val rise: Float,
    val drift: Float,
    val delay: Float
)

/**
 * The daily-goal celebration: a calm, premium "the ring blooms" moment (not confetti). The focus
 * ring — the app's own metaphor — fills to completion, a warm glow blooms, a checkmark settles in
 * and a few slow amber motes drift up. Auto-dismisses; tap anywhere to close early. Honors
 * reduce-motion by skipping the motion and just presenting the finished state.
 */
@Composable
fun GoalCelebrationOverlay(
    info: CelebrationInfo,
    reduceMotion: Boolean,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val overlayAlpha = remember { Animatable(0f) }
    val arc = remember { Animatable(0f) }
    val bloom = remember { Animatable(0f) }
    val content = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }

    val motes = remember {
        if (reduceMotion) emptyList()
        else List(10) {
            Mote(
                angle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                size = 3f + Random.nextFloat() * 4f,
                rise = 0.5f + Random.nextFloat() * 0.7f,
                drift = (Random.nextFloat() - 0.5f) * 0.5f,
                delay = Random.nextFloat() * 0.3f
            )
        }
    }

    val arcColor = TramoTheme.progress
    val trackColor = Color.White.copy(alpha = 0.12f)

    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        overlayAlpha.animateTo(1f, tween(250))
        if (reduceMotion) {
            arc.snapTo(1f); bloom.snapTo(0.45f); content.snapTo(1f)
            delay(1600)
        } else {
            arc.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            launch { bloom.animateTo(1f, tween(480)); bloom.animateTo(0.45f, tween(700)) }
            launch { particleProgress.animateTo(1f, tween(1500)) }
            content.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            delay(2200)
        }
        overlayAlpha.animateTo(0f, tween(300))
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(184.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = size.minDimension * 0.055f
                    val ringRadius = size.minDimension / 2f - stroke / 2f - 6f
                    val c = Offset(size.width / 2f, size.height / 2f)

                    if (bloom.value > 0f) {
                        val glowRadius = ringRadius * (1f + bloom.value * 0.8f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CelebrationAmber.copy(alpha = 0.30f * bloom.value),
                                    Color.Transparent
                                ),
                                center = c,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = c
                        )
                    }

                    drawCircle(color = trackColor, radius = ringRadius, center = c, style = Stroke(stroke))

                    if (arc.value > 0f) {
                        drawArc(
                            color = arcColor,
                            startAngle = -90f,
                            sweepAngle = arc.value * 360f,
                            useCenter = false,
                            topLeft = Offset(c.x - ringRadius, c.y - ringRadius),
                            size = Size(ringRadius * 2f, ringRadius * 2f),
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }

                    motes.forEach { m ->
                        val local = ((particleProgress.value - m.delay) / (1f - m.delay)).coerceIn(0f, 1f)
                        if (local > 0f && local < 1f) {
                            val x = c.x + cos(m.angle) * ringRadius + m.drift * ringRadius * local
                            val y = c.y + sin(m.angle) * ringRadius - m.rise * ringRadius * local
                            val a = min(local * 4f, 1f) * (1f - local)
                            drawCircle(color = CelebrationAmber.copy(alpha = a), radius = m.size, center = Offset(x, y))
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = arcColor,
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            alpha = content.value
                            scaleX = 0.6f + content.value * 0.4f
                            scaleY = 0.6f + content.value * 0.4f
                        }
                )
            }

            Spacer(Modifier.height(28.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = content.value }
            ) {
                Text(
                    text = stringResource(R.string.goal_celebration_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.goal_celebration_subtitle, info.sessions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
                if (info.streak > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "🔥 ${info.streak}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CelebrationAmber
                    )
                }
            }
        }
    }
}
