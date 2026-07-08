package com.luis.tramo.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R
import com.luis.tramo.timer.TimerStatus
import com.luis.tramo.ui.components.ScreenEntrance
import com.luis.tramo.ui.components.rememberReduceMotion
import com.luis.tramo.ui.theme.Spacing
import com.luis.tramo.ui.theme.TabularFigures
import com.luis.tramo.ui.theme.TramoTheme
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    bottomBar: @Composable () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: TimerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Keep the screen awake only while enabled AND a session is running.
    val view = LocalView.current
    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val reduceMotion = rememberReduceMotion()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        topBar = { TimerHeader(onOpenSettings) },
        bottomBar = bottomBar
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            ScreenEntrance(index = 0, visible = visible, reduceMotion = reduceMotion) {
                TimerSessionCard(
                    state = state,
                    reduceMotion = reduceMotion,
                    onPlayPause = viewModel::onPlayPause,
                    onSkip = viewModel::onSkip,
                    onStop = viewModel::onStop
                )
            }
            ScreenEntrance(index = 1, visible = visible, reduceMotion = reduceMotion) {
                StreakCard(streak = state.streak, weekDots = state.weekDots)
            }
            ScreenEntrance(index = 2, visible = visible, reduceMotion = reduceMotion) {
                TasksCard(tasks = state.todaysTasks)
            }
        }
    }
}

@Composable
private fun TimerHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
        }
    }
}

@Composable
private fun TimerSessionCard(
    state: TimerUiState,
    reduceMotion: Boolean,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(Spacing.xl), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(state.sessionType.labelRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Spacing.xl))
            TimerRing(
                progress = state.progress,
                timeText = state.timeText,
                running = state.isRunning,
                reduceMotion = reduceMotion,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .aspectRatio(1f)
            )
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.timer_sessions_today, state.completedFocusToday),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xl))
            Controls(
                isRunning = state.isRunning,
                canStop = state.status != TimerStatus.IDLE,
                onPlayPause = onPlayPause,
                onSkip = onSkip,
                onStop = onStop
            )
        }
    }
}

@Composable
private fun StreakCard(streak: Int, weekDots: List<Boolean>) {
    ElevatedCard(shape = RoundedCornerShape(Spacing.xl), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
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
            Spacer(Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(Spacing.md))
            WeekRow(dots = weekDots)
        }
    }
}

@Composable
private fun WeekRow(dots: List<Boolean>) {
    val labels = remember {
        val first = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        (0L until 7L).map { first.plus(it).getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()) }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dots.forEachIndexed { index, done ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Box(
                    Modifier
                        .size(12.dp)
                        .background(
                            color = if (done) TramoTheme.progress else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                )
                Text(
                    text = labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TasksCard(tasks: List<TaskPreview>) {
    ElevatedCard(shape = RoundedCornerShape(Spacing.xl), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(
                text = stringResource(R.string.timer_todays_tasks),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(Spacing.md))
            if (tasks.isEmpty()) {
                Text(
                    text = stringResource(R.string.timer_no_tasks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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

@Composable
private fun TimerRing(
    progress: Float,
    timeText: String,
    running: Boolean,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "ringProgress"
    )
    // The product's single glass moment: a soft teal glow behind the number, ONLY while running.
    // A radial gradient (no live blur) — cheap enough to sit behind the per-second ticking number.
    val glowAlpha by animateFloatAsState(
        targetValue = if (running) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 500),
        label = "ringGlow"
    )
    val glowColor = MaterialTheme.colorScheme.primary
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
            if (glowAlpha > 0f) {
                val glowRadius = size.minDimension * 0.42f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.16f * glowAlpha),
                            Color.Transparent
                        ),
                        center = center,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = center
                )
            }
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl, Alignment.CenterHorizontally),
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
