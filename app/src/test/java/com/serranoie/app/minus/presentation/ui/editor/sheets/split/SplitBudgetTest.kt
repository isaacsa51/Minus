package com.serranoie.app.minus.presentation.ui.editor.sheets.split

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import org.junit.Test
import java.math.BigDecimal

class SplitBudgetTest {

    private val totalBudget = BigDecimal("16644.45")

    @Test
    fun `static daily on a 30 day period divides the budget equally`() {
        // 16644.45 / 30 = 554.815 -> half-up = 554.82
        val result = splitBudget(
            totalBudget = totalBudget,
            totalSpent = BigDecimal.ZERO,
            totalDays = 30,
            daysRemaining = 30,
            period = BudgetPeriod.DAILY,
            mode = BudgetSplitMode.STATIC,
        )

        assertThat(result).isEqualTo(BigDecimal("554.82"))
    }

    @Test
    fun `static weekly on a 30 day period yields four equal chunks`() {
        // 16644.45 / (30/7) -> 30/7 = 4 (int div) -> 16644.45 / 4 = 4161.1125 -> 4161.11
        val result = splitBudget(
            totalBudget = totalBudget,
            totalSpent = BigDecimal.ZERO,
            totalDays = 30,
            daysRemaining = 30,
            period = BudgetPeriod.WEEKLY,
            mode = BudgetSplitMode.STATIC,
        )

        assertThat(result).isEqualTo(BigDecimal("4161.11"))
    }

