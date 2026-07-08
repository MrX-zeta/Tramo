package com.luis.tramo.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.task.Subtask
import com.luis.tramo.data.task.TaskCategory
import com.luis.tramo.data.task.TaskEntity
import com.luis.tramo.data.task.TaskPriority
import com.luis.tramo.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskFilter { ACTIVE, COMPLETED }

/** All fields collected by the task-creation/edit bottom sheet. */
data class NewTaskInput(
    val title: String,
    val iconEmoji: String,
    val category: TaskCategory,
    val priority: TaskPriority,
    val colorArgb: Long,
    val tags: List<String>,
    val subtasks: List<Subtask>,
    val isRecurring: Boolean,
    /** Domain weekdays; the ViewModel maps them to ISO ints at the persistence edge. */
    val recurringDays: List<DayOfWeek>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ACTIVE)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    // Ids swiped-to-delete but still inside their undo window: hidden from the list, not yet removed
    // from Room. The pending Room delete lives in [deleteJobs] so it survives UI recomposition.
    private val _pendingDeletes = MutableStateFlow<Set<Long>>(emptySet())
    private val deleteJobs = mutableMapOf<Long, Job>()

    // Emits the pre-completion task whenever one transitions to done — by checkbox, swipe, or the
    // last subtask rolling up — so the screen shows one consistent "completed" snackbar with undo.
    private val _taskCompleted = MutableSharedFlow<TaskEntity>(extraBufferCapacity = 1)
    val taskCompleted = _taskCompleted.asSharedFlow()

    // Symmetric event for the reverse transition (completed → active), so reopening a task — by
    // unchecking its box or a subtask — also gives feedback.
    private val _taskReopened = MutableSharedFlow<TaskEntity>(extraBufferCapacity = 1)
    val taskReopened = _taskReopened.asSharedFlow()

    val tasks: StateFlow<List<TaskEntity>> = combine(
        _filter.flatMapLatest { filter ->
            when (filter) {
                TaskFilter.ACTIVE -> repository.activeTasks()
                TaskFilter.COMPLETED -> repository.completedTasks()
            }
        },
        _pendingDeletes
    ) { list, pending -> list.filter { it.id !in pending } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    // Unlimited: no gating. (Monetization deferred — the former Pro 5-task limit is removed.)
    fun addTask(input: NewTaskInput) {
        viewModelScope.launch { repository.add(input.toNewEntity()) }
    }

    /** Save an edit into the EXISTING row (id/createdAt/completion preserved), never a new task. */
    fun saveEdit(task: TaskEntity, input: NewTaskInput) {
        viewModelScope.launch {
            // Preserve each unchanged subtask's done state (matched by title) so an edit never wipes progress.
            val mergedSubtasks = input.subtasks.map { edited ->
                task.subtasks.firstOrNull { it.title == edited.title }?.let { edited.copy(done = it.done) } ?: edited
            }
            repository.update(
                task.copy(
                    title = input.title.trim(),
                    category = input.category,
                    priority = input.priority,
                    tags = input.tags,
                    subtasks = mergedSubtasks,
                    iconEmoji = input.iconEmoji,
                    colorArgb = input.colorArgb,
                    isRecurring = input.isRecurring,
                    recurringDays = input.recurringDays.map { it.value }.sorted()
                )
            )
        }
    }

    /**
     * Swipe-right / checkbox. Completing an active task also marks its subtasks done (rollup);
     * reopening a completed one flips it back to active. Tab-aware because [task.isCompleted] differs
     * between the Activas and Completadas lists.
     */
    fun toggleTaskCompleted(task: TaskEntity) {
        val completing = !task.isCompleted
        val subtasks =
            if (completing && task.subtasks.isNotEmpty()) task.subtasks.map { it.copy(done = true) }
            else task.subtasks
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = completing, subtasks = subtasks))
            if (completing) _taskCompleted.emit(task) else _taskReopened.emit(task)
        }
    }

    /** Toggle a subtask; completing the last one rolls the parent up to complete (and vice-versa). */
    fun toggleSubtask(task: TaskEntity, index: Int) {
        val updated = task.subtasks.toMutableList()
        val current = updated.getOrNull(index) ?: return
        updated[index] = current.copy(done = !current.done)
        val allDone = updated.isNotEmpty() && updated.all { it.done }
        val justCompleted = allDone && !task.isCompleted
        val justReopened = !allDone && task.isCompleted
        viewModelScope.launch {
            repository.update(task.copy(subtasks = updated, isCompleted = allDone))
            if (justCompleted) _taskCompleted.emit(task)
            else if (justReopened) _taskReopened.emit(task)
        }
    }

    /** Restore a task to a prior snapshot — used to undo a completion (reopens + restores subtasks). */
    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch { repository.update(task) }
    }

    /** Swipe-left: hide the task immediately and commit the Room delete only after the undo window. */
    fun deleteWithUndo(task: TaskEntity) {
        _pendingDeletes.update { it + task.id }
        deleteJobs.remove(task.id)?.cancel()
        deleteJobs[task.id] = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            repository.delete(task)
            _pendingDeletes.update { it - task.id }
            deleteJobs.remove(task.id)
        }
    }

    /** "Deshacer": cancel the pending delete and restore the task in place. */
    fun undoDelete(id: Long) {
        deleteJobs.remove(id)?.cancel()
        _pendingDeletes.update { it - id }
    }

    private fun NewTaskInput.toNewEntity() = TaskEntity(
        title = title.trim(),
        category = category,
        priority = priority,
        tags = tags,
        subtasks = subtasks,
        iconEmoji = iconEmoji,
        colorArgb = colorArgb,
        isRecurring = isRecurring,
        // Persist ISO weekday numbers (1..7), never a UI list index.
        recurringDays = recurringDays.map { it.value }.sorted(),
        createdAt = System.currentTimeMillis()
    )

    companion object {
        const val UNDO_WINDOW_MS = 4000L
    }
}
