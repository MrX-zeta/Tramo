package com.luis.tramo.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import com.luis.tramo.R
import com.luis.tramo.data.task.Subtask
import com.luis.tramo.data.task.TaskCategory
import com.luis.tramo.data.task.TaskPriority
import kotlinx.coroutines.delay

private data class TaskTemplate(val labelRes: Int, val title: String, val emoji: String)

private val TEMPLATES = listOf(
    TaskTemplate(R.string.template_meeting, "Meeting", "📅"),
    TaskTemplate(R.string.template_email, "Email", "✉️"),
    TaskTemplate(R.string.template_coding, "Coding", "💻"),
    TaskTemplate(R.string.template_design, "Design", "🎨")
)

private val EMOJIS = listOf(
    "📅", "✉️", "💻", "🎨", "📞", "📝", "📊", "📈",
    "🗂️", "📌", "🔖", "✅", "⏰", "🔔", "💡", "🚀",
    "🎯", "🧠", "📚", "✏️", "🖊️", "🖥️", "📱", "⌨️",
    "🖱️", "🗓️", "📋", "📎", "🔍", "🔧", "⚙️", "🛠️",
    "🧪", "🔬", "🩺", "💊", "🏃", "🧘", "🍎", "☕",
    "🍵", "🎧", "🎵", "🎬", "📷", "🌱", "🔥", "⭐"
)

private val SWATCHES = listOf(
    0xFF6366F1L, 0xFF0EA5A4L, 0xFFF59E0BL, 0xFFEF4444L, 0xFF10B981L,
    0xFF3B82F6L, 0xFFEC4899L, 0xFF8B5CF6L, 0xFF64748BL, 0xFFF97316L
)

private val DAY_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskSheet(
    onDismiss: () -> Unit,
    onConfirm: (NewTaskInput) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboard = LocalSoftwareKeyboardController.current
    val titleFocus = remember { FocusRequester() }

    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var colorArgb by remember { mutableStateOf(SWATCHES.first()) }
    var recurring by remember { mutableStateOf(false) }
    val recurringDays = remember { mutableStateListOf<Int>() }

    // Auto-open the keyboard on the title once the sheet has settled (less friction).
    LaunchedEffect(Unit) {
        delay(350)
        titleFocus.requestFocus()
        keyboard?.show()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick-fill templates.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TEMPLATES) { template ->
                    AssistChip(
                        onClick = {
                            title = template.title
                            emoji = template.emoji
                        },
                        label = { Text("${template.emoji} ${stringResource(template.labelRes)}") }
                    )
                }
            }

            // Title (auto-focused) with the chosen emoji as a leading indicator.
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_title_label)) },
                singleLine = true,
                leadingIcon = if (emoji.isNotEmpty()) {
                    { Text(emoji, fontSize = 20.sp) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus)
            )

            // Priority.
            Text(stringResource(R.string.task_priority_label), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityChip(TaskPriority.HIGH, priority, MaterialTheme.colorScheme.error) { priority = it }
                PriorityChip(TaskPriority.MEDIUM, priority, WarningColor) { priority = it }
                PriorityChip(TaskPriority.LOW, priority, MaterialTheme.colorScheme.surfaceVariant) { priority = it }
            }

            // Emoji icon grid (8x6).
            Text(stringResource(R.string.task_icon_label), style = MaterialTheme.typography.labelLarge)
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                items(EMOJIS) { item ->
                    val selected = item == emoji
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(
                                width = if (selected) 2.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { emoji = if (selected) "" else item },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item, fontSize = 20.sp)
                    }
                }
            }

            // Color swatches.
            Text(stringResource(R.string.task_color_label), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(SWATCHES) { argb ->
                    val selected = argb == colorArgb
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .padding(3.dp)
                            .background(Color(argb), CircleShape)
                            .clickable { colorArgb = argb }
                    )
                }
            }

            // Recurring toggle + day-of-week selector.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.task_recurring_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = recurring, onCheckedChange = { recurring = it })
            }
            if (recurring) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_LABELS.forEachIndexed { index, label ->
                        val selected = index in recurringDays
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) recurringDays.remove(index) else recurringDays.add(index)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onConfirm(
                        NewTaskInput(
                            title = title,
                            iconEmoji = emoji,
                            category = TaskCategory.WORK,
                            priority = priority,
                            colorArgb = colorArgb,
                            tags = emptyList(),
                            subtasks = emptyList(),
                            isRecurring = recurring,
                            recurringDays = recurringDays.sorted().toList()
                        )
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.task_add_confirm))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityChip(
    value: TaskPriority,
    selected: TaskPriority,
    color: Color,
    onSelect: (TaskPriority) -> Unit
) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelect(value) },
        label = { Text(stringResource(value.labelRes), fontWeight = FontWeight.Medium) },
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = color)
    )
}

private val WarningColor = Color(0xFFF59E0B)