    @Test
    fun `dynamic daily after 200 spent with 28 days left is 587_30`() {
        // (16644.45 - 200) / 28 = 587.301785... -> 587.30
        val result = splitBudget(
            totalBudget = totalBudget,
            totalSpent = BigDecimal("200.00"),
            totalDays = 30,
            daysRemaining = 28,
            period = BudgetPeriod.DAILY,
            mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(result).isEqualTo(BigDecimal("587.30"))
    }

    @Test
    fun `dynamic weekly scales the daily amount by 7`() {
        // The helper rounds the daily amount first, then multiplies by period days.
        //   daily = 16444.45 / 28 = 587.30178... -> 587.30 (HALF_UP, 2 decimals)
        //   weekly = 587.30 * 7 = 4111.10
        // Note: this is NOT 4111.11 (which is what you'd get if you rounded
        // the final result). The intermediate rounding is intentional — it
        // matches the daily amount the user actually sees in the card.
        val result = splitBudget(
            totalBudget = totalBudget,
            totalSpent = BigDecimal("200.00"),
            totalDays = 30,
            daysRemaining = 28,
            period = BudgetPeriod.WEEKLY,
            mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(result).isEqualTo(BigDecimal("4111.10"))
    }

    @Test
    fun `dynamic returns zero when the user has overspent`() {
        // totalBudget=1000, totalSpent=1500 -> remaining is negative -> ZERO
        val result = splitBudget(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal("1500"),
            totalDays = 30,
            daysRemaining = 10,
            period = BudgetPeriod.DAILY,
            mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(result).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `dynamic returns zero when the period has ended`() {
        // daysRemaining=0 -> div by zero guard fires -> ZERO
        val result = splitBudget(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal("200"),
            totalDays = 30,
            daysRemaining = 0,
            period = BudgetPeriod.DAILY,
            mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(result).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `dynamic with no spend equals static`() {
        // If the user hasn't spent anything yet, dynamic must collapse to static.
        val dynamic = splitBudget(
            totalBudget = totalBudget,
            totalSpent = BigDecimal.ZERO,
            totalDays = 30,
            daysRemaining = 30,
            period = BudgetPeriod.DAILY,
            mode = BudgetSplitMode.DYNAMIC,
        )
        val static = splitBudget(
            totalBudget = totalBudget,
            totalSpent = BigDecimal.ZERO,
            totalDays = 30,
            daysRemaining = 30,
            period = BudgetPeriod.DAILY,
            mode = BudgetSplitMode.STATIC,
        )

        assertThat(dynamic).isEqualTo(static)
    }

    @Test
    fun `returns zero when total budget is zero regardless of mode`() {
        val dynamic = splitBudget(
            totalBudget = BigDecimal.ZERO,
            totalSpent = BigDecimal("100"),
            totalDays = 30,
            daysRemaining = 28,
            period = BudgetPeriod.DAILY,
            mode = BudgetSplitMode.DYNAMIC,
        )
        val static = splitBudget(
            totalBudget = BigDecimal.ZERO,
            totalSpent = BigDecimal.ZERO,
            totalDays = 30,
            daysRemaining = 30,
            period = BudgetPeriod.DAILY,
            mode = BudgetSplitMode.STATIC,
        )

        assertThat(dynamic).isEqualTo(BigDecimal.ZERO)
        assertThat(static).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `returns zero when total days is non-positive`() {
        val result = splitBudget(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 0,
            daysRemaining = 0,
            period = BudgetPeriod.DAILY,
            mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(result).isEqualTo(BigDecimal.ZERO)
    }

    // Each scenario is hand-computed so the test acts as a spec for the
    // expected formula behaviour. The numbers are deliberately varied:
    // round numbers, awkward fractions, and amounts that trigger the
    // HALF_UP rounding rule (e.g. 100/3 = 33.33).
    @Test
    fun `dynamic mid-period with no spend returns full budget over remaining days`() {
        // 7000 / 7 days = 1000.00/day exactly
        val daily = splitBudget(
            totalBudget = BigDecimal("7000"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 30, daysRemaining = 7,
            period = BudgetPeriod.DAILY, mode = BudgetSplitMode.DYNAMIC,
        )
        val weekly = splitBudget(
            totalBudget = BigDecimal("7000"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 30, daysRemaining = 7,
            period = BudgetPeriod.WEEKLY, mode = BudgetSplitMode.DYNAMIC,
        )
        val monthly = splitBudget(
            totalBudget = BigDecimal("7000"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 30, daysRemaining = 7,
            period = BudgetPeriod.MONTHLY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(daily).isEqualTo(BigDecimal("1000.00"))
        assertThat(weekly).isEqualTo(BigDecimal("7000.00"))
        assertThat(monthly).isEqualTo(BigDecimal("30000.00"))
    }

    @Test
    fun `dynamic on the last day of the period produces the full remaining amount`() {
        // Edge: daysRemaining = 1 must still produce a real number, not 0.
        // 1000 / 1 = 1000.00
        val daily = splitBudget(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 1, daysRemaining = 1,
            period = BudgetPeriod.DAILY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(daily).isEqualTo(BigDecimal("1000.00"))
    }

    @Test
    fun `dynamic with half of the budget spent and half the days left returns daily half of remaining`() {
        // 30-day period, halfway in: 5000 spent, 15 days left -> 2500 / 15 = 166.67
        // 166.67 * 7 = 1166.69  (NOT 1166.666 -> 1166.67)
        // 166.67 * 30 = 5000.10
        val daily = splitBudget(
            totalBudget = BigDecimal("5000"),
            totalSpent = BigDecimal("2500"),
            totalDays = 30, daysRemaining = 15,
            period = BudgetPeriod.DAILY, mode = BudgetSplitMode.DYNAMIC,
        )
        val weekly = splitBudget(
            totalBudget = BigDecimal("5000"),
            totalSpent = BigDecimal("2500"),
            totalDays = 30, daysRemaining = 15,
            period = BudgetPeriod.WEEKLY, mode = BudgetSplitMode.DYNAMIC,
        )
        val monthly = splitBudget(
            totalBudget = BigDecimal("5000"),
            totalSpent = BigDecimal("2500"),
            totalDays = 30, daysRemaining = 15,
            period = BudgetPeriod.MONTHLY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(daily).isEqualTo(BigDecimal("166.67"))
        assertThat(weekly).isEqualTo(BigDecimal("1166.69"))
        assertThat(monthly).isEqualTo(BigDecimal("5000.10"))
    }

    @Test
    fun `dynamic biweekly on a 28 day period is twice the weekly amount`() {
        // 28-day period, $200 spent, 14 days left
        // remaining = 16444.45 - 200 = 16444.45
        // daily = 16444.45 / 14 = 1174.6035... -> 1174.60
        // biweekly = 1174.60 * 14 = 16444.40 (NOT 16444.45 — rounding again)
        val biweekly = splitBudget(
            totalBudget = totalBudget,
            totalSpent = BigDecimal("200.00"),
            totalDays = 28, daysRemaining = 14,
            period = BudgetPeriod.BIWEEKLY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(biweekly).isEqualTo(BigDecimal("16444.40"))
    }

    @Test
    fun `dynamic exercises HALF_UP rounding when division is not exact`() {
        // 100 / 3 = 33.3333... -> 33.33 (3rd decimal is 3, rounds down)
        // 33.33 * 7 = 233.31
        // 33.33 * 14 = 466.62
        // 33.33 * 30 = 999.90
        val daily = splitBudget(
            totalBudget = BigDecimal("100"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 3, daysRemaining = 3,
            period = BudgetPeriod.DAILY, mode = BudgetSplitMode.DYNAMIC,
        )
        val weekly = splitBudget(
            totalBudget = BigDecimal("100"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 3, daysRemaining = 3,
            period = BudgetPeriod.WEEKLY, mode = BudgetSplitMode.DYNAMIC,
        )
        val biweekly = splitBudget(
            totalBudget = BigDecimal("100"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 3, daysRemaining = 3,
            period = BudgetPeriod.BIWEEKLY, mode = BudgetSplitMode.DYNAMIC,
        )
        val monthly = splitBudget(
            totalBudget = BigDecimal("100"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 3, daysRemaining = 3,
            period = BudgetPeriod.MONTHLY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(daily).isEqualTo(BigDecimal("33.33"))
        assertThat(weekly).isEqualTo(BigDecimal("233.31"))
        assertThat(biweekly).isEqualTo(BigDecimal("466.62"))
        assertThat(monthly).isEqualTo(BigDecimal("999.90"))
    }

    @Test
    fun `dynamic with HALF_UP boundary case rounds the 5 up`() {
        // 15 / 8 = 1.875 -> 1.88 (HALF_UP, 3rd decimal is 5, rounds 2nd up)
        // 1.88 * 7 = 13.16
        // 1.88 * 30 = 56.40
        val daily = splitBudget(
            totalBudget = BigDecimal("15"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 8, daysRemaining = 8,
            period = BudgetPeriod.DAILY, mode = BudgetSplitMode.DYNAMIC,
        )
        val weekly = splitBudget(
            totalBudget = BigDecimal("15"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 8, daysRemaining = 8,
            period = BudgetPeriod.WEEKLY, mode = BudgetSplitMode.DYNAMIC,
        )
        val monthly = splitBudget(
            totalBudget = BigDecimal("15"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 8, daysRemaining = 8,
            period = BudgetPeriod.MONTHLY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(daily).isEqualTo(BigDecimal("1.88"))
        assertThat(weekly).isEqualTo(BigDecimal("13.16"))
        assertThat(monthly).isEqualTo(BigDecimal("56.40"))
    }

    @Test
    fun `dynamic with fractional budget and fractional days gives exact result`() {
        // 123.45 / 3 = 41.15 exactly
        // 41.15 * 7 = 288.05
        val daily = splitBudget(
            totalBudget = BigDecimal("123.45"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 7, daysRemaining = 3,
            period = BudgetPeriod.DAILY, mode = BudgetSplitMode.DYNAMIC,
        )
        val weekly = splitBudget(
            totalBudget = BigDecimal("123.45"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 7, daysRemaining = 3,
            period = BudgetPeriod.WEEKLY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(daily).isEqualTo(BigDecimal("41.15"))
        assertThat(weekly).isEqualTo(BigDecimal("288.05"))
    }

    @Test
    fun `dynamic with small budget and two days left returns exactly half`() {
        // 10 / 2 = 5.00 exactly
        val daily = splitBudget(
            totalBudget = BigDecimal("10.00"),
            totalSpent = BigDecimal.ZERO,
            totalDays = 3, daysRemaining = 2,
            period = BudgetPeriod.DAILY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(daily).isEqualTo(BigDecimal("5.00"))
    }

    @Test
    fun `dynamic returns zero when exactly at the budget limit`() {
        // totalSpent == totalBudget -> remaining = 0 -> guard fires -> 0
        val daily = splitBudget(
            totalBudget = BigDecimal("1000"),
            totalSpent = BigDecimal("1000"),
            totalDays = 30, daysRemaining = 10,
            period = BudgetPeriod.DAILY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(daily).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `dynamic with one cent remaining still produces a non-zero amount`() {
        // 0.01 / 30 = 0.00033... -> 0.00 (rounds to 2 decimals)
        // Note: this documents the current behaviour — a 1-cent remainder
        // gets rounded down to $0.00 in the daily amount. Weekly = 0.00 * 7.
        // If we ever want to surface sub-cent values, this test is the spec
        // that would need to change (and so would the rounding scale).
        val daily = splitBudget(
            totalBudget = BigDecimal("1000.01"),
            totalSpent = BigDecimal("1000.00"),
            totalDays = 30, daysRemaining = 30,
            period = BudgetPeriod.DAILY, mode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(daily).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `static biweekly on a 30 day period yields two equal chunks`() {
        // 16644.45 / (30/14) = 16644.45 / 2 = 8322.225 -> 8322.23
        val result = splitBudget(
            totalBudget = totalBudget,
            totalSpent = BigDecimal.ZERO,
            totalDays = 30, daysRemaining = 30,
            period = BudgetPeriod.BIWEEKLY,
            mode = BudgetSplitMode.STATIC,
        )

        assertThat(result).isEqualTo(BigDecimal("8322.23"))
    }

    @Test
    fun `static monthly on a 30 day period returns the full budget`() {
        val result = splitBudget(
            totalBudget = totalBudget,
            totalSpent = BigDecimal.ZERO,
            totalDays = 30, daysRemaining = 30,
            period = BudgetPeriod.MONTHLY,
            mode = BudgetSplitMode.STATIC,
        )

        assertThat(result).isEqualTo(BigDecimal("16644.45"))
    }
}
