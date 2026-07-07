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
    val createdAt: Long = 0L,
    val iconEmoji: String = "",
    /** Custom card color; 0 falls back to the category color. */
    val colorArgb: Long = 0L,
    val isRecurring: Boolean = false,
    /** Recurring days as ISO-8601 [java.time.DayOfWeek] values (1 = Monday .. 7 = Sunday). */
    val recurringDays: List<Int> = emptyList()
) {
    val completedSubtasks: Int get() = subtasks.count { it.done }

    /** The card's effective accent color: custom if set, otherwise the category color. */
    val effectiveColorArgb: Long get() = if (colorArgb != 0L) colorArgb else category.colorArgb

    /** completed / total subtasks, 0f when there are no subtasks. */
    val progress: Float
        get() = if (subtasks.isEmpty()) 0f else completedSubtasks.toFloat() / subtasks.size
}
