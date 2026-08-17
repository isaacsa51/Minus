package com.serranoie.app.minus.presentation.ui.history

import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class HistoryCalculationsTest {

    private fun recurrentTransaction(
        id: Long,
        startDate: LocalDate,
        frequency: RecurrentFrequency = RecurrentFrequency.MONTHLY,
        subscriptionDay: Int? = null,
    ): Transaction = Transaction(
        id = id,
        amount = BigDecimal("15.00"),
        comment = "Netflix",
        date = startDate.atStartOfDay(),
        isRecurrent = true,
        recurrentFrequency = frequency,
        subscriptionDay = subscriptionDay,
    )

    @Test
    fun when_next_charge_date_is_paid_then_item_is_excluded_from_upcoming_in_period() {
        val today = LocalDate.of(2026, 3, 1)
        val periodStart = LocalDate.of(2026, 3, 1)
        val periodEnd = LocalDate.of(2026, 3, 31)
        val tx = recurrentTransaction(id = 1L, startDate = LocalDate.of(2026, 1, 15), subscriptionDay = 15)
        val nextChargeDate = LocalDate.of(2026, 3, 15)

        val (upcoming, _) = buildUpcomingRecurrentItems(
            transactions = listOf(tx),
            budgetStartDate = periodStart,
            budgetEndDate = periodEnd,
            today = today,
            paidOccurrences = setOf(PaidRecurrentOccurrence(tx.id, nextChargeDate)),
        )

        assertTrue(upcoming.isEmpty())
    }

    @Test
    fun when_next_charge_date_is_not_paid_then_item_is_included() {
        val today = LocalDate.of(2026, 3, 1)
        val periodStart = LocalDate.of(2026, 3, 1)
        val periodEnd = LocalDate.of(2026, 3, 31)
        val tx = recurrentTransaction(id = 1L, startDate = LocalDate.of(2026, 1, 15), subscriptionDay = 15)

        val (upcoming, _) = buildUpcomingRecurrentItems(
            transactions = listOf(tx),
            budgetStartDate = periodStart,
            budgetEndDate = periodEnd,
            today = today,
            paidOccurrences = emptySet(),
        )

        assertEquals(1, upcoming.size)
        assertEquals(LocalDate.of(2026, 3, 15), upcoming.first().nextChargeDate)
    }

    @Test
    fun when_a_different_transactions_occurrence_is_paid_then_this_item_is_unaffected() {
        val today = LocalDate.of(2026, 3, 1)
        val periodStart = LocalDate.of(2026, 3, 1)
        val periodEnd = LocalDate.of(2026, 3, 31)
        val tx = recurrentTransaction(id = 1L, startDate = LocalDate.of(2026, 1, 15), subscriptionDay = 15)
        val otherPaidOccurrence = PaidRecurrentOccurrence(
            transactionId = 2L,
            occurrenceDate = LocalDate.of(2026, 3, 15),
        )

        val (upcoming, _) = buildUpcomingRecurrentItems(
            transactions = listOf(tx),
            budgetStartDate = periodStart,
            budgetEndDate = periodEnd,
            today = today,
            paidOccurrences = setOf(otherPaidOccurrence),
        )

        assertEquals(1, upcoming.size)
    }

    @Test
    fun when_out_of_period_charge_date_is_paid_then_item_is_excluded_from_future_out_of_period() {
        val today = LocalDate.of(2026, 3, 1)
        val periodStart = LocalDate.of(2026, 3, 1)
        val periodEnd = LocalDate.of(2026, 3, 10)
        val tx = recurrentTransaction(id = 1L, startDate = LocalDate.of(2026, 1, 15), subscriptionDay = 15)
        val nextChargeDate = LocalDate.of(2026, 3, 15)

        val (upcoming, future) = buildUpcomingRecurrentItems(
            transactions = listOf(tx),
            budgetStartDate = periodStart,
            budgetEndDate = periodEnd,
            today = today,
            paidOccurrences = setOf(PaidRecurrentOccurrence(tx.id, nextChargeDate)),
        )

        assertTrue(upcoming.isEmpty())
        assertTrue(future.isEmpty())
    }

    @Test
    fun when_out_of_period_charge_date_is_not_paid_then_item_appears_in_future_out_of_period() {
        val today = LocalDate.of(2026, 3, 1)
        val periodStart = LocalDate.of(2026, 3, 1)
        val periodEnd = LocalDate.of(2026, 3, 10)
        val tx = recurrentTransaction(id = 1L, startDate = LocalDate.of(2026, 1, 15), subscriptionDay = 15)

        val (upcoming, future) = buildUpcomingRecurrentItems(
            transactions = listOf(tx),
            budgetStartDate = periodStart,
            budgetEndDate = periodEnd,
            today = today,
        )

        assertTrue(upcoming.isEmpty())
        assertEquals(1, future.size)
        assertEquals(LocalDate.of(2026, 3, 15), future.first().nextChargeDate)
    }

    @Test
    fun when_one_occurrence_is_paid_then_only_that_occurrence_is_excluded() {
        val periodStart = LocalDate.of(2026, 3, 1)
        val periodEnd = LocalDate.of(2026, 3, 31)
        val today = LocalDate.of(2026, 3, 31)
        val tx = recurrentTransaction(id = 1L, startDate = LocalDate.of(2026, 3, 2), frequency = RecurrentFrequency.WEEKLY)
        val paidDate = LocalDate.of(2026, 3, 9)

        val charges = getRecurringChargesInPeriod(
            transaction = tx,
            periodStart = periodStart,
            periodEnd = periodEnd,
            today = today,
            paidOccurrences = setOf(PaidRecurrentOccurrence(tx.id, paidDate)),
        )

        val chargeDates = charges.map { it.date?.toLocalDate() }
        assertTrue(chargeDates.contains(LocalDate.of(2026, 3, 2)))
        assertTrue(!chargeDates.contains(paidDate))
        assertTrue(chargeDates.contains(LocalDate.of(2026, 3, 16)))
    }

    @Test
    fun when_no_occurrences_are_paid_then_all_occurrences_in_period_are_returned() {
        val periodStart = LocalDate.of(2026, 3, 1)
        val periodEnd = LocalDate.of(2026, 3, 31)
        val today = LocalDate.of(2026, 3, 31)
        val tx = recurrentTransaction(id = 1L, startDate = LocalDate.of(2026, 3, 2), frequency = RecurrentFrequency.WEEKLY)

        val charges = getRecurringChargesInPeriod(
            transaction = tx,
            periodStart = periodStart,
            periodEnd = periodEnd,
            today = today,
        )

        assertEquals(5, charges.size)
    }
}
