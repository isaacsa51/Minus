package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import logcat.logcat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class BudgetCalculator @Inject constructor() {

    /**
     * Calculate the current budget state.
     *
     * @param settings User's budget settings
     * @param transactions List of all transactions in the period
     * @param currentDate The date to calculate for (usually today)
     * @return BudgetState with all calculated values
     */
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

        val dailyBudget = if (totalDaysInPeriod > 0) {
            settings.totalBudget.divide(
                BigDecimal(totalDaysInPeriod),
                2,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }
        logcat { "dailyBudget: $dailyBudget (totalBudget=${settings.totalBudget} / totalDaysInPeriod=$totalDaysInPeriod)" }

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
                .divide(settings.totalBudget, 4, RoundingMode.HALF_UP)
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        }

        return BudgetState(
            remainingToday = remainingToday,
            totalSpentToday = spentToday,
            dailyBudget = dailyBudget,
            daysRemaining = daysRemaining.coerceAtLeast(0),
            progress = progress,
            isOverBudget = remainingBudget < BigDecimal.ZERO,
            totalBudget = settings.totalBudget,
            totalSpentInPeriod = totalSpentInPeriod
        ).also {
            logcat { "BudgetState result: $it" }
        }
    }

    private fun calculatePeriodEnd(start: LocalDate, period: BudgetPeriod): LocalDate {
        return when (period) {
            BudgetPeriod.DAILY -> start
            BudgetPeriod.WEEKLY -> start.plusWeeks(1)
            BudgetPeriod.BIWEEKLY -> start.plusWeeks(2)
            BudgetPeriod.MONTHLY -> start.plusMonths(1)
        }
    }

    fun getPeriodLabel(period: BudgetPeriod): String {
        return when (period) {
            BudgetPeriod.DAILY -> "Daily"
            BudgetPeriod.WEEKLY -> "Weekly"
            BudgetPeriod.BIWEEKLY -> "Bi-weekly"
            BudgetPeriod.MONTHLY -> "Monthly"
        }
    }
}
