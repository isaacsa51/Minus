package com.serranoie.app.minus.presentation.ui.theme.component.budget.pill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.onboarding.periodLabel
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed

/**
 * The status text on the left of the pill ("Today", "Per week", "Daily Amount Exceeded",
 * "Budget amount exceeded", …) plus the small secondary line beneath it: either the red
 * "budget exhausted" note or the "For tomorrow $…" projection.
 */
@Composable
internal fun StatusLabel(
    budgetState: BudgetState?,
    budgetPeriod: BudgetPeriod = BudgetPeriod.DAILY,
    isOverBudget: Boolean,
    isOverSubPeriodAllocation: Boolean = false,
    exhaustedMessage: String? = null,
    projectionLabel: String? = null,
    projectionAmount: String? = null,
    currencySymbol: String = "",
    symbolAtEnd: Boolean = false,
    bigVariant: Boolean = false,
    splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
    wrapContent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val textColor = LocalContentColor.current
    val hasProjection = projectionAmount != null
    val secondaryVisible = hasProjection || exhaustedMessage != null
    val secondaryStyle = MaterialTheme.typography.labelSmallEmphasized.copy(letterSpacing = 0.sp)

    val label = when {
        isOverBudget -> stringResource(R.string.budget_pill_over_budget)
        budgetState == null -> stringResource(R.string.budget_pill_no_budget)
        isOverSubPeriodAllocation -> stringResource(
            when (budgetPeriod) {
                BudgetPeriod.DAILY -> R.string.budget_pill_label_daily_exceeded
                BudgetPeriod.WEEKLY -> R.string.budget_pill_label_weekly_exceeded
                BudgetPeriod.BIWEEKLY -> R.string.budget_pill_label_biweekly_exceeded
                BudgetPeriod.MONTHLY -> R.string.budget_pill_label_monthly_exceeded
            }
        )

        splitMode == BudgetSplitMode.DYNAMIC -> stringResource(
            when (budgetPeriod) {
                BudgetPeriod.DAILY -> R.string.budget_pill_label_per_daily
                BudgetPeriod.WEEKLY -> R.string.budget_pill_label_per_weekly
                BudgetPeriod.BIWEEKLY -> R.string.budget_pill_label_per_biweekly
                BudgetPeriod.MONTHLY -> R.string.budget_pill_label_per_monthly
            }
        )

        else -> budgetPeriod.periodLabel()
    }

    val labelVerticalOffset by animateDpAsState(
        label = "labelVerticalOffset",
        targetValue = if (secondaryVisible && !bigVariant) (-2).dp else 0.dp,
        animationSpec = tween(300),
    )
    val centreContent = bigVariant || isOverBudget || isOverSubPeriodAllocation

    Column(
        modifier = modifier
            .heightIn(min = if (bigVariant) 72.dp else 44.dp)
            .animateContentSize(animationSpec = tween(300)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (centreContent) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .then(if (wrapContent) Modifier.wrapContentWidth() else Modifier.fillMaxWidth())
                .offset { IntOffset(0, labelVerticalOffset.roundToPx()) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdaptiveSingleLineText(
                text = label,
                style = if (bigVariant || isOverBudget || isOverSubPeriodAllocation) {
                    MaterialTheme.typography.titleMediumEmphasized
                } else {
                    MaterialTheme.typography.titleMediumCondensed
                },
                color = textColor,
                minFontSize = if (bigVariant) 14.sp else 12.sp,
                modifier = if (wrapContent) Modifier.wrapContentWidth() else Modifier.weight(1f),
                textAlign = if (bigVariant) TextAlign.Center else TextAlign.Start,
                fillWidth = !wrapContent,
            )
        }

        AnimatedVisibility(
            visible = secondaryVisible && !bigVariant, enter = slideInVertically(
                initialOffsetY = { -it }, animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        ) {
            if (hasProjection) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${projectionLabel.orEmpty()} ",
                        style = secondaryStyle,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SegmentedAmountText(
                        text = projectionAmount,
                        style = secondaryStyle,
                        color = textColor,
                        minFontSize = 12.sp,
                        currencySymbol = currencySymbol,
                        symbolAtEnd = symbolAtEnd,
                        textAlign = TextAlign.Start,
                        fillWidth = false,
                    )
                }
            } else {
                Text(
                    text = exhaustedMessage.orEmpty(),
                    style = secondaryStyle,
                    color = colorBad,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }
        }
    }
}
