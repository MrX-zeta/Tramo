package com.luis.tramo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luis.tramo.ui.theme.Spacing
import com.luis.tramo.ui.theme.TabularFigures

/**
 * A fixed-height stat card with tabular figures and an optional leading icon. The fixed height +
 * reserved two-line label keep neighbouring cards aligned regardless of label length.
 */
@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary
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
            Column {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(Spacing.xs))
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.merge(TabularFigures),
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    maxLines = 1
                )
            }
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

// Sized so the Report's lower block (KPI row + heatmap + monthly card) fits within one viewport,
// letting the KPI cards stay fully visible at max scroll instead of being clipped by the app bar.
private val STAT_CARD_HEIGHT = 144.dp
