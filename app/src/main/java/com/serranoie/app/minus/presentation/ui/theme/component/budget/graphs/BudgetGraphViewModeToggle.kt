package com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed

fun budgetGraphViewModeToggleTag(viewMode: BudgetGraphViewMode) =
    "BUDGET_GRAPH_VIEW_MODE_TOGGLE_${viewMode.name}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetGraphViewModeToggle(
    selected: BudgetGraphViewMode,
    onSelected: (BudgetGraphViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = BudgetGraphViewMode.entries
    val selectedIndex = modes.indexOf(selected).coerceAtLeast(0)

    SecondaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
    ) {
        modes.forEach { mode ->
            Tab(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                modifier = Modifier.testTag(budgetGraphViewModeToggleTag(mode)),
                text = {
                    Text(
                        text = when (mode) {
                            BudgetGraphViewMode.CUMULATIVE -> stringResource(R.string.budget_graph_view_mode_cumulative)
                            BudgetGraphViewMode.CATEGORIES -> stringResource(R.string.budget_graph_view_mode_categories)
                        },
                        style = MaterialTheme.typography.labelSmallCondensed,
                    )
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ViewModePreview() {
    MinusTheme {
        Surface {
            BudgetGraphViewModeToggle(
                selected = BudgetGraphViewMode.CUMULATIVE,
                onSelected = { }
            )
        }
    }
}
