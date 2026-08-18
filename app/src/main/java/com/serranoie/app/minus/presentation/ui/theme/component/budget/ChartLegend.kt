package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R

/** A single colored dot + label pair for [ChartLegend]. */
data class LegendEntry(val label: String, val color: Color)

/**
 * A wrapping row of colored-dot legend entries, reusable across any chart that needs to explain
 * what its colors mean (series names, category breakdowns, etc.).
 */
@Composable
fun ChartLegend(entries: List<LegendEntry>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        entries.forEach { entry ->
            LegendItem(color = entry.color, label = entry.label)
        }
    }
}

@Composable
internal fun BudgetGraphLegend() {
    ChartLegend(
        entries = listOf(
            LegendEntry(
                label = stringResource(R.string.budget_graph_legend_current),
                color = MaterialTheme.colorScheme.tertiary,
            ),
            LegendEntry(
                label = stringResource(R.string.budget_graph_legend_previous),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
            ),
        )
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
