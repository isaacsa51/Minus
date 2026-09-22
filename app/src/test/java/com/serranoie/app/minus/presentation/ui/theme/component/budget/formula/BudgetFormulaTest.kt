package com.serranoie.app.minus.presentation.ui.theme.component.budget.formula

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
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

    @Test
    fun `daily over its allocation ends with tomorrow's projection instead of a negative amount`() {
        val over = state.copy(
            totalSpentToday = BigDecimal("100.00"),
            totalSpentInPeriod = BigDecimal("400.00"),
        )
        val metrics = calculateBudgetMetrics(over, BudgetPeriod.DAILY, BudgetSplitMode.DYNAMIC)
        val rows = buildBudgetFormula(request(BudgetSplitMode.DYNAMIC).copy(budgetState = over, viewPeriod = BudgetPeriod.DAILY))

        assertThat(rows.last().caption).isEqualTo(FormulaCaption.NEXT_BLOCK)
        assertThat(rows.none { it.caption == FormulaCaption.LEFT }).isTrue()
        assertThat(rows.last().result.compareTo(metrics.nextPeriodAllocation)).isEqualTo(0)
        assertThat(evaluate(rows.last()).compareTo(rows.last().result)).isEqualTo(0)
    }

    @Test
    fun `static over its block keeps the plain left row and never projects`() {
        val over = state.copy(totalSpentToday = BigDecimal("100.00"))
        val metrics = calculateBudgetMetrics(over, BudgetPeriod.DAILY, BudgetSplitMode.STATIC)
        val rows = buildBudgetFormula(request(BudgetSplitMode.STATIC).copy(budgetState = over, viewPeriod = BudgetPeriod.DAILY))

        assertThat(metrics.isOverCurrentSubPeriod).isTrue()
        assertThat(metrics.nextPeriodAllocation).isNull()
        assertThat(rows.last().caption).isEqualTo(FormulaCaption.LEFT)
        assertThat(rows.last().result.compareTo(metrics.periodRemaining)).isEqualTo(0)
    }

    @Test
    fun `reserved charges get their own labelled row and are split out of the remaining budget`() {
        val netflix = Transaction(amount = BigDecimal("15.00"), comment = "Netflix", date = LocalDate.of(2026, 9, 25).atStartOfDay())
        val gym = Transaction(amount = BigDecimal("30.00"), comment = "Gym", date = LocalDate.of(2026, 9, 14).atStartOfDay())
        val reserved = state.copy(
            totalSpentInPeriod = BigDecimal("445.00"),
            reservedCharges = listOf(gym, netflix),
        )
        val metrics = calculateBudgetMetrics(reserved, BudgetPeriod.WEEKLY, BudgetSplitMode.DYNAMIC)
        val rows = buildBudgetFormula(request(BudgetSplitMode.DYNAMIC).copy(budgetState = reserved))

        val reservedRow = rows.single { it.caption == FormulaCaption.RESERVED }
        assertThat(reservedRow.terms.filterIsInstance<FormulaTerm.Charge>().map { it.transaction.comment })
            .containsExactly("Netflix")
        assertThat(reservedRow.result).isEqualTo(BigDecimal("15.00"))
        val remainingRow = rows.first { it.caption == FormulaCaption.REMAINING_BUDGET }
        assertThat(remainingRow.terms).hasSize(5)
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
            val times = (row.terms.getOrNull(2) as? FormulaTerm.Count)?.n ?: 1
            return first.numerator.multiply(BigDecimal(times))
                .divide(BigDecimal(first.denominator.n), 2, RoundingMode.HALF_UP)
        }
        fun value(term: FormulaTerm): BigDecimal = when (term) {
            is FormulaTerm.Amount -> term.value
            is FormulaTerm.Count -> BigDecimal(term.n)
            is FormulaTerm.Charge -> term.transaction.amount
            else -> error("unexpected $term")
        }
        return row.terms.drop(1).chunked(2).fold(value(first)) { acc, (op, term) ->
            when ((op as FormulaTerm.Op).symbol) {
                "+" -> acc.add(value(term))
                "−" -> acc.subtract(value(term))
                "×" -> acc.multiply(value(term))
                else -> error("unexpected $op")
            }
        }
    }
}
