package com.luis.tramo.ui.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R
import com.luis.tramo.timer.TimerStatus

@Composable
fun TimerScreen(
    onOpenTasks: () -> Unit = {},
    onOpenReport: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: TimerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
        TextButton(
            onClick = onOpenReport,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Text(stringResource(R.string.timer_open_report))
        }
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.timer_open_settings))
            }
            TextButton(onClick = onOpenTasks) {
                Text(stringResource(R.string.timer_open_tasks))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(state.sessionType.labelRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(32.dp))

            TimerRing(
                progress = state.progress,
                timeText = state.timeText,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(1f)
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.timer_sessions_today, state.completedFocusToday),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))
            Controls(
                isRunning = state.isRunning,
                canStop = state.status != TimerStatus.IDLE,
                onPlayPause = viewModel::onPlayPause,
                onSkip = viewModel::onSkip,
                onStop = viewModel::onStop
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
        animationSpec = tween(durationMillis = 400),
        label = "ringProgress"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 20.dp.toPx()
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
                color = progressColor,
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
            style = MaterialTheme.typography.displayMedium
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onStop, enabled = canStop) {
            Text(stringResource(R.string.timer_stop))
        }
        Button(
            onClick = onPlayPause,
            modifier = Modifier.widthIn(min = 120.dp)
        ) {
            Text(
                stringResource(
                    if (isRunning) R.string.timer_pause else R.string.timer_play
                )
            )
        }
        OutlinedButton(onClick = onSkip) {
            Text(stringResource(R.string.timer_skip))
        }
    }
}
