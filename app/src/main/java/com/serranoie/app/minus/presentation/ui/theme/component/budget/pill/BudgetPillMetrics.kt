package com.serranoie.app.minus.presentation.ui.theme.component.budget.pill

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.carryOverNext
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.carryOverRemaining
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.dynamicAllocations
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.nextAllocationFor
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.spentIn
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.toDays
import java.math.BigDecimal
import java.math.RoundingMode

internal data class BudgetMetrics(
    val periodRemaining: BigDecimal,
    val spendProgress: Float,
    val isCurrentPeriodOverBudget: Boolean,
    val isOverCurrentSubPeriod: Boolean,
    val nextPeriodAllocation: BigDecimal? = null,
    val periodBudget: BigDecimal = BigDecimal.ZERO,
    val periodSpent: BigDecimal = BigDecimal.ZERO,
    val spentInPeriod: BigDecimal = BigDecimal.ZERO,
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

    val periodSpent = state.spentIn(period).add(draftSpend)
    val spentInPeriod = state.totalSpentInPeriod.add(draftSpend)

    val staticRemaining = periodBudget.subtract(periodSpent)

    val dynamic = if (splitMode == BudgetSplitMode.DYNAMIC) state.dynamicAllocations(draftSpend) else null
    val dynamicAllocation = dynamic?.forPeriod(period) ?: BigDecimal.ZERO

    val periodRemaining = when (splitMode) {
        BudgetSplitMode.DYNAMIC ->
            dynamicAllocation.subtract(periodSpent).coerceAtLeast(BigDecimal.ZERO)

        BudgetSplitMode.STATIC -> staticRemaining

        BudgetSplitMode.CARRY_OVER -> state.carryOverRemaining(period, draftSpend)
    }
    val blockBudget = when (splitMode) {
        BudgetSplitMode.STATIC -> periodBudget
        BudgetSplitMode.DYNAMIC -> dynamicAllocation
        BudgetSplitMode.CARRY_OVER -> periodRemaining.add(periodSpent)
    }

    val isOverBudget = state.isOverBudget || (hasDraft && spentInPeriod > state.totalBudget)

    val isOverSubPeriod = when (splitMode) {
        BudgetSplitMode.DYNAMIC -> when (period) {
            BudgetPeriod.DAILY -> dynamic?.isTodayOverDailyAllocation == true
            else -> dynamicAllocation.signum() == 1 && periodSpent > dynamicAllocation
        }

        BudgetSplitMode.STATIC, BudgetSplitMode.CARRY_OVER -> periodRemaining.signum() == -1
    }

    val progressBudget = if (splitMode == BudgetSplitMode.CARRY_OVER) blockBudget else periodBudget
    val progress = if (isOverBudget || isOverSubPeriod) {
        1f
    } else if (progressBudget.signum() == 1) {
        periodSpent.divide(progressBudget, 2, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
    } else 0f

    val nextPeriodAllocation = when (splitMode) {
        BudgetSplitMode.STATIC -> null
        BudgetSplitMode.DYNAMIC -> state.nextAllocationFor(period, draftSpend)
        BudgetSplitMode.CARRY_OVER -> state.carryOverNext(period, draftSpend)
    }?.takeIf { isOverSubPeriod && !isOverBudget && it.signum() == 1 }

    return BudgetMetrics(
        periodRemaining = periodRemaining,
        spendProgress = progress,
        isCurrentPeriodOverBudget = isOverBudget,
        isOverCurrentSubPeriod = isOverSubPeriod,
        nextPeriodAllocation = nextPeriodAllocation,
        periodBudget = blockBudget,
        periodSpent = periodSpent,
        spentInPeriod = spentInPeriod,
    )
}

@Composable
internal fun resolveExhaustedMessage(
    state: BudgetState?, period: BudgetPeriod, splitMode: BudgetSplitMode
): String? {
    if (state == null || state.isOverBudget) return null

    fun isExhausted(p: BudgetPeriod): Boolean {
        val remaining = if (splitMode == BudgetSplitMode.CARRY_OVER) {
            state.carryOverRemaining(p)
        } else {
            state.dailyBudget.multiply(BigDecimal(p.toDays())).subtract(state.spentIn(p))
        }
        return remaining.signum() <= 0
    }

    return when (splitMode) {
        BudgetSplitMode.STATIC, BudgetSplitMode.CARRY_OVER -> {
            if (period == BudgetPeriod.DAILY || isExhausted(period)) return null

            val labels = buildList {
                if (isExhausted(BudgetPeriod.DAILY)) add(stringResource(R.string.budget_pill_exhausted_daily_label))
                if (period >= BudgetPeriod.BIWEEKLY && isExhausted(BudgetPeriod.WEEKLY)) add(stringResource(R.string.budget_pill_exhausted_weekly_label))
                if (period == BudgetPeriod.MONTHLY && isExhausted(BudgetPeriod.BIWEEKLY)) add(stringResource(R.string.budget_pill_exhausted_biweekly_label))
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
            val a = state.dynamicAllocations()
            val isOverDaily = a.isTodayOverDailyAllocation
            val isOverWeekly =
                state.totalSpentThisWeek > a.weeklyAllocation && a.weeklyAllocation > BigDecimal.ZERO
            val isOverBiweekly =
                state.totalSpentThisBiweek > a.biweeklyAllocation && a.biweeklyAllocation > BigDecimal.ZERO
            val isOverMonthly =
                state.totalSpentThisMonth > a.monthlyAllocation && a.monthlyAllocation > BigDecimal.ZERO

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
