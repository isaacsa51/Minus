package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class RecurringExpenseCalculator @Inject constructor() {

    fun calculateRecurringDueToday(transactions: List<Transaction>, today: LocalDate): BigDecimal {
        val recurrentTransactions = transactions.filter { it.isRecurrent && !it.isDeleted }
        return recurrentTransactions.filter { transaction ->
            isRecurringDueToday(transaction, today)
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
}
