package com.luis.tramo.ui.tasks

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R
import com.luis.tramo.data.task.TaskEntity
import com.luis.tramo.navigation.TramoTopBar
import com.luis.tramo.ui.components.ScreenEntrance
import com.luis.tramo.ui.components.rememberReduceMotion
import com.luis.tramo.ui.theme.TramoTheme
import kotlinx.coroutines.launch

private const val COMPLETE_THRESHOLD = 0.28f
private const val DELETE_THRESHOLD = 0.55f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    bottomBar: @Composable () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val reduceMotion = rememberReduceMotion()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.task_deleted)
    val completedMessage = stringResource(R.string.task_completed)
    val undoLabel = stringResource(R.string.action_undo)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TramoTopBar(R.string.timer_open_tasks, onOpenSettings, scrollBehavior) },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingTask = null; showSheet = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ScreenEntrance(index = 0, visible = visible, reduceMotion = reduceMotion) {
                PrimaryTabRow(selectedTabIndex = filter.ordinal) {
                    TaskFilter.entries.forEach { entry ->
                        Tab(
                            selected = filter == entry,
                            onClick = { viewModel.selectFilter(entry) },
                            text = { Text(stringResource(entry.labelRes())) }
                        )
                    }
                }
            }
            ScreenEntrance(index = 1, visible = visible, reduceMotion = reduceMotion, modifier = Modifier.weight(1f)) {
                if (tasks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.task_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            SwipeableTaskRow(
                                task = task,
                                reduceMotion = reduceMotion,
                                canComplete = filter == TaskFilter.ACTIVE,
                                onSwipeComplete = {
                                    viewModel.toggleTaskCompleted(task)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(completedMessage, duration = SnackbarDuration.Short)
                                    }
                                },
                                onToggleChecked = { viewModel.toggleTaskCompleted(task) },
                                onEdit = { editingTask = task; showSheet = true },
                                onToggleSubtask = { index -> viewModel.toggleSubtask(task, index) },
                                onDelete = {
                                    viewModel.deleteWithUndo(task)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = deletedMessage,
                                            actionLabel = undoLabel,
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(task.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (showSheet) {
        AddTaskSheet(
            editing = editingTask,
            onDismiss = { showSheet = false },
            onConfirm = { input ->
                val target = editingTask
                if (target != null) viewModel.saveEdit(target, input) else viewModel.addTask(input)
                showSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LazyItemScope.SwipeableTaskRow(
    task: TaskEntity,
    reduceMotion: Boolean,
    canComplete: Boolean,
    onSwipeComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleChecked: () -> Unit,
    onToggleSubtask: (Int) -> Unit
) {
    lateinit var dismissState: SwipeToDismissBoxState
    dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { total ->
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) total * DELETE_THRESHOLD
            else total * COMPLETE_THRESHOLD
        }
    )
    val shape = RoundedCornerShape(16.dp)
    val rowModifier = if (reduceMotion) Modifier else Modifier.animateItem()

    val crossed = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
    val lidRotation by animateFloatAsState(
        targetValue = if (crossed) -35f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "lid_rotation"
    )
    val isCompleting = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
    val rawProgress = if (isCompleting) {
        val offset = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
        (offset / 300f).coerceIn(0f, 1f)
    } else 0f
    val checkProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "check_progress"
    )

    LaunchedEffect(dismissState.settledValue) {
        when (dismissState.settledValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                onSwipeComplete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete()
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> {}
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = rowModifier,
        enableDismissFromStartToEnd = canComplete,
        backgroundContent = { SwipeBackground(dismissState, shape, lidRotation, checkProgress) }
    ) {
        TaskCard(
            task = task,
            onClick = onEdit,
            onToggleCompleted = onToggleChecked,
            onToggleSubtask = onToggleSubtask
        )
    }
}

@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState, shape: Shape, lidRotation: Float, checkProgress: Float) {
    val direction = state.dismissDirection
    val isDelete = direction == SwipeToDismissBoxValue.EndToStart
    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }
    val iconColor = if (isDelete) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
    val completeDesc = stringResource(R.string.task_complete_action)
    val deleteDesc = stringResource(R.string.task_delete_action)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(color)
            .padding(horizontal = 26.dp),
        contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            val progress = state.progress.coerceIn(0f, 1f)
            if (isDelete) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .semantics { contentDescription = deleteDesc }
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_trash_lid),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                rotationZ = lidRotation
                                transformOrigin = TransformOrigin(0.15f, 0.3f)
                            }
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_trash_body),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .size(30.dp)
                        .semantics { contentDescription = completeDesc }
                ) {
                    drawStretchCheck(checkProgress, iconColor)
                }
            }
        }
    }
}

private fun DrawScope.drawStretchCheck(progress: Float, color: Color) {
    if (progress <= 0.01f) return
    val w = size.width
    val h = size.height
    val stroke = w * 0.13f
    val style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val p1 = Offset(0.14f * w, 0.54f * h)
    val elbow = Offset(0.40f * w, 0.78f * h)
    val end = Offset(0.86f * w, 0.26f * h)

    val shortLen = kotlin.math.hypot(elbow.x - p1.x, elbow.y - p1.y)
    val longLen = kotlin.math.hypot(end.x - elbow.x, end.y - elbow.y)
    val totalLen = shortLen + longLen
    val drawn = progress * totalLen

    val path = Path().apply {
        moveTo(p1.x, p1.y)
        if (drawn <= shortLen) {
            val a = drawn / shortLen
            lineTo(p1.x + (elbow.x - p1.x) * a, p1.y + (elbow.y - p1.y) * a)
        } else {
            lineTo(elbow.x, elbow.y)
            val b = ((drawn - shortLen) / longLen).coerceIn(0f, 1f)
            lineTo(elbow.x + (end.x - elbow.x) * b, elbow.y + (end.y - elbow.y) * b)
        }
    }
    drawPath(path, color, style = style)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskCard(
    task: TaskEntity,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit,
    onToggleSubtask: (Int) -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(Color(task.effectiveColorArgb))
            )
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.iconEmoji.isNotEmpty()) {
                        Text(
                            text = task.iconEmoji,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f)
                    )
                    PriorityBadge(task)
                    Checkbox(checked = task.isCompleted, onCheckedChange = { onToggleCompleted() })
                }
                if (task.tags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        task.tags.take(4).forEach { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }
                if (task.subtasks.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { task.progress },
                        color = TramoTheme.progress,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.task_subtask_progress,
                            task.completedSubtasks,
                            task.subtasks.size
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    task.subtasks.forEachIndexed { index, subtask ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = subtask.done,
                                onCheckedChange = { onToggleSubtask(index) }
                            )
                            Text(
                                text = subtask.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (subtask.done) TextDecoration.LineThrough else null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityBadge(task: TaskEntity) {
    Text(
        text = stringResource(task.priority.labelRes),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

private fun TaskFilter.labelRes(): Int = when (this) {
    TaskFilter.ACTIVE -> R.string.task_filter_active
    TaskFilter.COMPLETED -> R.string.task_filter_completed
}