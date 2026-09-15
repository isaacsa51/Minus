package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed

internal enum class SubscriptionsViewMode { WHOLE_PERIOD, WEEKLY, CATEGORY }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SubscriptionsViewModeToggle(
    selected: SubscriptionsViewMode,
    onSelected: (SubscriptionsViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = SubscriptionsViewMode.entries
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        modes.forEachIndexed { index, mode ->
            ToggleButton(
                checked = selected == mode,
                onCheckedChange = { onSelected(mode) },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(
                    text = when (mode) {
                        SubscriptionsViewMode.WHOLE_PERIOD -> stringResource(R.string.subscriptions_view_mode_whole_period)
                        SubscriptionsViewMode.WEEKLY -> stringResource(R.string.subscriptions_view_mode_weekly)
                        SubscriptionsViewMode.CATEGORY -> stringResource(R.string.subscriptions_view_mode_category)
                    },
                    style = MaterialTheme.typography.labelSmallCondensed,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionsViewModeTogglePreview() {
    MinusTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SubscriptionsViewMode.entries.forEach { mode ->
                SubscriptionsViewModeToggle(
                    selected = mode,
                    onSelected = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
