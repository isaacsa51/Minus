package com.serranoie.app.minus.presentation.ui.theme.component.budget.formula

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.blockWindow
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.toDays
import com.serranoie.app.minus.presentation.ui.theme.component.budget.pill.BudgetMetrics
import com.serranoie.app.minus.presentation.ui.theme.component.budget.pill.calculateBudgetMetrics
import java.math.BigDecimal
import java.math.RoundingMode

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
    NEXT_BLOCK,
    RESERVED,
    CARRIED,
    SPREAD_LEFTOVER,
}

internal sealed interface FormulaTerm {
    data class Amount(val value: BigDecimal) : FormulaTerm
    data class Count(val n: Int, val unit: BudgetPeriod) : FormulaTerm
    data class Fraction(val numerator: BigDecimal, val denominator: Count) : FormulaTerm
    data class Op(val symbol: String) : FormulaTerm
    data class Charge(val transaction: Transaction) : FormulaTerm
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
        BudgetSplitMode.STATIC, BudgetSplitMode.CARRY_OVER, BudgetSplitMode.ASK_ME -> {
            val surplusOnFirstDay = settings?.rollOverCarryForward == true
            if (surplus.signum() == 1) {
                if (surplusOnFirstDay) {
                    add(
                        FormulaRow(
                            FormulaCaption.SURPLUS_FIRST_DAY,
                            amountOp(total, "−", surplus),
                            base
                        )
                    )
                } else {
                    add(
                        FormulaRow(
                            FormulaCaption.SURPLUS_SPLIT,
                            amountOp(base, "+", surplus),
                            total
                        )
                    )
                }
            }
            val splitBase = if (surplusOnFirstDay) base else total
            val days = FormulaTerm.Count(state.periodTotalDays, BudgetPeriod.DAILY)
            val perDay = if (request.splitMode == BudgetSplitMode.ASK_ME && state.periodTotalDays > 0) {
                splitBase.divide(BigDecimal(state.periodTotalDays), 2, RoundingMode.HALF_UP)
            } else {
                state.dailyBudget
            }
            add(
                FormulaRow(
                    FormulaCaption.PER_DAY,
                    listOf(FormulaTerm.Fraction(splitBase, days)),
                    perDay
                )
            )
            val spread = state.dailyBudget.subtract(perDay)
            if (spread.signum() != 0) {
                val terms = listOf(FormulaTerm.Amount(perDay)) + signed(spread)
                add(FormulaRow(FormulaCaption.SPREAD_LEFTOVER, terms, state.dailyBudget))
            }
            if (request.splitMode.carriesLeftover) {
                addCarryOverRows(state, period, metrics)
                return@buildList
            }
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
                add(
                    FormulaRow(
                        FormulaCaption.ADJUSTMENTS,
                        amountOp(total, op, adjustments.abs()),
                        state.totalBudget
                    )
                )
            }
            val window = blockWindow(state.periodTotalDays, state.daysRemaining, period.toDays())
            val blockEnd = settings?.getPeriodEndDate()?.minusDays(window.daysAfter.toLong())
            val reserved = state.reservedCharges.filter { charge ->
                period == BudgetPeriod.DAILY || (blockEnd != null && charge.date?.toLocalDate()
                    ?.isAfter(blockEnd) == true)
            }
            val reservedTotal = reserved.sumOf { it.amount }
            if (reserved.isNotEmpty()) {
                val terms = reserved.flatMapIndexed { index, charge ->
                    listOfNotNull(
                        FormulaTerm.Op("+").takeIf { index > 0 },
                        FormulaTerm.Charge(charge)
                    )
                }
                add(FormulaRow(FormulaCaption.RESERVED, terms, reservedTotal))
            }
            val spentBeforeBlock = metrics.spentInPeriod.subtract(metrics.periodSpent)
            val pool = state.totalBudget.subtract(spentBeforeBlock)
            val remainingTerms = if (reserved.isEmpty()) {
                amountOp(state.totalBudget, "−", spentBeforeBlock)
            } else {
                amountOp(state.totalBudget, "−", spentBeforeBlock.subtract(reservedTotal)) +
                        listOf(FormulaTerm.Op("−"), FormulaTerm.Amount(reservedTotal))
            }
            add(FormulaRow(FormulaCaption.REMAINING_BUDGET, remainingTerms, pool))
            val terms = buildList {
                add(
                    FormulaTerm.Fraction(
                        pool,
                        FormulaTerm.Count(window.daysFromStart, BudgetPeriod.DAILY)
                    )
                )
                if (window.daysInBlock > 1) {
                    add(FormulaTerm.Op("×"))
                    add(FormulaTerm.Count(window.daysInBlock, BudgetPeriod.DAILY))
                }
            }
            add(FormulaRow(FormulaCaption.SPREAD_OVER_LEFT, terms, metrics.periodBudget))
        }
    }

    val next = metrics.nextPeriodAllocation
    if (next == null) {
        add(
            FormulaRow(
                FormulaCaption.LEFT,
                amountOp(metrics.periodBudget, "−", metrics.periodSpent),
                metrics.periodBudget.subtract(metrics.periodSpent),
            )
        )
        return@buildList
    }

    val window = blockWindow(state.periodTotalDays, state.daysRemaining, period.toDays())
    val nextDays = minOf(period.toDays(), window.daysAfter)
    val remaining = state.totalBudget.subtract(metrics.spentInPeriod)
    add(
        FormulaRow(
            FormulaCaption.REMAINING_BUDGET,
            amountOp(state.totalBudget, "−", metrics.spentInPeriod),
            remaining
        )
    )
    val terms = buildList {
        add(
            FormulaTerm.Fraction(
                remaining,
                FormulaTerm.Count(window.daysAfter, BudgetPeriod.DAILY)
            )
        )
        if (nextDays > 1) {
            add(FormulaTerm.Op("×"))
            add(FormulaTerm.Count(nextDays, BudgetPeriod.DAILY))
        }
    }
    add(FormulaRow(FormulaCaption.NEXT_BLOCK, terms, next))
}

