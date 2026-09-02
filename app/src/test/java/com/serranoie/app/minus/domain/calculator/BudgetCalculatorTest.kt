package com.serranoie.app.minus.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.computeDynamicAllocations
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.computeNextBlockAllocations
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BudgetCalculatorTest {

    private val calculator = BudgetCalculator()

    private val start = LocalDate.of(2026, 1, 1)
    private val end = LocalDate.of(2026, 1, 30) // 30 days inclusive
    private val midPeriod = LocalDate.of(2026, 1, 15)

    private fun settings(
        totalBudget: String = "1000.00",
        startDate: LocalDate = start,
        endDate: LocalDate? = end,
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
    ) = BudgetSettings(
        totalBudget = BigDecimal(totalBudget),
        period = period,
        startDate = startDate,
        endDate = endDate,
        splitMode = splitMode,
    )

    private fun txn(
        amount: String,
        date: LocalDate? = LocalDate.of(2026, 1, 10),
        deleted: Boolean = false,
    ) = Transaction.create(amount = BigDecimal(amount), date = date?.atStartOfDay())
        .copy(isDeleted = deleted)

    @Test
    fun `static daily budget is the total spread over every day of the explicit period`() {
        val state = calculator.calculate(settings(), emptyList(), midPeriod)

        assertThat(state.dailyBudget).isEqualTo(BigDecimal("33.33"))
    }

    @Test
    fun `period end falls back to one week after the start when no end date is set and period is WEEKLY`() {
        val state = calculator.calculate(
            settings(endDate = null, period = BudgetPeriod.WEEKLY),
            emptyList(),
            start,
        )

        assertThat(state.dailyBudget).isEqualTo(BigDecimal("125.00"))
    }

    @Test
    fun `days remaining is clamped to zero once the period has ended`() {
        val state = calculator.calculate(settings(), emptyList(), LocalDate.of(2026, 2, 10))

        assertThat(state.daysRemaining).isEqualTo(0)
    }

    @Test
    fun `days remaining is one on the final day of the period`() {
        val state = calculator.calculate(settings(), emptyList(), end)

        assertThat(state.daysRemaining).isEqualTo(1)
    }

    @Test
    fun `days remaining counts the current day and the end day inclusively`() {
        val state = calculator.calculate(settings(), emptyList(), midPeriod)

        assertThat(state.daysRemaining).isEqualTo(16)
    }

    @Test
    fun `total period spend sums every non-deleted transaction passed in, regardless of its date`() {
        val state = calculator.calculate(
            settings(),
            listOf(
                txn("120.00", date = LocalDate.of(2026, 1, 10)),
                txn("80.00", date = LocalDate.of(2026, 3, 15)), // outside the period window
            ),
            midPeriod,
        )

        assertThat(state.totalSpentInPeriod).isEqualTo(BigDecimal("200.00"))
    }

    @Test
    fun `deleted transactions are excluded from period spend`() {
        val state = calculator.calculate(
            settings(),
            listOf(txn("100.00"), txn("999.00", deleted = true)),
            midPeriod,
        )

        assertThat(state.totalSpentInPeriod).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `spent today only counts transactions dated on the current day`() {
        val state = calculator.calculate(
            settings(),
            listOf(
                txn("25.00", date = midPeriod),
                txn("10.00", date = midPeriod),
                txn("500.00", date = midPeriod.minusDays(1)),
            ),
            midPeriod,
        )

        assertThat(state.totalSpentToday).isEqualTo(BigDecimal("35.00"))
    }

    @Test
    fun `transactions with no date never count toward spent today`() {
        val state = calculator.calculate(
            settings(),
            listOf(txn("40.00", date = null)),
            midPeriod,
        )

        assertThat(state.totalSpentToday).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `deleted transactions dated today are excluded from spent today`() {
        val state = calculator.calculate(
            settings(),
            listOf(txn("40.00", date = midPeriod, deleted = true)),
            midPeriod,
        )

        assertThat(state.totalSpentToday).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `is over budget is false when spend equals the budget exactly`() {
        val state = calculator.calculate(settings(), listOf(txn("1000.00")), midPeriod)

        assertThat(state.isOverBudget).isFalse()
    }

    @Test
    fun `is over budget is true once spend exceeds the budget`() {
        val state = calculator.calculate(settings(), listOf(txn("1000.01")), midPeriod)

        assertThat(state.isOverBudget).isTrue()
    }

    @Test
    fun `remaining today goes negative when today's spend exceeds the daily budget`() {
        val state = calculator.calculate(settings(), listOf(txn("50.00", date = midPeriod)), midPeriod)

        assertThat(state.remainingToday).isEqualTo(BigDecimal("33.33").subtract(BigDecimal("50.00")))
    }

    @Test
    fun `progress is the clamped spent-to-budget ratio`() {
        val state = calculator.calculate(settings(), listOf(txn("250.00")), midPeriod)

        assertThat(state.progress).isEqualTo(0.25f)
    }

    @Test
    fun `progress is zero when the total budget is zero`() {
        val state = calculator.calculate(
            settings(totalBudget = "0.00"),
            listOf(txn("50.00")),
            midPeriod,
        )

        assertThat(state.progress).isEqualTo(0f)
    }

    @Test
    fun `progress is capped at one when overspent`() {
        val state = calculator.calculate(settings(), listOf(txn("5000.00")), midPeriod)

        assertThat(state.progress).isEqualTo(1f)
    }

    @Test
    fun `dynamic daily budget is what is left divided by the days that remain`() {
        val state = calculator.calculate(
            settings(splitMode = BudgetSplitMode.DYNAMIC),
            listOf(txn("400.00")),
            LocalDate.of(2026, 1, 25),
        )

        assertThat(state.dailyBudget).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `dynamic daily budget rounds half up to two places`() {
        val state = calculator.calculate(
            settings(splitMode = BudgetSplitMode.DYNAMIC),
            emptyList(),
            LocalDate.of(2026, 1, 24), // 24..30 inclusive = 7 days
        )

        assertThat(state.dailyBudget).isEqualTo(BigDecimal("142.86"))
    }

    @Test
    fun `dynamic daily budget is zero when the budget is already fully spent`() {
        val state = calculator.calculate(
            settings(splitMode = BudgetSplitMode.DYNAMIC),
            listOf(txn("1000.00")),
            midPeriod,
        )

        assertThat(state.dailyBudget).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `dynamic daily budget is zero once the period has ended`() {
        val state = calculator.calculate(
            settings(splitMode = BudgetSplitMode.DYNAMIC),
            listOf(txn("400.00")),
            LocalDate.of(2026, 2, 10),
        )

        assertThat(state.dailyBudget).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `on the last day the dynamic daily budget is the entire remaining balance`() {
        val state = calculator.calculate(
            settings(splitMode = BudgetSplitMode.DYNAMIC),
            listOf(txn("400.00")),
            end, // raw days remaining = 1
        )

        assertThat(state.dailyBudget).isEqualTo(BigDecimal("600.00"))
    }

    @Test
    fun `static allocations never flag today as over the daily allocation`() {
        val state = calculator.calculate(
            settings(splitMode = BudgetSplitMode.STATIC),
            listOf(txn("900.00", date = midPeriod)),
            midPeriod,
        )

        assertThat(state.isTodayOverDailyAllocation).isFalse()
    }

    @Test
    fun `dynamic daily allocation matches the shared dynamic-allocation helper`() {
        val transactions = listOf(
            txn("300.00", date = LocalDate.of(2026, 1, 10)),
            txn("60.00", date = midPeriod),
        )
        val state = calculator.calculate(
            settings(splitMode = BudgetSplitMode.DYNAMIC),
            transactions,
            midPeriod,
        )

        val expected = computeDynamicAllocations(
            totalBudget = BigDecimal("1000.00"),
            totalSpentInPeriod = BigDecimal("360.00"),
            totalSpentToday = BigDecimal("60.00"),
            daysRemaining = 16,
        )
        assertThat(state.dailyAllocation).isEqualTo(expected.dailyAllocation)
        assertThat(state.isTodayOverDailyAllocation).isEqualTo(expected.isTodayOverDailyAllocation)
    }

    @Test
    fun `next-period allocations match the shared next-block helper`() {
        val state = calculator.calculate(
            settings(splitMode = BudgetSplitMode.STATIC),
            listOf(txn("300.00", date = midPeriod)),
            midPeriod,
        )

        val expected = computeNextBlockAllocations(
            totalBudget = BigDecimal("1000.00"),
            totalSpentInPeriod = BigDecimal("300.00"),
            totalDays = 30,
            daysRemaining = 16,
        )
        assertThat(state.nextDailyAllocation).isEqualTo(expected.dailyAllocation)
        assertThat(state.nextWeeklyAllocation).isEqualTo(expected.weeklyAllocation)
        assertThat(state.nextBiweeklyAllocation).isEqualTo(expected.biweeklyAllocation)
        assertThat(state.nextMonthlyAllocation).isEqualTo(expected.monthlyAllocation)
    }
}
