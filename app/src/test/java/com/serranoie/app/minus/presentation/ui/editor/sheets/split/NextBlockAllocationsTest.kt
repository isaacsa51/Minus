package com.serranoie.app.minus.presentation.ui.editor.sheets.split

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import org.junit.Test
import java.math.BigDecimal

class NextBlockAllocationsTest {

    @Test
    fun `spec example - 120 over 3 days, 50 spent today, next day gets 35`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("120.00"),
            totalSpentInPeriod = BigDecimal("50.00"),
            totalDays = 3,
            daysRemaining = 3,
        )

        assertThat(next.dailyAllocation).isEqualTo(BigDecimal("35.00"))
    }

    @Test
    fun `mid period - only the final day is left after the current one`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("120.00"),
            totalSpentInPeriod = BigDecimal("90.00"),
            totalDays = 3,
            daysRemaining = 2,
        )

        assertThat(next.dailyAllocation).isEqualTo(BigDecimal("30.00"))
    }

    @Test
    fun `last day of the period - no next block, allocation is zero`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("120.00"),
            totalSpentInPeriod = BigDecimal("100.00"),
            totalDays = 3,
            daysRemaining = 1,
        )

        assertThat(next.dailyAllocation).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `weekly - first week of a 21 day period leaves two whole weeks`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("1000.00"),
            totalSpentInPeriod = BigDecimal("100.00"),
            totalDays = 21,
            daysRemaining = 19,
        )

        assertThat(next.weeklyAllocation).isEqualTo(BigDecimal("450.00"))
    }

    @Test
    fun `weekly - second week leaves exactly one whole week`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("1000.00"),
            totalSpentInPeriod = BigDecimal("100.00"),
            totalDays = 21,
            daysRemaining = 12,
        )

        assertThat(next.weeklyAllocation).isEqualTo(BigDecimal("900.00"))
    }

    @Test
    fun `weekly - final week of the period has no next week`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("1000.00"),
            totalSpentInPeriod = BigDecimal("100.00"),
            totalDays = 21,
            daysRemaining = 5,
        )

        assertThat(next.weeklyAllocation).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `biweekly - one whole 14 day block after the current one`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("2000.00"),
            totalSpentInPeriod = BigDecimal("200.00"),
            totalDays = 28,
            daysRemaining = 25,
        )

        assertThat(next.biweeklyAllocation).isEqualTo(BigDecimal("1800.00"))
    }

    @Test
    fun `monthly view on a single month budget has no next month`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("900.00"),
            totalSpentInPeriod = BigDecimal.ZERO,
            totalDays = 30,
            daysRemaining = 30,
        )

        assertThat(next.monthlyAllocation).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `rounding follows HALF_UP to two decimals`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("100.00"),
            totalSpentInPeriod = BigDecimal.ZERO,
            totalDays = 4,
            daysRemaining = 4,
        )

        assertThat(next.dailyAllocation).isEqualTo(BigDecimal("33.33"))
    }

    @Test
    fun `overspent period - every allocation is zero`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("100.00"),
            totalSpentInPeriod = BigDecimal("150.00"),
            totalDays = 10,
            daysRemaining = 6,
        )

        assertThat(next).isEqualTo(NextBlockAllocations.ZERO)
    }

    @Test
    fun `period already over - every allocation is zero`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("100.00"),
            totalSpentInPeriod = BigDecimal("10.00"),
            totalDays = 10,
            daysRemaining = 0,
        )

        assertThat(next).isEqualTo(NextBlockAllocations.ZERO)
    }

    @Test
    fun `forPeriod returns the matching allocation`() {
        val next = computeNextBlockAllocations(
            totalBudget = BigDecimal("1000.00"),
            totalSpentInPeriod = BigDecimal("100.00"),
            totalDays = 30,
            daysRemaining = 25,
        )

        assertThat(next.forPeriod(BudgetPeriod.DAILY)).isEqualTo(next.dailyAllocation)
        assertThat(next.forPeriod(BudgetPeriod.WEEKLY)).isEqualTo(next.weeklyAllocation)
        assertThat(next.forPeriod(BudgetPeriod.BIWEEKLY)).isEqualTo(next.biweeklyAllocation)
        assertThat(next.forPeriod(BudgetPeriod.MONTHLY)).isEqualTo(next.monthlyAllocation)
    }
}
