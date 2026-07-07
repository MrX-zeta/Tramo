package com.luis.tramo.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R
import com.luis.tramo.navigation.TramoLargeTopBar
import com.luis.tramo.timer.TimerStatus
import com.luis.tramo.ui.theme.Spacing
import com.luis.tramo.ui.theme.TabularFigures
import com.luis.tramo.ui.theme.TramoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    bottomBar: @Composable () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: TimerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Subtle entrance that matches the onboarding motion.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TramoLargeTopBar(R.string.app_name, onOpenSettings, scrollBehavior) },
        bottomBar = bottomBar
    ) { innerPadding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(state.sessionType.labelRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(28.dp))
                TimerRing(
                    progress = state.progress,
                    timeText = state.timeText,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .aspectRatio(1f)
                )

                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.timer_sessions_today, state.completedFocusToday),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(28.dp))
                Controls(
                    isRunning = state.isRunning,
                    canStop = state.status != TimerStatus.IDLE,
                    onPlayPause = viewModel::onPlayPause,
                    onSkip = viewModel::onSkip,
                    onStop = viewModel::onStop
                )

                Spacer(Modifier.weight(1f))
                StreakTasksCard(
                    streak = state.streak,
                    tasks = state.todaysTasks,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TimerRing(
    progress: Float,
    timeText: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "ringProgress"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val ringBrush = Brush.sweepGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primary
        )
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 22.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = ringBrush,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayMedium.merge(TabularFigures),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Controls(
    isRunning: Boolean,
    canStop: Boolean,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val outline = MaterialTheme.colorScheme.onSurfaceVariant
        IconButton(onClick = onStop, enabled = canStop, modifier = Modifier.size(56.dp)) {
            Canvas(Modifier.size(22.dp)) { drawStop(if (canStop) outline else outline.copy(alpha = 0.3f)) }
        }
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(76.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            val onPrimary = MaterialTheme.colorScheme.onPrimary
            Canvas(Modifier.size(30.dp)) {
                if (isRunning) drawPause(onPrimary) else drawPlay(onPrimary)
            }
        }
        IconButton(onClick = onSkip, modifier = Modifier.size(56.dp)) {
            Canvas(Modifier.size(22.dp)) { drawSkip(outline) }
        }
    }
}

@Composable
private fun StreakTasksCard(
    streak: Int,
    tasks: List<TaskPreview>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    text = stringResource(R.string.timer_streak, streak),
                    style = MaterialTheme.typography.titleMedium.merge(TabularFigures),
                    fontWeight = FontWeight.Bold,
                    color = TramoTheme.progress
                )
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.timer_todays_tasks),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (tasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.timer_no_tasks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    tasks.forEach { task ->
                        Text(
                            text = if (task.emoji.isNotEmpty()) "${task.emoji}  ${task.title}" else task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// --- Canvas-drawn control glyphs (dependency-free, crisp on any density) ---

private fun DrawScope.drawPlay(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.12f, 0f)
        lineTo(size.width * 0.12f, size.height)
        lineTo(size.width * 0.95f, size.height / 2f)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawPause(color: Color) {
    val barWidth = size.width * 0.28f
    val radius = androidx.compose.ui.geometry.CornerRadius(barWidth * 0.3f)
    drawRoundRect(color, topLeft = Offset(0f, 0f), size = Size(barWidth, size.height), cornerRadius = radius)
    drawRoundRect(
        color,
        topLeft = Offset(size.width - barWidth, 0f),
        size = Size(barWidth, size.height),
        cornerRadius = radius
    )
}

private fun DrawScope.drawStop(color: Color) {
    drawRoundRect(
        color,
        size = Size(size.width, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.2f)
    )
}

private fun DrawScope.drawSkip(color: Color) {
    val triangle = Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, size.height)
        lineTo(size.width * 0.72f, size.height / 2f)
        close()
    }
    drawPath(triangle, color)
    drawRoundRect(
        color,
        topLeft = Offset(size.width * 0.82f, 0f),
        size = Size(size.width * 0.18f, size.height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.06f)
    )
}
