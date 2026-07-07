package com.luis.tramo.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luis.tramo.R

/** A supported app language. An empty [tag] means "follow the system locale". */
internal data class AppLocale(val tag: String, val endonym: String)

/** Supported locales at v1. Endonyms are proper nouns, so they read the same in every language. */
internal val SUPPORTED_LOCALES = listOf(
    AppLocale("", ""), // system default — label resolved via string resource
    AppLocale("en", "English"),
    AppLocale("es", "Español"),
    AppLocale("fr", "Français"),
    AppLocale("de", "Deutsch"),
    AppLocale("ja", "日本語"),
    AppLocale("pt", "Português")
)

@Composable
internal fun localeLabel(locale: AppLocale): String =
    if (locale.tag.isEmpty()) stringResource(R.string.settings_language_system) else locale.endonym

@Composable
internal fun localeLabelFor(tag: String): String {
    val match = SUPPORTED_LOCALES.firstOrNull { it.tag == tag } ?: SUPPORTED_LOCALES.first()
    return localeLabel(match)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocalePickerSheet(
    currentTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SUPPORTED_LOCALES, key = { it.tag.ifEmpty { "system" } }) { locale ->
                LocaleRow(
                    label = localeLabel(locale),
                    selected = locale.tag == currentTag,
                    onClick = { onSelect(locale.tag) }
                )
            }
        }
    }
}

@Composable
private fun LocaleRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) borderColor else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                // Checkmark glyph (avoids pulling in the material-icons-extended artifact).
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
