package com.luis.tramo.ui.tasks

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
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
import com.luis.tramo.ui.theme.TaskSwatches
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
    var colorArgb by remember { mutableStateOf(TaskSwatches.first()) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { p ->
                    PriorityButton(
                        value = p,
                        selected = priority == p,
                        onSelect = { priority = p },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Emoji icon grid (8x6).
            Text(stringResource(R.string.task_icon_label), style = MaterialTheme.typography.labelLarge)
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(EMOJIS) { item ->
                    val selected = item == emoji
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else Color.Transparent
                            )
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
                items(TaskSwatches) { argb ->
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

/**
 * Priority selector button. Selected state uses M3 *Container / on*Container role pairs, which are
 * guaranteed legible in both light and dark — this is what fixes the previously unreadable "Low"
 * button (surfaceVariant background + onSurfaceVariant text, never dark-on-dark).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityButton(
    value: TaskPriority,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container: Color
    val content: Color
    when {
        !selected -> {
            container = Color.Transparent
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
        value == TaskPriority.HIGH -> {
            container = MaterialTheme.colorScheme.errorContainer
            content = MaterialTheme.colorScheme.onErrorContainer
        }
        value == TaskPriority.MEDIUM -> {
            container = MaterialTheme.colorScheme.tertiaryContainer
            content = MaterialTheme.colorScheme.onTertiaryContainer
        }
        else -> { // LOW
            container = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(44.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(value.labelRes),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
