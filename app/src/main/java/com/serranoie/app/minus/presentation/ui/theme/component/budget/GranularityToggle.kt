package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.analytics.GraphGranularity
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed

fun budgetGraphGranularityToggleTag(granularity: GraphGranularity) =
    "BUDGET_GRAPH_TOGGLE_${granularity.name}"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GranularityToggle(
    selected: GraphGranularity,
    totalDays: Int,
    onSelected: (GraphGranularity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableGranularities = remember(totalDays) {
        GraphGranularity.entries.filter { granularity ->
            when (granularity) {
                GraphGranularity.DAYS -> true
                GraphGranularity.WEEK -> totalDays >= 7
                GraphGranularity.BIWEEK -> totalDays >= 14
                GraphGranularity.MONTH -> totalDays >= 30
                GraphGranularity.TOTAL -> true
            }
        }
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        availableGranularities.forEachIndexed { index, granularity ->
            val isSelected = selected == granularity
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelected(granularity) },
                modifier = Modifier
                    .testTag(budgetGraphGranularityToggleTag(granularity))
                    .height(38.dp),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    availableGranularities.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(
                    text = when (granularity) {
                        GraphGranularity.DAYS -> stringResource(R.string.graph_granularity_days)
                        GraphGranularity.WEEK -> stringResource(R.string.graph_granularity_week)
                        GraphGranularity.BIWEEK -> stringResource(R.string.graph_granularity_biweek)
                        GraphGranularity.MONTH -> stringResource(R.string.graph_granularity_month)
                        GraphGranularity.TOTAL -> stringResource(R.string.graph_granularity_total)
                    },
                    style = MaterialTheme.typography.labelSmallCondensed,
                )
            }
        }
    }
}
