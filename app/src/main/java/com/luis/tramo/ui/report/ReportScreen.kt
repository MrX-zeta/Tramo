package com.luis.tramo.ui.report

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R
import com.luis.tramo.ui.components.StatCard
import com.luis.tramo.ui.theme.Spacing
import com.luis.tramo.ui.theme.TabularFigures
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ReportScreen(
    bottomBar: @Composable () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val heat by viewModel.heatmapState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { ReportHeader(onOpenSettings) },
        bottomBar = bottomBar
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            // 1 — Two summary KPI cards.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                StatCard(
                    value = heat.totalCompletions.toString(),
                    label = stringResource(R.string.report_total_sessions),
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = heat.activeDays.toString(),
                    label = stringResource(R.string.report_active_days),
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 2 — Activity heatmap.
            if (heat.totalCompletions == 0) {
                Text(stringResource(R.string.report_activity_title), style = MaterialTheme.typography.titleMedium)
                ChartEmptyState()
            } else {
                ActivityHeatmap(cells = heat.cells)
            }

            // 3 — Weekly / Monthly toggle + per-day bar chart.
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
                text = stringResource(R.string.report_daily_chart_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (state.dailyMinutes.all { it == 0 }) {
                ChartEmptyState()
            } else {
                BarChart(
                    values = state.dailyMinutes,
                    labels = state.dailyLabels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }
        }
    }
}

@Composable
private fun ReportHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.report_title),
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
private fun ChartEmptyState() {
    ElevatedCard(shape = RoundedCornerShape(Spacing.lg), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.report_empty_state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

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
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Mon-first weekday labels, matching the cells' weekday-major order.
    val dayLabels = remember {
        (0L until 7L).map { DayOfWeek.MONDAY.plus(it).getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.report_activity_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = LocalDate.now().format(DATE_FORMAT),
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant
            )
        }
        // Selected-day readout: DD/MM/AAAA + value.
        Text(
            text = selected?.let { "${it.date.format(DATE_FORMAT)} · ${it.count}" } ?: " ",
            style = MaterialTheme.typography.bodySmall,
            color = onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs)
        )
        Spacer(Modifier.height(Spacing.sm))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().alpha(gridAlpha)) {
            val gap = 4.dp
            val labelWidth = 18.dp
            val gridWidth = maxWidth - labelWidth - Spacing.sm
            val cell = (gridWidth - gap * 11) / 12 // square cell edge
            Row {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    dayLabels.forEach { label ->
                        Box(Modifier.size(width = labelWidth, height = cell), contentAlignment = Alignment.CenterStart) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.width(Spacing.sm))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(12),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(gap),
                    modifier = Modifier
                        .width(gridWidth)
                        .height(cell * 7 + gap * 6)
                ) {
                    items(cells) { itemCell ->
                        val isSelected = selected == itemCell
                        val color = if (itemCell.level == 0) emptyColor else primary.copy(alpha = ALPHA_BY_LEVEL[itemCell.level])
                        Box(
                            modifier = Modifier
                                .size(cell)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
                                    else Modifier
                                )
                                .clickable { selected = itemCell }
                        )
                    }
                }
            }
        }
    }
}

private val ALPHA_BY_LEVEL = floatArrayOf(0f, 0.4f, 0.6f, 0.8f, 1f)

private val BarLabelKey = ExtraStore.Key<List<String>>()

private val BarLabelFormatter = CartesianValueFormatter { context, x, _ ->
    // Must never be blank — Vico throws on blank formatter output.
    context.model.extraStore[BarLabelKey].getOrElse(x.toInt()) { "·" }.ifBlank { "·" }
}

@Composable
private fun BarChart(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    // NOTE: the model transaction below drives Vico's built-in grow-in bar animation.
    // This entrance animation is a protected asset — do not remove it.
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values, labels) {
        modelProducer.runTransaction {
            columnModel { series(values.ifEmpty { listOf(0) }) }
            extras { it[BarLabelKey] = labels }
        }
    }
    val column = rememberLineComponent(
        fill = Fill(barColor),
        thickness = 14.dp,
        shape = RoundedCornerShape(topStartPercent = 40, topEndPercent = 40)
    )
    val label = rememberTextComponent(
        style = androidx.compose.ui.text.TextStyle(color = axisColor, fontSize = 11.sp)
    )
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(ColumnCartesianLayer.ColumnProvider.series(column)),
            // No gridlines: guideline = null on both axes; keep the axis baseline + Y labels.
            startAxis = VerticalAxis.rememberStart(label = label, guideline = null),
            bottomAxis = HorizontalAxis.rememberBottom(label = label, guideline = null, valueFormatter = BarLabelFormatter)
        ),
        modelProducer = modelProducer,
        modifier = modifier
    )
}

private fun ReportRange.labelRes(): Int = when (this) {
    ReportRange.WEEKLY -> R.string.report_range_weekly
    ReportRange.MONTHLY -> R.string.report_range_monthly
}
