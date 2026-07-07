package com.luis.tramo.data.task

import androidx.annotation.StringRes
import com.luis.tramo.R
import kotlinx.serialization.Serializable

/** Category drives the color-coded left border of a task card. */
enum class TaskCategory(val colorArgb: Long, @param:StringRes val labelRes: Int) {
    WORK(0xFF6366F1L, R.string.category_work),
    STUDY(0xFF0EA5A4L, R.string.category_study),
    PERSONAL(0xFFF59E0BL, R.string.category_personal),
    HEALTH(0xFFEF4444L, R.string.category_health)
}

enum class TaskPriority(@param:StringRes val labelRes: Int) {
    LOW(R.string.priority_low),
    MEDIUM(R.string.priority_medium),
    HIGH(R.string.priority_high)
}

@Serializable
data class Subtask(
    val title: String,
    val done: Boolean = false
)
