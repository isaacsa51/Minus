package com.serranoie.app.minus.presentation.ui.budget

import com.serranoie.app.minus.domain.calculator.RecurringExpenseCalculator
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.computeDynamicAllocations
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.splitBudget
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
        paidOccurrences: Set<PaidRecurrentOccurrence> = emptySet(),
    ): BudgetState {
        val periodEnd = settings.getPeriodEndDate()
        val daysRemaining = ChronoUnit.DAYS.between(currentDate, periodEnd).toInt() + 1
        val originalTotalDays = ChronoUnit.DAYS.between(settings.startDate, periodEnd).toInt() + 1

        val activeTransactions = transactions.filter { !it.isDeleted && !it.isRecurrent }
        val totalExpensesInPeriod = activeTransactions
            .filter { it.amount > BigDecimal.ZERO && !it.isAdjustment }
            .sumOf { it.amount }
        val totalIncomeInPeriod = activeTransactions
            .filter { it.amount < BigDecimal.ZERO }
            .sumOf { it.amount }
            .abs()
        val totalDecreasesInPeriod = activeTransactions
            .filter { it.amount > BigDecimal.ZERO && it.isAdjustment }
            .sumOf { it.amount }

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

        val effectiveTotalBudget = settings.totalBudget
            .add(rolloverAmount)
            .add(totalIncomeInPeriod)
            .subtract(totalDecreasesInPeriod)
        val remainingBudget = effectiveTotalBudget.subtract(totalExpensesInPeriod)
        val originalDailyBudget = when (settings.splitMode) {
            BudgetSplitMode.DYNAMIC -> {
                if (daysRemaining <= 0) {
                    BigDecimal.ZERO
                } else {
                    val remaining = effectiveTotalBudget.subtract(totalExpensesInPeriod)
                    if (remaining <= BigDecimal.ZERO) {
                        BigDecimal.ZERO
                    } else {
                        remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
                    }
                }
            }

            BudgetSplitMode.STATIC -> {
                if (originalTotalDays > 0) {
                    settings.totalBudget.divide(
                        BigDecimal(originalTotalDays),
                        2,
                        RoundingMode.HALF_UP,
                    )
                } else {
                    BigDecimal.ZERO
                }
            }
        }

        val todayTransactions = activeTransactions.filter { it.date?.toLocalDate() == currentDate }
        val regularSpentToday = todayTransactions.filter { it.amount > BigDecimal.ZERO }.sumOf { it.amount }
        val incomeToday = todayTransactions.filter { it.amount < BigDecimal.ZERO }.sumOf { it.amount }.abs()

        val recurringDueToday = recurringExpenseCalculator.calculateRecurringDueToday(
            transactions, currentDate, paidOccurrences
        )
        val spentToday = regularSpentToday.add(recurringDueToday)

        val totalSpentThisWeek = calculateSpentInSubPeriod(
            transactions, settings.startDate, currentDate, 7
        )
        val totalSpentThisBiweek = calculateSpentInSubPeriod(
            transactions, settings.startDate, currentDate, 14
        )
        val totalSpentThisMonth = calculateSpentInSubPeriod(
            transactions, settings.startDate, currentDate, 30
        )

        val remainingToday = originalDailyBudget.add(carryForFirstDay).add(incomeToday).subtract(spentToday)

        val progress = if (effectiveTotalBudget > BigDecimal.ZERO) {
            totalExpensesInPeriod.divide(effectiveTotalBudget, 4, RoundingMode.HALF_UP)
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        val allocations = if (settings.splitMode == BudgetSplitMode.DYNAMIC) {
            // DYNAMIC: (remaining) / blocksRemaining(daysRemaining, period)
            val a = computeDynamicAllocations(
                totalBudget = effectiveTotalBudget,
                totalSpentInPeriod = totalExpensesInPeriod.add(recurringDueToday),
                totalSpentToday = spentToday,
                daysRemaining = daysRemaining.coerceAtLeast(0),
            )
            Quintuple(
                a.dailyAllocation, a.weeklyAllocation,
                a.biweeklyAllocation, a.monthlyAllocation,
                a.isTodayOverDailyAllocation,
            )
        } else {
            // STATIC: totalBudget / (totalDays / periodBlockDays) for each
            val totalDaysClamped = originalTotalDays.coerceAtLeast(1)
            val totalSpent = totalExpensesInPeriod.add(recurringDueToday)
            Quintuple(
                daily = splitBudget(
                    totalBudget = effectiveTotalBudget, totalSpent = totalSpent,
                    totalDays = totalDaysClamped, daysRemaining = daysRemaining.coerceAtLeast(0),
                    period = BudgetPeriod.DAILY, mode = BudgetSplitMode.STATIC,
                ),
                weekly = splitBudget(
                    totalBudget = effectiveTotalBudget, totalSpent = totalSpent,
                    totalDays = totalDaysClamped, daysRemaining = daysRemaining.coerceAtLeast(0),
                    period = BudgetPeriod.WEEKLY, mode = BudgetSplitMode.STATIC,
                ),
                biweekly = splitBudget(
                    totalBudget = effectiveTotalBudget, totalSpent = totalSpent,
                    totalDays = totalDaysClamped, daysRemaining = daysRemaining.coerceAtLeast(0),
                    period = BudgetPeriod.BIWEEKLY, mode = BudgetSplitMode.STATIC,
                ),
                monthly = splitBudget(
                    totalBudget = effectiveTotalBudget, totalSpent = totalSpent,
                    totalDays = totalDaysClamped, daysRemaining = daysRemaining.coerceAtLeast(0),
                    period = BudgetPeriod.MONTHLY, mode = BudgetSplitMode.STATIC,
                ),
                isOverDaily = false,
            )
        }

        return BudgetState(
            remainingToday = remainingToday,
            totalSpentToday = spentToday,
            dailyBudget = originalDailyBudget,
            daysRemaining = daysRemaining.coerceAtLeast(0),
            progress = progress,
            isOverBudget = remainingBudget < BigDecimal.ZERO,
            totalBudget = effectiveTotalBudget,
            totalSpentInPeriod = totalExpensesInPeriod.add(recurringDueToday),
            totalSpentThisWeek = totalSpentThisWeek,
            totalSpentThisBiweek = totalSpentThisBiweek,
            totalSpentThisMonth = totalSpentThisMonth,
            dailyAllocation = allocations.daily,
            weeklyAllocation = allocations.weekly,
            biweeklyAllocation = allocations.biweekly,
            monthlyAllocation = allocations.monthly,
            isTodayOverDailyAllocation = allocations.isOverDaily,
        )
    }

    private fun calculateSpentInSubPeriod(
        transactions: List<Transaction>,
        budgetStart: LocalDate,
        currentDate: LocalDate,
        blockDays: Int,
    ): BigDecimal {
        val daysFromStart = ChronoUnit.DAYS.between(budgetStart, currentDate)
        if (daysFromStart < 0) return BigDecimal.ZERO

        val currentBlockIndex = daysFromStart / blockDays
        val blockStart = budgetStart.plusDays(currentBlockIndex * blockDays)
        val blockEnd = blockStart.plusDays(blockDays.toLong() - 1)

        return transactions.filter { !it.isDeleted && !it.isRecurrent }
            .filter {
                val txDate = it.date?.toLocalDate()
                txDate != null && !txDate.isBefore(blockStart) && !txDate.isAfter(blockEnd)
            }
            .sumOf { it.amount }
    }
}

private data class Quintuple(
    val daily: BigDecimal,
    val weekly: BigDecimal,
    val biweekly: BigDecimal,
    val monthly: BigDecimal,
    val isOverDaily: Boolean,
)
