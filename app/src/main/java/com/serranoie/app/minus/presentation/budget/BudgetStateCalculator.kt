package com.serranoie.app.minus.presentation.budget

import com.serranoie.app.minus.domain.calculator.RecurringExpenseCalculator
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class BudgetStateCalculator @Inject constructor(
    private val recurringExpenseCalculator: RecurringExpenseCalculator,
) {

    fun filterPeriodTransactions(
        transactions: List<Transaction>,
        settings: BudgetSettings,
        currentPeriodId: Long,
        currentPeriodStartedAtMillis: Long,
    ): List<Transaction> {
        val periodEnd = settings.getPeriodEndDate()
        return transactions.filter { transaction ->
            if (currentPeriodId > 0L && transaction.periodId > 0L) {
                return@filter transaction.periodId == currentPeriodId
            }
            val txDate = transaction.date?.toLocalDate() ?: return@filter false
            if (txDate.isBefore(settings.startDate) || txDate.isAfter(periodEnd)) {
                return@filter false
            }
            if (txDate.isEqual(settings.startDate) && currentPeriodStartedAtMillis > 0L) {
                return@filter transaction.createdAt >= currentPeriodStartedAtMillis
            }
            true
        }
    }

    fun calculateBudgetState(
        settings: BudgetSettings,
        transactions: List<Transaction>,
        currentDate: LocalDate,
    ): BudgetState {
        val periodEnd = settings.getPeriodEndDate()
        val daysRemaining = ChronoUnit.DAYS.between(currentDate, periodEnd).toInt() + 1
        val originalTotalDays = ChronoUnit.DAYS.between(settings.startDate, periodEnd).toInt() + 1

        val totalSpentInPeriod = transactions.filter { !it.isDeleted }.sumOf { it.amount }

        val carryForFirstDay = if (
            settings.rollOverCarryForward && currentDate.isEqual(settings.startDate)
        ) {
            settings.rollOverLimit ?: BigDecimal.ZERO
        } else {
            BigDecimal.ZERO
        }

        val rolloverAmount = if (settings.rollOverCarryForward) {
            settings.rollOverLimit ?: BigDecimal.ZERO
        } else {
            BigDecimal.ZERO
        }

        val effectiveTotalBudget = settings.totalBudget.add(rolloverAmount)
        val remainingBudget = effectiveTotalBudget.subtract(totalSpentInPeriod)

        val originalDailyBudget = if (originalTotalDays > 0) {
            settings.totalBudget.divide(
                BigDecimal(originalTotalDays),
                2,
                RoundingMode.HALF_UP,
            )
        } else {
            BigDecimal.ZERO
        }

        val regularSpentToday =
            transactions.filter { !it.isDeleted && it.date?.toLocalDate() == currentDate }
                .sumOf { it.amount }

        val recurringDueToday = recurringExpenseCalculator.calculateRecurringDueToday(transactions, currentDate)
        val spentToday = regularSpentToday.add(recurringDueToday)
        val remainingToday = originalDailyBudget.add(carryForFirstDay).subtract(spentToday)

        val progress = if (effectiveTotalBudget > BigDecimal.ZERO) {
            totalSpentInPeriod.divide(effectiveTotalBudget, 4, RoundingMode.HALF_UP)
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        return BudgetState(
            remainingToday = remainingToday,
            totalSpentToday = spentToday,
            dailyBudget = originalDailyBudget,
            daysRemaining = daysRemaining.coerceAtLeast(0),
            progress = progress,
            isOverBudget = remainingBudget < BigDecimal.ZERO,
            totalBudget = effectiveTotalBudget,
            totalSpentInPeriod = totalSpentInPeriod.add(recurringDueToday),
        )
    }
}
