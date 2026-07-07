package com.luis.tramo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luis.tramo.ui.theme.Spacing
import com.luis.tramo.ui.theme.TabularFigures

/**
 * A fixed-height stat card with tabular figures. The fixed height means a wrapping label never
 * makes one card render taller than its neighbors, and tabular digits keep the number from shifting.
 */
@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    ElevatedCard(
        shape = RoundedCornerShape(Spacing.lg),
        modifier = modifier.height(STAT_CARD_HEIGHT)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.merge(TabularFigures),
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
            // Reserve two lines so a wrapping label never changes the value's position or the
            // card's content height — every card in a row stays aligned.
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private val STAT_CARD_HEIGHT = 104.dp
