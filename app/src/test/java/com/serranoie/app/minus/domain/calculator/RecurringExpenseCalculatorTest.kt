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
    fun calculateMonthlyEquivalent_normalizesEachFrequencyToAMonthlyAmount() {
        val weekly = recurrentTransaction(LocalDate.of(2026, 1, 1), RecurrentFrequency.WEEKLY, amount = BigDecimal("10.00"))
        val biweekly = recurrentTransaction(LocalDate.of(2026, 1, 1), RecurrentFrequency.BIWEEKLY, amount = BigDecimal("20.00"))
        val monthly = recurrentTransaction(LocalDate.of(2026, 1, 1), RecurrentFrequency.MONTHLY, amount = BigDecimal("15.00"))

        val result = calculator.calculateMonthlyEquivalent(listOf(weekly, biweekly, monthly))

        // 10 * 4.33 + 20 * 2.17 + 15 * 1 = 43.30 + 43.40 + 15.00 = 101.70
        assertEquals(BigDecimal("101.70"), result)
    }

    @Test
    fun calculateMonthlyEquivalent_emptyListReturnsZero() {
        assertEquals(BigDecimal("0.00"), calculator.calculateMonthlyEquivalent(emptyList()))
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
