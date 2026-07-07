package com.luis.tramo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R

private data class LanguageOption(val tag: String, val labelRes: Int)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("", R.string.settings_language_system),
    LanguageOption("en", R.string.settings_language_english),
    LanguageOption("es", R.string.settings_language_spanish)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showLanguageDialog by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall
            )

            // Timer preset.
            SettingSection(title = stringResource(R.string.settings_focus_preset)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val presets = SettingsViewModel.FOCUS_PRESETS
                    presets.forEachIndexed { index, minutes ->
                        SegmentedButton(
                            selected = state.focusPreset == minutes,
                            onClick = { viewModel.setFocusPreset(minutes) },
                            shape = SegmentedButtonDefaults.itemShape(index, presets.size)
                        ) {
                            Text(stringResource(R.string.settings_minutes, minutes))
                        }
                    }
                }
            }

            HorizontalDivider()

            // Dark mode override.
            val systemDark = isSystemInDarkTheme()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_dark_mode), style = MaterialTheme.typography.titleMedium)
                    if (state.darkOverride != null) {
                        TextButton(
                            onClick = { viewModel.setDarkOverride(null) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text(stringResource(R.string.settings_follow_system))
                        }
                    }
                }
                Switch(
                    checked = state.darkOverride ?: systemDark,
                    onCheckedChange = { viewModel.setDarkOverride(it) }
                )
            }

            HorizontalDivider()

            // Daily goal stepper.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_daily_goal),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedIconButton(onClick = { viewModel.setDailyGoal(state.dailyGoal - 1) }) {
                    Text("–", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = state.dailyGoal.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                OutlinedIconButton(onClick = { viewModel.setDailyGoal(state.dailyGoal + 1) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            HorizontalDivider()

            // Language row → modal.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLanguageDialog = true }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(languageLabelFor(state.languageTag)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            current = state.languageTag,
            onSelect = {
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        content()
    }
}

@Composable
private fun LanguagePickerDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                LANGUAGE_OPTIONS.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = current == option.tag,
                                onClick = { onSelect(option.tag) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == option.tag,
                            onClick = { onSelect(option.tag) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(option.labelRes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_close)) }
        }
    )
}

private fun languageLabelFor(tag: String): Int =
    LANGUAGE_OPTIONS.firstOrNull { it.tag == tag }?.labelRes ?: R.string.settings_language_system
