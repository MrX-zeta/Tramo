package com.luis.tramo.ui.tasks

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.luis.tramo.navigation.TramoTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.luis.tramo.ui.components.ScreenEntrance
import com.luis.tramo.ui.components.rememberReduceMotion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R
import com.luis.tramo.data.task.TaskEntity
import com.luis.tramo.ui.theme.TramoTheme
import kotlinx.coroutines.launch

// Asymmetric positional thresholds (fraction of the row width). The destructive delete deliberately
// needs far more travel than the reversible complete, so an accidental swipe can't delete.
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
                                onToggleCompleted = { viewModel.toggleTaskCompleted(task) },
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
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
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
    // Reactive reflow when the row leaves the list; instant under reduced motion.
    val rowModifier = if (reduceMotion) Modifier else Modifier.animateItem()
    SwipeToDismissBox(
        state = dismissState,
        modifier = rowModifier,
        backgroundContent = { SwipeBackground(dismissState, shape) },
        onDismiss = { direction ->
            when (direction) {
                // Reversible: mark complete/reopen and let the list flow remove the row (no hard dismiss).
                SwipeToDismissBoxValue.StartToEnd -> onToggleCompleted()
                // Destructive: hide + snackbar undo; the Room delete commits only after the undo window.
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                SwipeToDismissBoxValue.Settled -> {}
            }
        }
    ) {
        TaskCard(
            task = task,
            onClick = onEdit,
            onToggleCompleted = onToggleCompleted,
            onToggleSubtask = onToggleSubtask
        )
    }
}

@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState, shape: Shape) {
    val direction = state.dismissDirection
    val isDelete = direction == SwipeToDismissBoxValue.EndToStart
    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(color)
            .padding(horizontal = 24.dp),
        // Icon pinned to the acting edge, vertically centered, fixed while the row slides over it.
        contentAlignment = if (isDelete) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            val progress = state.progress.coerceIn(0f, 1f)
            Icon(
                imageVector = if (isDelete) Icons.Filled.Delete else Icons.Filled.Check,
                contentDescription = stringResource(
                    if (isDelete) R.string.task_delete_action else R.string.task_complete_action
                ),
                tint = if (isDelete) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        val s = 0.6f + 0.4f * progress
                        scaleX = s
                        scaleY = s
                        alpha = progress
                    }
            )
        }
    }
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
            // Color-coded accent border (custom color, or category color).
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
