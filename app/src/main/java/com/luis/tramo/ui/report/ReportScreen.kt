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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R
import com.luis.tramo.ui.components.StatCard
import com.luis.tramo.ui.theme.Spacing
import com.luis.tramo.ui.theme.TabularFigures
import com.luis.tramo.ui.theme.TramoTheme
import com.luis.tramo.util.formatDuration
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
private val CARD_SHAPE = RoundedCornerShape(Spacing.xl)

@Composable
fun ReportScreen(
    bottomBar: @Composable () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today by viewModel.todayState.collectAsStateWithLifecycle()
    val heat by viewModel.heatmapState.collectAsStateWithLifecycle()
    val monthly by viewModel.monthlyState.collectAsStateWithLifecycle()

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
            TodayCard(today)
            OverviewCard(state = state, onSelectRange = viewModel::selectRange)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                StatCard(
                    value = heat.totalCompletions.toString(),
                    label = stringResource(R.string.report_total_sessions),
                    valueColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Filled.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = heat.activeDays.toString(),
                    label = stringResource(R.string.report_active_days),
                    valueColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Filled.LocalFireDepartment,
                    iconTint = TramoTheme.progress,
                    modifier = Modifier.weight(1f)
                )
            }
            HeatmapCard(cells = heat.cells, columnLabels = remember { viewModel.heatmapColumnLabels() })
            MonthlyCard(monthly = monthly)
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

// --- Card 1: today's focus summary ---

@Composable
private fun TodayCard(today: TodayUiState) {
    val focusMinutes = today.focusSeconds / 60
    val breakMinutes = today.breakSeconds / 60
    val total = today.focusSeconds + today.breakSeconds
    val progress = if (total > 0) today.focusSeconds.toFloat() / total else 0f

    ElevatedCard(shape = CARD_SHAPE, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(
                text = stringResource(R.string.report_today_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = formatDuration(today.focusSeconds),
                style = MaterialTheme.typography.displaySmall.merge(TabularFigures),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                BreakdownItem(stringResource(R.string.report_label_focus), "$focusMinutes min", MaterialTheme.colorScheme.primary)
                BreakdownItem(stringResource(R.string.report_label_break), "$breakMinutes min", MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(Spacing.md))
            LinearProgressIndicator(
                progress = { progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

@Composable
private fun BreakdownItem(label: String, value: String, dotColor: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(dotColor))
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "$label  ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.merge(TabularFigures),
            fontWeight = FontWeight.SemiBold
        )
    }
}

// --- Card 2: Semanal/Mensual overview with stats + per-day chart ---

@Composable
private fun OverviewCard(state: ReportUiState, onSelectRange: (ReportRange) -> Unit) {
    ElevatedCard(shape = CARD_SHAPE, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.report_focus_time),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                SingleChoiceSegmentedButtonRow {
                    ReportRange.entries.forEachIndexed { index, range ->
                        SegmentedButton(
                            selected = state.range == range,
                            onClick = { onSelectRange(range) },
                            shape = SegmentedButtonDefaults.itemShape(index, ReportRange.entries.size)
                        ) {
                            Text(stringResource(range.labelRes()))
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.lg))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(formatDuration(state.rangeFocusSeconds), stringResource(R.string.report_total_focus_time))
                StatColumn(formatDuration(state.avgSessionSeconds), stringResource(R.string.report_avg_focus))
                StatColumn(state.rangeSessionCount.toString(), stringResource(R.string.report_total_sessions))
            }
            Spacer(Modifier.height(Spacing.lg))
            if (state.dailyMinutes.all { it == 0 }) {
                EmptyChartText()
            } else {
                BarChart(
                    values = state.dailyMinutes,
                    labels = state.dailyLabels,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.merge(TabularFigures),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 2
        )
    }
}

// --- Card 4: heatmap ---

@Composable
private fun HeatmapCard(cells: List<HeatmapCell>, columnLabels: List<String>) {
    ElevatedCard(shape = CARD_SHAPE, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            if (cells.all { it.count == 0 }) {
                Text(stringResource(R.string.report_activity_map), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Spacing.sm))
                EmptyChartText()
            } else {
                ActivityHeatmap(cells = cells, columnLabels = columnLabels)
            }
        }
    }
}

@Composable
private fun ActivityHeatmap(cells: List<HeatmapCell>, columnLabels: List<String>) {
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

    val dayLabels = remember {
        (0L until 7L).map { DayOfWeek.MONDAY.plus(it).getDisplayName(TextStyle.NARROW, Locale.getDefault()) }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.report_activity_map),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = selected?.date?.format(DATE_FORMAT) ?: LocalDate.now().format(DATE_FORMAT),
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant
            )
        }
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
            val cell = (gridWidth - gap * 11) / 12
            Column {
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
                        modifier = Modifier.width(gridWidth).height(cell * 7 + gap * 6)
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
                // X axis: month label under each of the 12 week-columns (shown where the month changes).
                Spacer(Modifier.height(Spacing.xs))
                Row(modifier = Modifier.padding(start = labelWidth + Spacing.sm)) {
                    columnLabels.forEachIndexed { index, label ->
                        Box(Modifier.size(width = cell, height = 12.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                        }
                        if (index < columnLabels.lastIndex) Spacer(Modifier.width(gap))
                    }
                }
            }
        }
    }
}

private val ALPHA_BY_LEVEL = floatArrayOf(0f, 0.4f, 0.6f, 0.8f, 1f)

// --- Card 5: monthly sessions ---

@Composable
private fun MonthlyCard(monthly: MonthlyUiState) {
    ElevatedCard(shape = CARD_SHAPE, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(stringResource(R.string.report_monthly_activity), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.lg))
            if (monthly.counts.all { it == 0 }) {
                EmptyChartText()
            } else {
                BarChart(
                    values = monthly.counts,
                    labels = monthly.labels,
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChartText() {
    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.report_empty_state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// --- Shared bar chart (protected entrance animation via runTransaction) ---

private val BarLabelKey = ExtraStore.Key<List<String>>()

private val BarLabelFormatter = CartesianValueFormatter { context, x, _ ->
    context.model.extraStore[BarLabelKey].getOrElse(x.toInt()) { "·" }.ifBlank { "·" }
}

@Composable
private fun BarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    // The transaction drives Vico's built-in grow-in bar animation — a PROTECTED asset. Do not remove.
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
