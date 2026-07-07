package com.luis.tramo.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.tramo.R

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // --- Timer group ---
            SettingsGroup(title = stringResource(R.string.settings_timer_section)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DurationField(
                        label = stringResource(R.string.settings_focus_minutes),
                        value = state.focusInput,
                        error = state.focusError,
                        onValueChange = viewModel::onFocusChange,
                        modifier = Modifier.weight(1f)
                    )
                    DurationField(
                        label = stringResource(R.string.settings_break_minutes),
                        value = state.breakInput,
                        error = state.breakError,
                        onValueChange = viewModel::onBreakChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- Preferences group ---
            val systemDark = isSystemInDarkTheme()
            SettingsGroup(title = stringResource(R.string.settings_section_general)) {
                SettingRow(
                    title = stringResource(R.string.settings_dark_mode),
                    subtitle = if (state.darkOverride != null) {
                        stringResource(R.string.settings_follow_system)
                    } else null,
                    onSubtitleClick = { viewModel.setDarkOverride(null) }
                ) {
                    AnimatedSwitch(
                        checked = state.darkOverride ?: systemDark,
                        onCheckedChange = { viewModel.setDarkOverride(it) }
                    )
                }

                RowDivider()

                SettingRow(title = stringResource(R.string.settings_daily_goal)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedIconButton(
                            onClick = { viewModel.setDailyGoal(state.dailyGoal - 1) },
                            modifier = Modifier.size(40.dp)
                        ) { Text("–", style = MaterialTheme.typography.titleLarge) }
                        Text(
                            text = state.dailyGoal.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        OutlinedIconButton(
                            onClick = { viewModel.setDailyGoal(state.dailyGoal + 1) },
                            modifier = Modifier.size(40.dp)
                        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }
                }

                RowDivider()

                SettingRow(
                    title = stringResource(R.string.settings_language),
                    modifier = Modifier.clickable { showLanguageDialog = true }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = localeLabelFor(state.languageTag),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "  ›",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LocalePickerSheet(
            currentTag = state.languageTag,
            onSelect = {
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

/** A titled section rendered as an elevated card with pronounced rounded corners. */
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        ElevatedCard(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/** A single row: title (with optional tappable subtitle) on the left, trailing control on the right. */
@Composable
private fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onSubtitleClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                TextButton(
                    onClick = { onSubtitleClick?.invoke() },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        trailing()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun DurationField(
    label: String,
    value: String,
    error: FieldError?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = error?.let { { Text(stringResource(errorTextFor(it))) } },
        modifier = modifier
    )
}

private fun errorTextFor(error: FieldError): Int = when (error) {
    FieldError.INVALID_NUMBER -> R.string.settings_error_invalid_number
    FieldError.FOCUS_BELOW_BREAK -> R.string.settings_error_focus_below_break
}
