package com.serranoie.app.minus.presentation.ui.theme.component.budget.formula

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.blocksRemaining
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.toDays
import com.serranoie.app.minus.presentation.ui.theme.component.budget.pill.calculateBudgetMetrics
import java.math.BigDecimal

data class BudgetFormulaRequest(
    val budgetState: BudgetState,
    val budgetSettings: BudgetSettings?,
    val viewPeriod: BudgetPeriod,
    val splitMode: BudgetSplitMode,
    val currencyCode: String,
    val draftAmount: BigDecimal = BigDecimal.ZERO,
    val tip: String? = null,
)

internal enum class FormulaCaption {
    SURPLUS_SPLIT,
    SURPLUS_FIRST_DAY,
    ADJUSTMENTS,
    PER_DAY,
    PER_PERIOD,
    REMAINING_BUDGET,
    SPREAD_OVER_LEFT,
    LEFT,
}

internal sealed interface FormulaTerm {
    data class Amount(val value: BigDecimal) : FormulaTerm
    data class Count(val n: Int, val unit: BudgetPeriod) : FormulaTerm
    data class Fraction(val numerator: BigDecimal, val denominator: Count) : FormulaTerm
    data class Op(val symbol: String) : FormulaTerm
}

internal data class FormulaRow(
    val caption: FormulaCaption,
    val terms: List<FormulaTerm>,
    val result: BigDecimal,
)

internal fun buildBudgetFormula(request: BudgetFormulaRequest): List<FormulaRow> = buildList {
    val state = request.budgetState
    val settings = request.budgetSettings
    val period = request.viewPeriod
    val metrics = calculateBudgetMetrics(state, period, request.splitMode, request.draftAmount)

    val surplus = settings?.rollOverLimit?.takeIf { it.signum() == 1 } ?: BigDecimal.ZERO
    val total = settings?.totalBudget ?: state.totalBudget
    val base = total.subtract(surplus)

    when (request.splitMode) {
        BudgetSplitMode.STATIC -> {
            val surplusOnFirstDay = settings?.rollOverCarryForward == true
            if (surplus.signum() == 1) {
                if (surplusOnFirstDay) {
                    add(FormulaRow(FormulaCaption.SURPLUS_FIRST_DAY, amountOp(total, "−", surplus), base))
                } else {
                    add(FormulaRow(FormulaCaption.SURPLUS_SPLIT, amountOp(base, "+", surplus), total))
                }
            }
            val splitBase = if (surplusOnFirstDay) base else total
            val days = FormulaTerm.Count(state.periodTotalDays, BudgetPeriod.DAILY)
            add(FormulaRow(FormulaCaption.PER_DAY, listOf(FormulaTerm.Fraction(splitBase, days)), state.dailyBudget))
            if (period != BudgetPeriod.DAILY) {
                val terms = listOf(
                    FormulaTerm.Amount(state.dailyBudget),
                    FormulaTerm.Op("×"),
                    FormulaTerm.Count(period.toDays(), BudgetPeriod.DAILY),
                )
                add(FormulaRow(FormulaCaption.PER_PERIOD, terms, metrics.periodBudget))
            }
        }

        BudgetSplitMode.DYNAMIC -> {
            if (surplus.signum() == 1) {
                add(FormulaRow(FormulaCaption.SURPLUS_SPLIT, amountOp(base, "+", surplus), total))
            }
            val adjustments = state.totalBudget.subtract(total)
            if (adjustments.signum() != 0) {
                val op = if (adjustments.signum() == 1) "+" else "−"
                add(FormulaRow(FormulaCaption.ADJUSTMENTS, amountOp(total, op, adjustments.abs()), state.totalBudget))
            }
            val remaining = state.totalBudget.subtract(metrics.spentInPeriod)
            add(FormulaRow(FormulaCaption.REMAINING_BUDGET, amountOp(state.totalBudget, "−", metrics.spentInPeriod), remaining))
            val blocks = FormulaTerm.Count(blocksRemaining(state.daysRemaining, period.toDays()), period)
            add(FormulaRow(FormulaCaption.SPREAD_OVER_LEFT, listOf(FormulaTerm.Fraction(remaining, blocks)), metrics.periodBudget))
        }
    }

    add(
        FormulaRow(
            FormulaCaption.LEFT,
            amountOp(metrics.periodBudget, "−", metrics.periodSpent),
            metrics.periodBudget.subtract(metrics.periodSpent),
        )
    )
}

private fun amountOp(left: BigDecimal, op: String, right: BigDecimal): List<FormulaTerm> =
    listOf(FormulaTerm.Amount(left), FormulaTerm.Op(op), FormulaTerm.Amount(right))
