package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.computeDynamicAllocations
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.computeNextBlockAllocations
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.splitBudget
import logcat.logcat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class BudgetCalculator @Inject constructor() {
    fun calculate(
        settings: BudgetSettings,
        transactions: List<Transaction>,
        currentDate: LocalDate
    ): BudgetState {
        logcat { "calculate called: settings=$settings, currentDate=$currentDate" }

        val periodEnd = settings.endDate ?: calculatePeriodEnd(settings.startDate, settings.period)
        logcat { "periodEnd calculated: $periodEnd (from endDate=${settings.endDate} or period=${settings.period})" }

        val totalDaysInPeriod = ChronoUnit.DAYS.between(settings.startDate, periodEnd).toInt() + 1
        logcat { "totalDaysInPeriod: $totalDaysInPeriod (from ${settings.startDate} to $periodEnd)" }

        val daysRemaining = ChronoUnit.DAYS.between(currentDate, periodEnd).toInt() + 1
        logcat { "daysRemaining: $daysRemaining (from $currentDate to $periodEnd)" }

        val totalSpentInPeriod = transactions
            .filter { !it.isDeleted }
            .sumOf { it.amount }
        logcat { "totalSpentInPeriod: $totalSpentInPeriod" }

        val remainingBudget = settings.totalBudget.subtract(totalSpentInPeriod)
        logcat { "remainingBudget: $remainingBudget" }

        val dailyBudget = when (settings.splitMode) {
            BudgetSplitMode.DYNAMIC -> {
                if (daysRemaining <= 0) {
                    BigDecimal.ZERO
                } else {
                    val remaining = settings.totalBudget.subtract(totalSpentInPeriod)
                    if (remaining <= BigDecimal.ZERO) {
                        BigDecimal.ZERO
                    } else {
                        remaining.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
                    }
                }
            }

            BudgetSplitMode.STATIC -> {
                if (totalDaysInPeriod > 0) {
                    settings.totalBudget.divide(
                        BigDecimal(totalDaysInPeriod),
                        2,
                        RoundingMode.HALF_UP
                    )
                } else {
                    BigDecimal.ZERO
                }
            }
        }
        logcat { "dailyBudget: $dailyBudget (splitMode=${settings.splitMode}, totalBudget=${settings.totalBudget}, daysRemaining=$daysRemaining, totalSpentInPeriod=$totalSpentInPeriod)" }

        val spentToday = transactions
            .filter {
                !it.isDeleted && it.date?.toLocalDate() == currentDate
            }
            .sumOf { it.amount }
        logcat { "spentToday: $spentToday" }

        val remainingToday = dailyBudget.subtract(spentToday)
        logcat { "remainingToday: $remainingToday (dailyBudget=$dailyBudget - spentToday=$spentToday)" }

        val progress = if (settings.totalBudget > BigDecimal.ZERO) {
            totalSpentInPeriod
                .divide(settings.totalBudget, 4, java.math.RoundingMode.HALF_UP)
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        val totalDaysClamped = totalDaysInPeriod.coerceAtLeast(1)
        val daysRemainingClamped = daysRemaining.coerceAtLeast(0)
        val (daily, weekly, biweekly, monthly, isOverDaily) = if (settings.splitMode == BudgetSplitMode.DYNAMIC) {
            val a = computeDynamicAllocations(
                totalBudget = settings.totalBudget,
                totalSpentInPeriod = totalSpentInPeriod,
                totalSpentToday = spentToday,
                daysRemaining = daysRemainingClamped,
            )
            Quintuple(
                a.dailyAllocation, a.weeklyAllocation,
                a.biweeklyAllocation, a.monthlyAllocation,
                a.isTodayOverDailyAllocation,
            )
        } else {
            Quintuple(
                daily = splitBudget(
                    totalBudget = settings.totalBudget, totalSpent = totalSpentInPeriod,
                    totalDays = totalDaysClamped, daysRemaining = daysRemainingClamped,
                    period = BudgetPeriod.DAILY, mode = BudgetSplitMode.STATIC,
                ),
                weekly = splitBudget(
                    totalBudget = settings.totalBudget, totalSpent = totalSpentInPeriod,
                    totalDays = totalDaysClamped, daysRemaining = daysRemainingClamped,
                    period = BudgetPeriod.WEEKLY, mode = BudgetSplitMode.STATIC,
                ),
                biweekly = splitBudget(
                    totalBudget = settings.totalBudget, totalSpent = totalSpentInPeriod,
                    totalDays = totalDaysClamped, daysRemaining = daysRemainingClamped,
                    period = BudgetPeriod.BIWEEKLY, mode = BudgetSplitMode.STATIC,
                ),
                monthly = splitBudget(
                    totalBudget = settings.totalBudget, totalSpent = totalSpentInPeriod,
                    totalDays = totalDaysClamped, daysRemaining = daysRemainingClamped,
                    period = BudgetPeriod.MONTHLY, mode = BudgetSplitMode.STATIC,
                ),
                isOverDaily = false,
            )
        }

        val nextAllocations = computeNextBlockAllocations(
            totalBudget = settings.totalBudget,
            totalSpentInPeriod = totalSpentInPeriod,
            totalDays = totalDaysInPeriod,
            daysRemaining = daysRemainingClamped,
        )

        return BudgetState(
            remainingToday = remainingToday,
            totalSpentToday = spentToday,
            dailyBudget = dailyBudget,
            daysRemaining = daysRemainingClamped,
            progress = progress,
            isOverBudget = remainingBudget < BigDecimal.ZERO,
            totalBudget = settings.totalBudget,
            totalSpentInPeriod = totalSpentInPeriod,
            dailyAllocation = daily,
            weeklyAllocation = weekly,
            biweeklyAllocation = biweekly,
            monthlyAllocation = monthly,
            isTodayOverDailyAllocation = isOverDaily,
            nextDailyAllocation = nextAllocations.dailyAllocation,
            nextWeeklyAllocation = nextAllocations.weeklyAllocation,
            nextBiweeklyAllocation = nextAllocations.biweeklyAllocation,
            nextMonthlyAllocation = nextAllocations.monthlyAllocation,
            periodTotalDays = totalDaysInPeriod,
        ).also {
            logcat { "BudgetState result: $it" }
        }
    }

    private data class Quintuple(
        val daily: BigDecimal,
        val weekly: BigDecimal,
        val biweekly: BigDecimal,
        val monthly: BigDecimal,
        val isOverDaily: Boolean,
    )

    private fun calculatePeriodEnd(start: LocalDate, period: BudgetPeriod): LocalDate {
        return when (period) {
            BudgetPeriod.DAILY -> start
            BudgetPeriod.WEEKLY -> start.plusWeeks(1)
            BudgetPeriod.BIWEEKLY -> start.plusWeeks(2)
            BudgetPeriod.MONTHLY -> start.plusMonths(1)
        }
    }
}
