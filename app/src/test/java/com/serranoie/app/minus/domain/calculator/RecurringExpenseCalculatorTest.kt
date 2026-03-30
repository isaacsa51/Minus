package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class RecurringExpenseCalculatorTest {

    private val calculator = RecurringExpenseCalculator()

    @Test
    fun weeklyRecurring_dueOnSeventhDay_returnsTrue() {
        val start = LocalDate.of(2026, 3, 1)
        val tx = recurrentTransaction(start, RecurrentFrequency.WEEKLY)

        assertTrue(calculator.isRecurringDueToday(tx, start.plusDays(7)))
        assertFalse(calculator.isRecurringDueToday(tx, start.plusDays(6)))
    }

    @Test
    fun monthlyRecurring_usesSubscriptionDay() {
        val start = LocalDate.of(2026, 1, 2)
        val tx = recurrentTransaction(start, RecurrentFrequency.MONTHLY, subscriptionDay = 10)

        assertTrue(calculator.isRecurringDueToday(tx, LocalDate.of(2026, 2, 10)))
        assertFalse(calculator.isRecurringDueToday(tx, LocalDate.of(2026, 2, 9)))
    }

    @Test
    fun calculateRecurringDueToday_sumsOnlyDueRecurringTransactions() {
        val today = LocalDate.of(2026, 3, 15)
        val dueWeekly = recurrentTransaction(today.minusDays(14), RecurrentFrequency.WEEKLY, amount = BigDecimal("5.00"))
        val dueMonthly = recurrentTransaction(today.minusMonths(1), RecurrentFrequency.MONTHLY, subscriptionDay = 15, amount = BigDecimal("7.50"))
        val notDue = recurrentTransaction(today.minusDays(1), RecurrentFrequency.WEEKLY, amount = BigDecimal("20.00"))

        val result = calculator.calculateRecurringDueToday(listOf(dueWeekly, dueMonthly, notDue), today)

        assertEquals(BigDecimal("12.50"), result)
    }

    private fun recurrentTransaction(
        startDate: LocalDate,
        frequency: RecurrentFrequency,
        subscriptionDay: Int? = null,
        amount: BigDecimal = BigDecimal("10.00")
    ): Transaction = Transaction.create(
        amount = amount,
        comment = "",
        date = startDate.atStartOfDay(),
        isRecurrent = true,
        recurrentFrequency = frequency,
        recurrentEndDate = LocalDateTime.of(2027, 1, 1, 0, 0),
        subscriptionDay = subscriptionDay
    )
}
