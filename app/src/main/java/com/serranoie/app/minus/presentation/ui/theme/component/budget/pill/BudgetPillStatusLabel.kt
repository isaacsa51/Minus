package com.serranoie.app.minus.presentation.ui.theme.component.budget.pill

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.isRoundedFontEnabled
import com.serranoie.app.minus.presentation.ui.onboarding.periodLabel
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.googleSansFlex
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.censor

/**
 * The status text on the left of the pill ("Today", "This week", "Daily Amount Exceeded",
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
    wrapContent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val textColor = LocalContentColor.current
    val hasProjection = projectionAmount != null
    val secondaryVisible = hasProjection || exhaustedMessage != null

    val context = LocalContext.current
    val isRounded = remember(context) { context.isRoundedFontEnabled }
    val secondaryStyle = remember(isRounded) {
        TextStyle(
            fontFamily = googleSansFlex(weight = 600, width = 125f, isRounded = isRounded),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp,
        )
    }

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
                modifier = if (wrapContent) Modifier
                    .wrapContentWidth()
                else Modifier.weight(1f),
                textAlign = if (centreContent) TextAlign.Center else TextAlign.Start,
                fillWidth = !wrapContent,
            )
        }

        AnimatedVisibility(
            visible = secondaryVisible && !bigVariant, enter = slideInVertically(
                initialOffsetY = { -it }, animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        ) {
            if (hasProjection) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.basicMarquee(),
                ) {
                    Text(
                        text = "${projectionLabel.orEmpty()} ",
                        style = secondaryStyle,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                    SegmentedAmountText(
                        text = projectionAmount,
                        style = secondaryStyle,
                        color = textColor,
                        minFontSize = 12.sp,
                        currencySymbol = currencySymbol,
                        symbolAtEnd = symbolAtEnd,
                        modifier = Modifier.censor(),
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

@PreviewLightDark
@Composable
private fun StatusLabelPreview() {
    MinusTheme {
        Surface {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusLabel(
                    budgetState = BudgetState.EMPTY,
                    budgetPeriod = BudgetPeriod.DAILY,
                    isOverBudget = false,
                )
                StatusLabel(
                    budgetState = BudgetState.EMPTY,
                    budgetPeriod = BudgetPeriod.DAILY,
                    isOverBudget = false,
                    projectionLabel = "For tomorrow",
                    projectionAmount = "25.00",
                    currencySymbol = "$",
                )
                StatusLabel(
                    budgetState = BudgetState.EMPTY,
                    budgetPeriod = BudgetPeriod.WEEKLY,
                    isOverBudget = false,
                    isOverSubPeriodAllocation = true,
                )
                StatusLabel(
                    budgetState = BudgetState.EMPTY,
                    budgetPeriod = BudgetPeriod.DAILY,
                    isOverBudget = true,
                    exhaustedMessage = "Budget exhausted",
                )
                StatusLabel(
                    budgetState = BudgetState.EMPTY,
                    budgetPeriod = BudgetPeriod.DAILY,
                    isOverBudget = false,
                    bigVariant = true,
                )
            }
        }
    }
}

