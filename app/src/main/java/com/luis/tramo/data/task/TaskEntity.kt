package com.luis.tramo.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: TaskCategory,
    val priority: TaskPriority,
    val tags: List<String>,
    val subtasks: List<Subtask>,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L
) {
    val completedSubtasks: Int get() = subtasks.count { it.done }

    /** completed / total subtasks, 0f when there are no subtasks. */
    val progress: Float
        get() = if (subtasks.isEmpty()) 0f else completedSubtasks.toFloat() / subtasks.size
}
