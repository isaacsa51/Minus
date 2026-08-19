package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val BUDGET_GRAPH_DAY_PREV_TAG = "BUDGET_GRAPH_DAY_PREV_TAG"
const val BUDGET_GRAPH_DAY_NEXT_TAG = "BUDGET_GRAPH_DAY_NEXT_TAG"
const val BUDGET_GRAPH_DAY_LABEL_TAG = "BUDGET_GRAPH_DAY_LABEL_TAG"

@Composable
internal fun BudgetGraphDayNavigation(
    date: LocalDate,
    dayIndex: Int,
    totalDays: Int,
    dateFormatter: DateTimeFormatter,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = date,
            modifier = Modifier
                .weight(1f)
                .testTag(BUDGET_GRAPH_DAY_LABEL_TAG),
            transitionSpec = {
                val fadeSpec = tween<Float>(180)
                if (targetState.isAfter(initialState)) {
                    slideInHorizontally(animationSpec = tween(220)) { it } + fadeIn(fadeSpec) togetherWith
                        slideOutHorizontally(animationSpec = tween(180)) { -it } + fadeOut(tween(120))
                } else {
                    slideInHorizontally(animationSpec = tween(220)) { -it } + fadeIn(fadeSpec) togetherWith
                        slideOutHorizontally(animationSpec = tween(180)) { it } + fadeOut(tween(120))
                }
            },
            label = "dayLabelSlide",
        ) { animatedDate ->
            Text(
                text = animatedDate.format(dateFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                modifier = Modifier
                    .size(32.dp)
                    .testTag(BUDGET_GRAPH_DAY_PREV_TAG),
                onClick = {
                    view.confirmFeedback()
                    onPrevDay()
                },
                enabled = canGoPrev,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.budget_graph_nav_prev)
                )
            }
            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = stringResource(R.string.budget_graph_page_indicator, dayIndex, totalDays),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                modifier = Modifier
                    .size(32.dp)
                    .testTag(BUDGET_GRAPH_DAY_NEXT_TAG),
                onClick = {
                    view.confirmFeedback()
                    onNextDay()
                },
                enabled = canGoNext,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.budget_graph_nav_next)
                )
            }
        }
    }
}
