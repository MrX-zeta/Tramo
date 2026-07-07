package com.luis.tramo.ui.report

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R
import com.luis.tramo.util.formatDuration
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val heat by viewModel.heatmapState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.report_title),
                style = MaterialTheme.typography.headlineSmall
            )

            // Lifetime stats.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LifetimeStatCard(
                    label = stringResource(R.string.report_total_completions),
                    value = heat.totalCompletions.toString(),
                    modifier = Modifier.weight(1f)
                )
                LifetimeStatCard(
                    label = stringResource(R.string.report_current_streak),
                    value = heat.currentStreak.toString(),
                    modifier = Modifier.weight(1f)
                )
                LifetimeStatCard(
                    label = stringResource(R.string.report_longest_streak),
                    value = heat.longestStreak.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            // 12-week activity heatmap.
            ActivityHeatmap(cells = heat.cells)

            // Today's totals.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = stringResource(R.string.report_focus_time),
                    value = formatDuration(state.focusSeconds),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.report_break_time),
                    value = formatDuration(state.breakSeconds),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = stringResource(R.string.report_avg_session),
                    value = formatDuration(state.avgSessionSeconds),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.report_session_count),
                    value = state.sessionCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            // Weekly / Monthly toggle.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val ranges = ReportRange.entries
                ranges.forEachIndexed { index, range ->
                    SegmentedButton(
                        selected = state.range == range,
                        onClick = { viewModel.selectRange(range) },
                        shape = SegmentedButtonDefaults.itemShape(index, ranges.size)
                    ) {
                        Text(stringResource(range.labelRes()))
                    }
                }
            }

            Text(
                text = stringResource(R.string.report_chart_title),
                style = MaterialTheme.typography.titleMedium
            )
            FocusIntensityChart(
                hourlyMinutes = state.hourlyMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LifetimeStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 16.dp, horizontal = 12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val HEATMAP_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
private fun ActivityHeatmap(cells: List<HeatmapCell>) {
    var selected by remember { mutableStateOf<HeatmapCell?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(cells) { if (cells.isNotEmpty()) loaded = true }
    val gridAlpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = tween(700),
        label = "heatmapAlpha"
    )

    val emptyColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val primary = MaterialTheme.colorScheme.primary

    Column {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.report_activity_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            selected?.let { cell ->
                Text(
                    text = "${cell.date.format(HEATMAP_DATE_FORMAT)} · ${cell.count}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(12),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .alpha(gridAlpha)
        ) {
            items(cells) { cell ->
                val color = if (cell.level == 0) emptyColor else primary.copy(alpha = ALPHA_BY_LEVEL[cell.level])
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .clickable { selected = cell }
                )
            }
        }
    }
}

private val ALPHA_BY_LEVEL = floatArrayOf(0f, 0.4f, 0.6f, 0.8f, 1f)

@Composable
private fun FocusIntensityChart(
    hourlyMinutes: List<Int>,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(hourlyMinutes) {
        modelProducer.runTransaction {
            columnModel { series(hourlyMinutes.ifEmpty { listOf(0) }) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom()
        ),
        modelProducer = modelProducer,
        modifier = modifier
    )
}

private fun ReportRange.labelRes(): Int = when (this) {
    ReportRange.WEEKLY -> R.string.report_range_weekly
    ReportRange.MONTHLY -> R.string.report_range_monthly
}