private fun MutableList<FormulaRow>.addCarryOverRows(
    state: BudgetState,
    period: BudgetPeriod,
    metrics: BudgetMetrics,
) {
    val days = blockWindow(state.periodTotalDays, state.daysRemaining, period.toDays()).daysInBlock
    val allowance = state.dailyBudget.multiply(BigDecimal(days))
    if (period != BudgetPeriod.DAILY) {
        val terms = listOf(
            FormulaTerm.Amount(state.dailyBudget),
            FormulaTerm.Op("×"),
            FormulaTerm.Count(days, BudgetPeriod.DAILY),
        )
        add(FormulaRow(FormulaCaption.PER_PERIOD, terms, allowance))
    }
    val carried = metrics.periodBudget.subtract(allowance)
    val budgetTerms = if (carried.signum() != 0 && metrics.periodBudget.signum() >= 0) {
        add(FormulaRow(FormulaCaption.CARRIED, listOf(FormulaTerm.Amount(allowance)) + signed(carried), metrics.periodBudget))
        listOf(FormulaTerm.Amount(metrics.periodBudget))
    } else {
        listOf(FormulaTerm.Amount(allowance)) + signed(carried)
    }
    val next = metrics.nextPeriodAllocation
    if (next == null) {
        val terms = budgetTerms + listOf(FormulaTerm.Op("−"), FormulaTerm.Amount(metrics.periodSpent))
        add(FormulaRow(FormulaCaption.LEFT, terms, metrics.periodRemaining))
    } else {
        val terms = budgetTerms + signed(next.subtract(metrics.periodRemaining)) + signed(metrics.periodSpent.negate())
        add(FormulaRow(FormulaCaption.NEXT_BLOCK, terms, next))
    }
}

private fun signed(value: BigDecimal): List<FormulaTerm> = when (value.signum()) {
    0 -> emptyList()
    1 -> listOf(FormulaTerm.Op("+"), FormulaTerm.Amount(value))
    else -> listOf(FormulaTerm.Op("−"), FormulaTerm.Amount(value.abs()))
}

private fun amountOp(left: BigDecimal, op: String, right: BigDecimal): List<FormulaTerm> =
    listOf(FormulaTerm.Amount(left), FormulaTerm.Op(op), FormulaTerm.Amount(right))
