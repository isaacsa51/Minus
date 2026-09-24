package com.serranoie.app.minus.presentation.ui.budget

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.LeftoverChoice
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.earnedAllowance
import com.serranoie.app.minus.presentation.ui.history.splitRecurringAndOneTime
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class BudgetStateCalculator @Inject constructor() {

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
        allTransactions: List<Transaction> = transactions,
        reserveUpcomingCharges: Boolean = false,
        leftoverChoices: Map<LocalDate, LeftoverChoice> = emptyMap(),
    ): BudgetState {
        val periodEnd = settings.getPeriodEndDate()
        val daysRemaining = ChronoUnit.DAYS.between(currentDate, periodEnd).toInt() + 1
        val originalTotalDays = ChronoUnit.DAYS.between(settings.startDate, periodEnd).toInt() + 1

        val activeTransactions = transactions.filter { !it.isDeleted && !it.isRecurrent }
        val unpaidRecurringCharges = splitRecurringAndOneTime(
            allTransactions = allTransactions,
            filteredTransactions = transactions,
            periodStart = settings.startDate,
            periodEnd = periodEnd,
            today = if (reserveUpcomingCharges) periodEnd else currentDate,
            paidOccurrences = paidOccurrences,
        ).first
        val totalExpensesInPeriod = activeTransactions
            .filter { it.amount > BigDecimal.ZERO && !it.isAdjustment }
            .sumOf { it.amount }
            .add(unpaidRecurringCharges.sumOf { it.amount })
        val totalIncomeInPeriod = activeTransactions
            .filter { it.amount < BigDecimal.ZERO }
            .sumOf { it.amount }
            .abs()
        val totalDecreasesInPeriod = activeTransactions
            .filter { it.amount > BigDecimal.ZERO && it.isAdjustment }
            .sumOf { it.amount }

        val carry = if (settings.rollOverCarryForward) {
            settings.rollOverLimit ?: BigDecimal.ZERO
        } else {
            BigDecimal.ZERO
        }
        val carryForFirstDay =
            if (currentDate.isEqual(settings.rollOverAppliedDate ?: settings.startDate)) carry else BigDecimal.ZERO
        val splitBudget = settings.totalBudget.subtract(carry)
        val leftovers = if (settings.splitMode == BudgetSplitMode.ASK_ME) {
            replayLeftovers(
                settings, splitBudget, carry, originalTotalDays, currentDate,
                activeTransactions + unpaidRecurringCharges, leftoverChoices,
            )
        } else {
            null
        }

        val effectiveTotalBudget = settings.totalBudget
            .add(totalIncomeInPeriod)
            .subtract(totalDecreasesInPeriod)
        val todayTransactions = activeTransactions.filter { it.date?.toLocalDate() == currentDate }
        val regularSpentToday = todayTransactions.filter { it.amount > BigDecimal.ZERO }.sumOf { it.amount }
        val incomeToday = todayTransactions.filter { it.amount < BigDecimal.ZERO }.sumOf { it.amount }.abs()

        val recurringDueToday = unpaidRecurringCharges
            .filter { it.date?.toLocalDate() == currentDate }
            .sumOf { it.amount }
        val spentToday = regularSpentToday.add(recurringDueToday)

        val remainingBudget = effectiveTotalBudget.subtract(totalExpensesInPeriod)
        val originalDailyBudget = when (settings.splitMode) {
            BudgetSplitMode.DYNAMIC -> {
                if (daysRemaining <= 0) {
                    BigDecimal.ZERO
                } else {
                    val remaining = effectiveTotalBudget.subtract(totalExpensesInPeriod)
                        .subtract(carryForFirstDay)
                        .add(spentToday)
                    if (remaining <= BigDecimal.ZERO) {
                        BigDecimal.ZERO
                    } else {
                        remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
                    }
                }
            }

            BudgetSplitMode.ASK_ME -> leftovers?.rate?.setScale(2, RoundingMode.HALF_UP) ?: BigDecimal.ZERO

            BudgetSplitMode.STATIC, BudgetSplitMode.CARRY_OVER -> {
                if (originalTotalDays > 0) {
                    splitBudget.divide(
                        BigDecimal(originalTotalDays),
                        2,
                        RoundingMode.HALF_UP,
                    )
                } else {
                    BigDecimal.ZERO
                }
            }
        }

        val totalSpentThisWeek = calculateSpentInSubPeriod(
            activeTransactions + unpaidRecurringCharges, settings.startDate, currentDate, 7
        )
        val totalSpentThisBiweek = calculateSpentInSubPeriod(
            activeTransactions + unpaidRecurringCharges, settings.startDate, currentDate, 14
        )
        val totalSpentThisMonth = calculateSpentInSubPeriod(
            activeTransactions + unpaidRecurringCharges, settings.startDate, currentDate, 30
        )

        val remainingToday = if (leftovers != null) {
            leftovers.remainingToday
        } else if (settings.splitMode == BudgetSplitMode.CARRY_OVER) {
            val surplus =
                if (currentDate.isBefore(settings.rollOverAppliedDate ?: settings.startDate)) BigDecimal.ZERO else carry
            earnedAllowance(splitBudget, originalTotalDays - daysRemaining + 1, originalTotalDays)
                .add(surplus)
                .add(totalIncomeInPeriod)
                .subtract(totalDecreasesInPeriod)
                .subtract(totalExpensesInPeriod)
        } else {
            originalDailyBudget.add(carryForFirstDay).add(incomeToday).subtract(spentToday)
        }

        val progress = if (effectiveTotalBudget > BigDecimal.ZERO) {
            totalExpensesInPeriod.divide(effectiveTotalBudget, 4, RoundingMode.HALF_UP)
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        val totalSpentInPeriod = totalExpensesInPeriod

        return BudgetState(
            remainingToday = remainingToday,
            totalSpentToday = spentToday,
            dailyBudget = originalDailyBudget,
            daysRemaining = daysRemaining.coerceAtLeast(0),
            progress = progress,
            isOverBudget = remainingBudget < BigDecimal.ZERO,
            totalBudget = effectiveTotalBudget,
            totalSpentInPeriod = totalSpentInPeriod,
            totalSpentThisWeek = totalSpentThisWeek,
            totalSpentThisBiweek = totalSpentThisBiweek,
            totalSpentThisMonth = totalSpentThisMonth,
            periodTotalDays = originalTotalDays,
            reservedCharges = unpaidRecurringCharges.filter { it.date?.toLocalDate()?.isAfter(currentDate) == true },
            splitBudget = leftovers?.rate
                ?.multiply(BigDecimal(originalTotalDays))
                ?.setScale(2, RoundingMode.HALF_UP)
                ?: splitBudget,
            pendingLeftover = leftovers?.pending ?: BigDecimal.ZERO,
        )
    }

    private class LeftoverReplay(val rate: BigDecimal, val remainingToday: BigDecimal, val pending: BigDecimal)

    private fun replayLeftovers(
        settings: BudgetSettings,
        splitBudget: BigDecimal,
        carry: BigDecimal,
        totalDays: Int,
        currentDate: LocalDate,
        flows: List<Transaction>,
        choices: Map<LocalDate, LeftoverChoice>,
    ): LeftoverReplay {
        val start = settings.startDate
        val end = settings.getPeriodEndDate()
        val base = if (totalDays > 0) {
            splitBudget.divide(BigDecimal(totalDays), MathContext.DECIMAL64)
        } else {
            BigDecimal.ZERO
        }
        if (currentDate.isBefore(start)) {
            return LeftoverReplay(base, flows.sumOf { it.amount }.negate(), BigDecimal.ZERO)
        }
        val lastDay = minOf(currentDate, end)
        val outflow = flows
            .groupBy { (it.date?.toLocalDate() ?: lastDay).coerceIn(start, lastDay) }
            .mapValues { (_, dayFlows) -> dayFlows.sumOf { it.amount } }
        val surplusDay = maxOf(settings.rollOverAppliedDate ?: start, start)

        var rate = base
        var pending = BigDecimal.ZERO
        var allowance = BigDecimal.ZERO
        var day = start
        while (!day.isAfter(lastDay)) {
            if (day != start) {
                pending += allowance.subtract(outflow[day.minusDays(1)] ?: BigDecimal.ZERO)
            }
            allowance = rate
            if (pending.signum() < 0 || choices[day] == LeftoverChoice.CARRY) {
                allowance += pending
                pending = BigDecimal.ZERO
            } else if (choices[day] == LeftoverChoice.SPREAD) {
                val daysLeft = totalDays - ChronoUnit.DAYS.between(start, day).toInt()
                rate += pending.divide(BigDecimal(daysLeft), MathContext.DECIMAL64)
                allowance = rate
                pending = BigDecimal.ZERO
            }
            if (day == surplusDay) allowance += carry
            day = day.plusDays(1)
        }

        var remaining = allowance.subtract(outflow[lastDay] ?: BigDecimal.ZERO)
        if (currentDate.isAfter(end)) {
            remaining += pending
            pending = BigDecimal.ZERO
        }
        return LeftoverReplay(
            rate = rate,
            remainingToday = remaining.setScale(2, RoundingMode.HALF_UP),
            pending = pending.setScale(2, RoundingMode.HALF_UP),
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

        return transactions
            .filter {
                val txDate = it.date?.toLocalDate()
                txDate != null && !txDate.isBefore(blockStart) && !txDate.isAfter(blockEnd)
            }
            .sumOf { it.amount }
    }
}
