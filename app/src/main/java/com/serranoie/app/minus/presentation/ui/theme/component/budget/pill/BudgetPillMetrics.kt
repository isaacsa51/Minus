package com.serranoie.app.minus.presentation.ui.theme.component.budget.pill

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.computeDynamicAllocations
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.computeNextBlockAllocations
import java.math.BigDecimal
import java.math.RoundingMode

internal data class BudgetMetrics(
    val periodRemaining: BigDecimal,
    val spendProgress: Float,
    val isCurrentPeriodOverBudget: Boolean,
    val isOverCurrentSubPeriod: Boolean,
    val nextPeriodAllocation: BigDecimal? = null,
)

internal fun calculateBudgetMetrics(
    state: BudgetState,
    period: BudgetPeriod,
    splitMode: BudgetSplitMode,
    draftSpend: BigDecimal = BigDecimal.ZERO,
): BudgetMetrics {
    val hasDraft = draftSpend.signum() != 0

    val multiplier = when (period) {
        BudgetPeriod.DAILY -> BigDecimal.ONE
        BudgetPeriod.WEEKLY -> BigDecimal(7)
        BudgetPeriod.BIWEEKLY -> BigDecimal(14)
        BudgetPeriod.MONTHLY -> BigDecimal(30)
    }
    val periodBudget = state.dailyBudget.multiply(multiplier)

    val basePeriodSpent = when (period) {
        BudgetPeriod.DAILY -> state.totalSpentToday
        BudgetPeriod.WEEKLY -> state.totalSpentThisWeek
        BudgetPeriod.BIWEEKLY -> state.totalSpentThisBiweek
        BudgetPeriod.MONTHLY -> state.totalSpentThisMonth
    }
    val periodSpent = basePeriodSpent.add(draftSpend)
    val spentInPeriod = state.totalSpentInPeriod.add(draftSpend)
    val spentToday = state.totalSpentToday.add(draftSpend)

    val staticRemaining = periodBudget.subtract(periodSpent)

    val liveDynamic = if (hasDraft && splitMode == BudgetSplitMode.DYNAMIC) {
        computeDynamicAllocations(
            totalBudget = state.totalBudget,
            totalSpentInPeriod = spentInPeriod,
            totalSpentToday = spentToday,
            daysRemaining = state.daysRemaining,
        )
    } else {
        null
    }
    val dynamicAllocation = liveDynamic?.forPeriod(period) ?: state.allocationFor(period)

    val periodRemaining = when (splitMode) {
        BudgetSplitMode.DYNAMIC ->
            dynamicAllocation.subtract(periodSpent).coerceAtLeast(BigDecimal.ZERO)

        BudgetSplitMode.STATIC -> staticRemaining
    }

    val isOverBudget = state.isOverBudget || (hasDraft && spentInPeriod > state.totalBudget)

    val isOverSubPeriod = when (splitMode) {
        BudgetSplitMode.DYNAMIC -> when (period) {
            BudgetPeriod.DAILY ->
                liveDynamic?.let { spentToday > it.dailyAllocation }
                    ?: state.isTodayOverDailyAllocation

            else -> dynamicAllocation.signum() == 1 && spentInPeriod > dynamicAllocation
        }

        BudgetSplitMode.STATIC -> staticRemaining.signum() == -1
    }

    val progress = if (periodBudget.signum() == 1) {
        periodSpent.divide(periodBudget, 2, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else 0f

    val nextAllocation = if (hasDraft) {
        computeNextBlockAllocations(
            totalBudget = state.totalBudget,
            totalSpentInPeriod = spentInPeriod,
            totalDays = state.periodTotalDays,
            daysRemaining = state.daysRemaining,
        ).forPeriod(period)
    } else {
        state.nextAllocationFor(period)
    }
    val nextPeriodAllocation = nextAllocation
        .takeIf { isOverSubPeriod && !isOverBudget && it.signum() == 1 }

    return BudgetMetrics(
        periodRemaining, progress, isOverBudget, isOverSubPeriod, nextPeriodAllocation,
    )
}

@Composable
internal fun resolveExhaustedMessage(
    state: BudgetState?, period: BudgetPeriod, splitMode: BudgetSplitMode
): String? {
    if (state == null) return null

    val dailyRem = state.dailyBudget.subtract(state.totalSpentToday)
    val isDailyExhausted = dailyRem.signum() <= 0
    val isWeeklyExhausted =
        (state.dailyBudget.multiply(BigDecimal(7))).subtract(state.totalSpentInPeriod).signum() <= 0
    val isBiweeklyExhausted =
        (state.dailyBudget.multiply(BigDecimal(14))).subtract(state.totalSpentInPeriod)
            .signum() <= 0

    return when (splitMode) {
        BudgetSplitMode.STATIC -> {
            val staticRem = when (period) {
                BudgetPeriod.WEEKLY -> state.dailyBudget.multiply(BigDecimal(7))
                    .subtract(state.totalSpentInPeriod)

                BudgetPeriod.BIWEEKLY -> state.dailyBudget.multiply(BigDecimal(14))
                    .subtract(state.totalSpentInPeriod)

                BudgetPeriod.MONTHLY -> state.dailyBudget.multiply(BigDecimal(30))
                    .subtract(state.totalSpentInPeriod)

                else -> BigDecimal.ZERO
            }
            if (staticRem.signum() <= 0) return null

            val labels = buildList {
                if (isDailyExhausted) add(stringResource(R.string.budget_pill_exhausted_daily_label))
                if (period >= BudgetPeriod.BIWEEKLY && isWeeklyExhausted) add(stringResource(R.string.budget_pill_exhausted_weekly_label))
                if (period == BudgetPeriod.MONTHLY && isBiweeklyExhausted) add(stringResource(R.string.budget_pill_exhausted_biweekly_label))
            }

            when (labels.size) {
                1 -> stringResource(R.string.budget_pill_exhausted_single, labels[0])
                2 -> stringResource(R.string.budget_pill_exhausted_double, labels[0], labels[1])
                3 -> stringResource(
                    R.string.budget_pill_exhausted_triple, labels[0], labels[1], labels[2]
                )

                else -> null
            }
        }

        BudgetSplitMode.DYNAMIC -> {
            val isOverDaily = state.isTodayOverDailyAllocation
            val isOverWeekly =
                state.totalSpentInPeriod > state.weeklyAllocation && state.weeklyAllocation > BigDecimal.ZERO
            val isOverBiweekly =
                state.totalSpentInPeriod > state.biweeklyAllocation && state.biweeklyAllocation > BigDecimal.ZERO
            val isOverMonthly =
                state.totalSpentInPeriod > state.monthlyAllocation && state.monthlyAllocation > BigDecimal.ZERO

            val currentOver = when (period) {
                BudgetPeriod.DAILY -> isOverDaily
                BudgetPeriod.WEEKLY -> isOverWeekly
                BudgetPeriod.BIWEEKLY -> isOverBiweekly
                BudgetPeriod.MONTHLY -> isOverMonthly
            }
            if (currentOver) return null

            val overspent = buildList {
                if (isOverDaily) add(stringResource(R.string.budget_pill_exhausted_daily_label))
                if (period >= BudgetPeriod.BIWEEKLY && isOverWeekly) add(stringResource(R.string.budget_pill_exhausted_weekly_label))
                if (period == BudgetPeriod.MONTHLY && isOverBiweekly) add(stringResource(R.string.budget_pill_exhausted_biweekly_label))
            }

            when (overspent.size) {
                1 -> stringResource(R.string.budget_pill_sub_exceeded_single, overspent[0])
                2 -> stringResource(
                    R.string.budget_pill_sub_exceeded_double, overspent[0], overspent[1]
                )

                3 -> stringResource(
                    R.string.budget_pill_sub_exceeded_triple,
                    overspent[0],
                    overspent[1],
                    overspent[2]
                )

                else -> null
            }
        }
    }
}
