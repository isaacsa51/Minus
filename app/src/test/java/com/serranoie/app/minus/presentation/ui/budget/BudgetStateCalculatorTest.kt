package com.serranoie.app.minus.presentation.ui.budget

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BudgetStateCalculatorTest {

    private val calculator = BudgetStateCalculator()

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

    @Test
    fun `marking a recurring occurrence paid on its due date does not double-count todays spend`() {
        val today = LocalDate.of(2026, 3, 15)
        val recurringTemplate = Transaction(
            id = 1L,
            amount = BigDecimal("15.00"),
            comment = "Netflix",
            date = LocalDate.of(2026, 2, 15).atStartOfDay(),
            periodId = 5L,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
        )
        val materializedFromMarkAsPaid = Transaction(
            id = 2L,
            amount = BigDecimal("15.00"),
            comment = "Netflix",
            date = today.atStartOfDay(),
            periodId = 5L,
            isRecurrent = false,
        )

        val result = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = BigDecimal("1000"),
                start = LocalDate.of(2026, 3, 1),
                end = LocalDate.of(2026, 3, 31),
            ),
            transactions = listOf(recurringTemplate, materializedFromMarkAsPaid),
            currentDate = today,
            paidOccurrences = setOf(PaidRecurrentOccurrence(transactionId = 1L, occurrenceDate = today)),
        )

        assertThat(result.totalSpentToday).isEqualTo(BigDecimal("15.00"))
        assertThat(result.totalSpentInPeriod).isEqualTo(BigDecimal("15.00"))
    }

    @Test
    fun `a recurring templates own row never counts toward period totals, only its projection or a materialized transaction does`() {
        val today = LocalDate.of(2026, 3, 15)
        val recurringTemplate = Transaction(
            id = 1L,
            amount = BigDecimal("15.00"),
            comment = "Netflix",
            date = LocalDate.of(2026, 2, 15).atStartOfDay(),
            periodId = 5L,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
        )

        val result = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = BigDecimal("1000"),
                start = LocalDate.of(2026, 3, 1),
                end = LocalDate.of(2026, 3, 31),
            ),
            transactions = listOf(recurringTemplate),
            currentDate = today,
        )

        assertThat(result.totalSpentInPeriod).isEqualTo(BigDecimal("15.00"))
        assertThat(result.totalSpentToday).isEqualTo(BigDecimal("15.00"))
    }

    @Test
    fun `an unpaid recurring occurrence still counts as projected spend today`() {
        val today = LocalDate.of(2026, 3, 15)
        val recurringTemplate = Transaction(
            id = 1L,
            amount = BigDecimal("15.00"),
            comment = "Netflix",
            date = LocalDate.of(2026, 2, 15).atStartOfDay(),
            periodId = 5L,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
        )

        val result = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = BigDecimal("1000"),
                start = LocalDate.of(2026, 3, 1),
                end = LocalDate.of(2026, 3, 31),
            ),
            transactions = listOf(recurringTemplate),
            currentDate = today,
            paidOccurrences = emptySet(),
        )

        assertThat(result.totalSpentToday).isEqualTo(BigDecimal("15.00"))
    }

    @Test
    fun `rollOverLimit via rollOverAppliedDate is added to remainingToday only on its target date, not on startDate`() {
        val withoutRollover = settings(
            totalBudget = BigDecimal("2000"),
            start = LocalDate.of(2026, 7, 1),
            end = LocalDate.of(2026, 7, 20),
            splitMode = BudgetSplitMode.DYNAMIC,
        )
        val withRollover = withoutRollover.copy(
            totalBudget = BigDecimal("2100.00"),
            rollOverCarryForward = true,
            rollOverLimit = BigDecimal("100.00"),
            rollOverAppliedDate = LocalDate.of(2026, 7, 11),
        )

        val baselineOnAppliedDate = calculator.calculateBudgetState(
            settings = withoutRollover, transactions = emptyList(), currentDate = LocalDate.of(2026, 7, 11),
        )
        val onAppliedDate = calculator.calculateBudgetState(
            settings = withRollover, transactions = emptyList(), currentDate = LocalDate.of(2026, 7, 11),
        )
        val onStartDate = calculator.calculateBudgetState(
            settings = withRollover, transactions = emptyList(), currentDate = LocalDate.of(2026, 7, 1),
        )
        val baselineOnStartDate = calculator.calculateBudgetState(
            settings = withoutRollover, transactions = emptyList(), currentDate = LocalDate.of(2026, 7, 1),
        )

        assertThat(onAppliedDate.remainingToday)
            .isEqualTo(baselineOnAppliedDate.remainingToday.add(BigDecimal("100.00")))
        assertThat(onStartDate.remainingToday).isEqualTo(onStartDate.dailyBudget)
        assertThat(onStartDate.totalBudget).isEqualTo(baselineOnStartDate.totalBudget.add(BigDecimal("100.00")))
    }

    @Test
    fun `rollOverAppliedDate falls back to startDate when not set, matching the previous behavior`() {
        val settingsWithRollover = settings(
            totalBudget = BigDecimal("2050"),
            start = LocalDate.of(2026, 7, 1),
            end = LocalDate.of(2026, 7, 20),
            splitMode = BudgetSplitMode.STATIC,
            carryForward = true,
            rolloverLimit = BigDecimal("50.00"),
        )

        val onStartDate = calculator.calculateBudgetState(
            settings = settingsWithRollover, transactions = emptyList(), currentDate = LocalDate.of(2026, 7, 1),
        )
        val onOtherDay = calculator.calculateBudgetState(
            settings = settingsWithRollover, transactions = emptyList(), currentDate = LocalDate.of(2026, 7, 2),
        )

        assertThat(onStartDate.remainingToday).isEqualTo(onStartDate.dailyBudget.add(BigDecimal("50.00")))
        assertThat(onOtherDay.remainingToday).isEqualTo(onOtherDay.dailyBudget)
        assertThat(onOtherDay.dailyBudget).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun `spending the day-one carry does not put the rest of the period over budget, and an unspent carry stays in the total`() {
        val carried = settings(
            totalBudget = BigDecimal("1200"),
            start = LocalDate.of(2026, 7, 1),
            end = LocalDate.of(2026, 7, 10),
            splitMode = BudgetSplitMode.STATIC,
            carryForward = true,
            rolloverLimit = BigDecimal("200.00"),
        )
        val spentItAll = listOf(transaction(amount = BigDecimal("300.00"), date = LocalDate.of(2026, 7, 1)))

        val dayOne = calculator.calculateBudgetState(carried, spentItAll, LocalDate.of(2026, 7, 1))
        val dayTwo = calculator.calculateBudgetState(carried, spentItAll, LocalDate.of(2026, 7, 2))
        val dayTwoUntouched = calculator.calculateBudgetState(carried, emptyList(), LocalDate.of(2026, 7, 2))

        assertThat(dayOne.remainingToday).isEqualTo(BigDecimal("0.00"))
        assertThat(dayOne.isOverBudget).isFalse()
        assertThat(dayTwo.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(dayTwo.remainingToday).isEqualTo(BigDecimal("100.00"))
        assertThat(dayTwo.isOverBudget).isFalse()
        assertThat(dayTwo.totalBudget.subtract(dayTwo.totalSpentInPeriod)).isEqualTo(BigDecimal("900.00"))
        assertThat(dayTwoUntouched.dailyBudget).isEqualTo(BigDecimal("100.00"))
        assertThat(dayTwoUntouched.totalBudget.subtract(dayTwoUntouched.totalSpentInPeriod)).isEqualTo(BigDecimal("1200"))
    }

    @Test
    fun `an unpaid subscription charge due earlier in the period counts as spent, even when its template belongs to an older period`() {
        val today = LocalDate.of(2026, 3, 20)
        val netflix = Transaction(
            id = 1L,
            amount = BigDecimal("15.00"),
            comment = "Netflix",
            date = LocalDate.of(2026, 1, 15).atStartOfDay(),
            periodId = 3L,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
        )
        val coffee = Transaction(
            id = 2L,
            amount = BigDecimal("5.00"),
            comment = "Coffee",
            date = today.atStartOfDay(),
            periodId = 5L,
        )

        val result = calculator.calculateBudgetState(
            settings = settings(
                totalBudget = BigDecimal("1000"),
                start = LocalDate.of(2026, 3, 1),
                end = LocalDate.of(2026, 3, 31),
                splitMode = BudgetSplitMode.DYNAMIC,
            ),
            transactions = listOf(coffee),
            currentDate = today,
            allTransactions = listOf(netflix, coffee),
        )

        assertThat(result.totalSpentInPeriod).isEqualTo(BigDecimal("20.00"))
        assertThat(result.totalSpentToday).isEqualTo(BigDecimal("5.00"))
        assertThat(result.remainingToday).isEqualTo(BigDecimal("76.67"))
    }
}
