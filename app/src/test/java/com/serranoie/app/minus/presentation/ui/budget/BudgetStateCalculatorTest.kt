package com.serranoie.app.minus.presentation.ui.budget

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.calculator.RecurringExpenseCalculator
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BudgetStateCalculatorTest {

    private val calculator = BudgetStateCalculator(RecurringExpenseCalculator())

    private fun settings(
        totalBudget: BigDecimal,
        start: LocalDate,
        end: LocalDate?,
        splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
        carryForward: Boolean = false,
        rolloverLimit: BigDecimal? = null,
    ): BudgetSettings = BudgetSettings(
        totalBudget = totalBudget,
        period = BudgetPeriod.DAILY,
        startDate = start,
        endDate = end,
        currencyCode = "USD",
        daysInPeriod = 1,
        rollOverEnabled = carryForward,
        rollOverLimit = rolloverLimit,
        rollOverCarryForward = carryForward,
        splitMode = splitMode,
    )

    @Test
    fun `static split - daily budget is totalBudget divided by period days`() {
        val result = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = BigDecimal("1000"),
                start = LocalDate.of(2026, 7, 1),
                end = LocalDate.of(2026, 7, 10),
                splitMode = BudgetSplitMode.STATIC,
            ),
            transactions = emptyList(),
            currentDate = LocalDate.of(2026, 7, 1),
        )
        assertThat(result.dailyBudget).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `static split - daily budget ignores how much was spent and how many days are left`() {
        // The static formula is fixed: it's always totalBudget / totalDays,
        // regardless of remaining days or spending. So the same totalBudget
        // and totalDays produce the same dailyBudget no matter what.
        val baseSettings = settings(
            totalBudget = BigDecimal("2000"),
            start = LocalDate.of(2026, 7, 1),
            end = LocalDate.of(2026, 7, 20),
            splitMode = BudgetSplitMode.STATIC,
        )
        val early = calculator.calculateBudgetState(
            settings = baseSettings,
            transactions = emptyList(),
            currentDate = LocalDate.of(2026, 7, 1),
        )
        val later = calculator.calculateBudgetState(
            settings = baseSettings,
            transactions = listOf(
                transaction(amount = BigDecimal("1500"), date = LocalDate.of(2026, 7, 5)),
            ),
            currentDate = LocalDate.of(2026, 7, 10),
        )
        assertThat(early.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(later.dailyBudget).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `dynamic split - daily budget is remaining divided by days remaining`() {
        val result = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = BigDecimal("2000"),
                start = LocalDate.of(2026, 7, 1),
                end = LocalDate.of(2026, 7, 20),
                splitMode = BudgetSplitMode.DYNAMIC,
            ),
            transactions = listOf(
                transaction(amount = BigDecimal("500"), date = LocalDate.of(2026, 7, 2)),
            ),
            currentDate = LocalDate.of(2026, 7, 16),
        )
        assertThat(result.dailyBudget).isEqualTo(BigDecimal("300.00"))
    }

    @Test
    fun `dynamic split - daily budget goes up when the day rolls over and nothing is spent`() {
        val totalBudget = BigDecimal("16773.63")
        val totalSpent = BigDecimal("2204.50")
        val start = LocalDate.of(2026, 7, 7)
        val end = LocalDate.of(2026, 7, 31)

        val day1 = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = totalBudget,
                start = start,
                end = end,
                splitMode = BudgetSplitMode.DYNAMIC,
            ),
            transactions = listOf(
                transaction(amount = totalSpent, date = LocalDate.of(2026, 7, 7)),
            ),
            currentDate = LocalDate.of(2026, 7, 10),
        )
        assertThat(day1.dailyBudget).isEqualTo(BigDecimal("662.23"))

        val day2 = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = totalBudget,
                start = start,
                end = end,
                splitMode = BudgetSplitMode.DYNAMIC,
            ),
            transactions = listOf(
                transaction(amount = totalSpent, date = LocalDate.of(2026, 7, 7)),
            ),
            currentDate = LocalDate.of(2026, 7, 11),
        )
        assertThat(day2.dailyBudget).isEqualTo(BigDecimal("693.77"))

        // The key invariant: the daily amount INCREASES when the user
        // doesn't spend and a day rolls over. This is the intended
        // behavior of the DYNAMIC split mode (and the source of the
        // user's confusion: "why is it more if I didn't spend
        // yesterday?"). The fix is to make the top-bar pill use the
        // same formula so it doesn't appear stuck.
        assertThat(day2.dailyBudget).isGreaterThan(day1.dailyBudget)
    }

    @Test
    fun `dynamic split - daily budget is zero when no days remain`() {
        val result = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = BigDecimal("1000"),
                start = LocalDate.of(2026, 7, 1),
                end = LocalDate.of(2026, 7, 10),
                splitMode = BudgetSplitMode.DYNAMIC,
            ),
            transactions = emptyList(),
            currentDate = LocalDate.of(2026, 7, 15),
        )
        assertThat(result.dailyBudget).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `dynamic split - daily budget is zero when the remaining balance is non-positive`() {
        // Over budget, don't show a negative daily.
        val result = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = BigDecimal("1000"),
                start = LocalDate.of(2026, 7, 1),
                end = LocalDate.of(2026, 7, 10),
                splitMode = BudgetSplitMode.DYNAMIC,
            ),
            transactions = listOf(
                transaction(amount = BigDecimal("1500"), date = LocalDate.of(2026, 7, 2)),
            ),
            currentDate = LocalDate.of(2026, 7, 3),
        )
        assertThat(result.dailyBudget).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `static and dynamic produce different daily budgets for the same data`() {
        val s = settings(
            totalBudget = BigDecimal("2000"),
            start = LocalDate.of(2026, 7, 1),
            end = LocalDate.of(2026, 7, 20),
        )
        val txs = listOf(
            transaction(amount = BigDecimal("500"), date = LocalDate.of(2026, 7, 2)),
        )
        val today = LocalDate.of(2026, 7, 10)

        val static = calculator.calculateBudgetState(
            settings = s.copy(splitMode = BudgetSplitMode.STATIC),
            transactions = txs,
            currentDate = today,
        )
        val dynamic = calculator.calculateBudgetState(
            settings = s.copy(splitMode = BudgetSplitMode.DYNAMIC),
            transactions = txs,
            currentDate = today,
        )
        assertThat(static.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(dynamic.dailyBudget).isEqualTo(BigDecimal("136.36"))
        assertThat(static.dailyBudget).isNotEqualTo(dynamic.dailyBudget)
    }

    private fun transaction(amount: BigDecimal, date: LocalDate): Transaction = Transaction(
        id = 1L,
        amount = amount,
        date = date.atTime(12, 0),
        periodId = 1L,
    )
}
