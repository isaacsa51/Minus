package com.serranoie.app.minus.presentation.ui.theme.component.budget.formula

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.theme.component.budget.pill.calculateBudgetMetrics
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class BudgetFormulaTest {

    private val settings = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = LocalDate.of(2026, 9, 1),
        rollOverLimit = BigDecimal("100.00"),
        rollOverCarryForward = false,
    )

    private val state = BudgetState(
        remainingToday = BigDecimal.ZERO,
        totalSpentToday = BigDecimal("20.00"),
        dailyBudget = BigDecimal("33.33"),
        daysRemaining = 20,
        progress = 0f,
        isOverBudget = false,
        totalBudget = BigDecimal("1050.00"),
        totalSpentInPeriod = BigDecimal("400.00"),
        totalSpentThisWeek = BigDecimal("150.00"),
        weeklyAllocation = BigDecimal("216.67"),
        periodTotalDays = 30,
    )

    @Test
    fun `static weekly chain evaluates step by step down to the pill amount`() {
        val metrics = calculateBudgetMetrics(state, BudgetPeriod.WEEKLY, BudgetSplitMode.STATIC)

        val rows = buildBudgetFormula(request(BudgetSplitMode.STATIC))

        assertThat(rows.map { it.caption }).containsExactly(
            FormulaCaption.SURPLUS_SPLIT,
            FormulaCaption.PER_DAY,
            FormulaCaption.PER_PERIOD,
            FormulaCaption.LEFT,
        ).inOrder()
        rows.forEach { assertThat(evaluate(it).compareTo(it.result)).isEqualTo(0) }
        assertThat(rows.last().result.compareTo(metrics.periodRemaining)).isEqualTo(0)
    }

    @Test
    fun `dynamic weekly chain includes surplus and adjustments and ends at the allocation minus spent`() {
        val metrics = calculateBudgetMetrics(state, BudgetPeriod.WEEKLY, BudgetSplitMode.DYNAMIC)

        val rows = buildBudgetFormula(request(BudgetSplitMode.DYNAMIC))

        assertThat(rows.map { it.caption }).containsExactly(
            FormulaCaption.SURPLUS_SPLIT,
            FormulaCaption.ADJUSTMENTS,
            FormulaCaption.REMAINING_BUDGET,
            FormulaCaption.SPREAD_OVER_LEFT,
            FormulaCaption.LEFT,
        ).inOrder()
        rows.forEach { assertThat(evaluate(it).compareTo(it.result)).isEqualTo(0) }
        assertThat(rows.last().result.compareTo(metrics.periodRemaining)).isEqualTo(0)
    }

    private fun request(splitMode: BudgetSplitMode) = BudgetFormulaRequest(
        budgetState = state,
        budgetSettings = settings,
        viewPeriod = BudgetPeriod.WEEKLY,
        splitMode = splitMode,
        currencyCode = "USD",
    )

    private fun evaluate(row: FormulaRow): BigDecimal {
        val first = row.terms.first()
        if (first is FormulaTerm.Fraction) {
            return first.numerator.divide(BigDecimal(first.denominator.n), 2, RoundingMode.HALF_UP)
        }
        val left = (first as FormulaTerm.Amount).value
        val op = (row.terms[1] as FormulaTerm.Op).symbol
        val right = when (val term = row.terms[2]) {
            is FormulaTerm.Amount -> term.value
            is FormulaTerm.Count -> BigDecimal(term.n)
            else -> error("unexpected $term")
        }
        return when (op) {
            "+" -> left.add(right)
            "−" -> left.subtract(right)
            "×" -> left.multiply(right)
            else -> error("unexpected $op")
        }
    }
}
