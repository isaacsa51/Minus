package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class RecurringExpenseCalculator @Inject constructor() {

    /**
     * Normalizes each transaction's amount to a monthly equivalent (weekly *~4.33,
     * biweekly *~2.17, monthly *1) and sums them — used for a "monthly commitment" total
     * across subscriptions on different billing cycles.
     */
    fun calculateMonthlyEquivalent(recurrentTransactions: List<Transaction>): BigDecimal {
        return recurrentTransactions.sumOf { transaction ->
            val frequency = transaction.recurrentFrequency ?: return@sumOf BigDecimal.ZERO
            val multiplier = when (frequency) {
                RecurrentFrequency.WEEKLY -> WEEKLY_TO_MONTHLY_MULTIPLIER
                RecurrentFrequency.BIWEEKLY -> BIWEEKLY_TO_MONTHLY_MULTIPLIER
                RecurrentFrequency.MONTHLY -> BigDecimal.ONE
            }
            transaction.amount.multiply(multiplier)
        }.setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateRecurringDueToday(
        transactions: List<Transaction>,
        today: LocalDate,
        paidOccurrences: Set<PaidRecurrentOccurrence> = emptySet(),
    ): BigDecimal {
        val recurrentTransactions = transactions.filter { it.isRecurrent && !it.isDeleted }
        return recurrentTransactions.filter { transaction ->
            isRecurringDueToday(transaction, today) &&
                !paidOccurrences.contains(PaidRecurrentOccurrence(transaction.id, today))
        }.sumOf { it.amount }
    }

    fun isRecurringDueToday(transaction: Transaction, today: LocalDate): Boolean {
        val frequency = transaction.recurrentFrequency ?: return false
        val startDate = transaction.date?.toLocalDate() ?: return false

        val endDate = transaction.recurrentEndDate?.toLocalDate()
        if (endDate != null && today.isAfter(endDate)) {
            return false
        }

        if (today.isBefore(startDate)) {
            return false
        }

        return when (frequency) {
            RecurrentFrequency.WEEKLY -> {
                val daysBetween = ChronoUnit.DAYS.between(startDate, today).toInt()
                daysBetween >= 0 && daysBetween % 7 == 0
            }

            RecurrentFrequency.BIWEEKLY -> {
                val daysBetween = ChronoUnit.DAYS.between(startDate, today).toInt()
                daysBetween >= 0 && daysBetween % 14 == 0
            }

            RecurrentFrequency.MONTHLY -> {
                val billingDay = transaction.subscriptionDay ?: startDate.dayOfMonth
                today.dayOfMonth == billingDay
            }
        }
    }

    private companion object {
        val WEEKLY_TO_MONTHLY_MULTIPLIER: BigDecimal = BigDecimal("4.33")
        val BIWEEKLY_TO_MONTHLY_MULTIPLIER: BigDecimal = BigDecimal("2.17")
    }
}
