package com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback

const val BUDGET_GRAPH_PREV_PAGE_TAG = "BUDGET_GRAPH_PREV_PAGE_TAG"
const val BUDGET_GRAPH_NEXT_PAGE_TAG = "BUDGET_GRAPH_NEXT_PAGE_TAG"
const val BUDGET_GRAPH_WINDOW_LABEL_TAG = "BUDGET_GRAPH_WINDOW_LABEL_TAG"

@Composable
internal fun BudgetGraphNavigation(
    currentWindow: Int,
    totalWindows: Int,
    onPrevWindow: () -> Unit,
    onNextWindow: () -> Unit,
) {
    val view = LocalView.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            modifier = Modifier
                .size(32.dp)
                .testTag(BUDGET_GRAPH_PREV_PAGE_TAG),
            onClick = {
                view.confirmFeedback()
                onPrevWindow()
            },
            enabled = currentWindow > 1
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.budget_graph_nav_prev)
            )
        }
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag(BUDGET_GRAPH_WINDOW_LABEL_TAG),
            text = stringResource(
                R.string.budget_graph_page_indicator,
                currentWindow,
                totalWindows
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(
            modifier = Modifier
                .size(32.dp)
                .testTag(BUDGET_GRAPH_NEXT_PAGE_TAG),
            onClick = {
                view.confirmFeedback()
                onNextWindow()
            },
            enabled = currentWindow < totalWindows
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.budget_graph_nav_next)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BudgetGraphNavigationPreview() {
    MinusTheme {
        BudgetGraphNavigation(
            currentWindow = 2,
            totalWindows = 5,
            onPrevWindow = {},
            onNextWindow = {},
        )
    }
}
