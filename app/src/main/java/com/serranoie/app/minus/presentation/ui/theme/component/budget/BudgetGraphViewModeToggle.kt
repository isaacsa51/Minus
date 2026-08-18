package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed

fun budgetGraphViewModeToggleTag(viewMode: BudgetGraphViewMode) =
    "BUDGET_GRAPH_VIEW_MODE_TOGGLE_${viewMode.name}"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BudgetGraphViewModeToggle(
    selected: BudgetGraphViewMode,
    onSelected: (BudgetGraphViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = BudgetGraphViewMode.entries

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
    ) {
        modes.forEachIndexed { index, mode ->
            val isSelected = selected == mode
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelected(mode) },
                modifier = Modifier
                    .testTag(budgetGraphViewModeToggleTag(mode))
                    .height(32.dp),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(
                    text = when (mode) {
                        BudgetGraphViewMode.CUMULATIVE -> stringResource(R.string.budget_graph_view_mode_cumulative)
                        BudgetGraphViewMode.CATEGORIES -> stringResource(R.string.budget_graph_view_mode_categories)
                    },
                    style = MaterialTheme.typography.labelSmallCondensed,
                )
            }
        }
    }
}
