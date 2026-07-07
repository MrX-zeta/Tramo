package com.luis.tramo.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.tramo.data.task.Subtask
import com.luis.tramo.data.task.TaskCategory
import com.luis.tramo.data.task.TaskEntity
import com.luis.tramo.data.task.TaskPriority
import com.luis.tramo.data.task.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskFilter { ACTIVE, COMPLETED }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ACTIVE)
    val filter: StateFlow<TaskFilter> = _filter.asStateFlow()

    private val _showProUpsell = MutableStateFlow(false)
    val showProUpsell: StateFlow<Boolean> = _showProUpsell.asStateFlow()

    val tasks: StateFlow<List<TaskEntity>> = _filter
        .flatMapLatest { filter ->
            when (filter) {
                TaskFilter.ACTIVE -> repository.activeTasks()
                TaskFilter.COMPLETED -> repository.completedTasks()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun addTask(
        title: String,
        category: TaskCategory,
        priority: TaskPriority,
        tags: List<String>,
        subtasks: List<Subtask>
    ) {
        viewModelScope.launch {
            // TODO: gate by real Pro entitlement once billing exists.
            if (repository.count() >= FREE_TASK_LIMIT) {
                _showProUpsell.value = true
                return@launch
            }
            repository.add(
                TaskEntity(
                    title = title.trim(),
                    category = category,
                    priority = priority,
                    tags = tags,
                    subtasks = subtasks,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun toggleSubtask(task: TaskEntity, index: Int) {
        val updated = task.subtasks.toMutableList()
        val current = updated.getOrNull(index) ?: return
        updated[index] = current.copy(done = !current.done)
        viewModelScope.launch {
            repository.update(task.copy(subtasks = updated))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.delete(task) }
    }

    fun dismissProUpsell() {
        _showProUpsell.value = false
    }

    companion object {
        const val FREE_TASK_LIMIT = 5
    }
}
