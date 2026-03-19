package com.serranoie.app.minus.domain.calculator

import android.util.Log
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val TAG = "BudgetCalculator - ISAAC"

/**
 * Calculates budget state based on settings, transactions, and current date.
 * Pure business logic - no Android dependencies.
 */
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
        Log.d(TAG, "calculate called: settings=$settings, currentDate=$currentDate")

        // Calculate period end date - use actual endDate from settings if available
        val periodEnd = settings.endDate ?: calculatePeriodEnd(settings.startDate, settings.period)
        Log.d(TAG, "periodEnd calculated: $periodEnd (from endDate=${settings.endDate} or period=${settings.period})")

        // Calculate total days in the period
        val totalDaysInPeriod = ChronoUnit.DAYS.between(settings.startDate, periodEnd).toInt() + 1
        Log.d(TAG, "totalDaysInPeriod: $totalDaysInPeriod (from ${settings.startDate} to $periodEnd)")

        // Calculate days remaining (including today)
        val daysRemaining = ChronoUnit.DAYS.between(currentDate, periodEnd).toInt() + 1
        Log.d(TAG, "daysRemaining: $daysRemaining (from $currentDate to $periodEnd)")

        // Calculate total spent in period (excluding deleted transactions)
        val totalSpentInPeriod = transactions
            .filter { !it.isDeleted }
            .sumOf { it.amount }
        Log.d(TAG, "totalSpentInPeriod: $totalSpentInPeriod")

        // Calculate remaining budget for the entire period
        val remainingBudget = settings.totalBudget.subtract(totalSpentInPeriod)
        Log.d(TAG, "remainingBudget: $remainingBudget")

        // Calculate daily budget based on TOTAL budget and TOTAL days in period
        // This should be constant throughout the period, not changing based on remaining days
        val dailyBudget = if (totalDaysInPeriod > 0) {
            settings.totalBudget.divide(
                BigDecimal(totalDaysInPeriod),
                2,
                RoundingMode.HALF_UP
            )
        } else {
            BigDecimal.ZERO
        }
        Log.d(TAG, "dailyBudget: $dailyBudget (totalBudget=${settings.totalBudget} / totalDaysInPeriod=$totalDaysInPeriod)")

        // Calculate spent today
        val spentToday = transactions
            .filter {
                !it.isDeleted && it.date?.toLocalDate() == currentDate
            }
            .sumOf { it.amount }
        Log.d(TAG, "spentToday: $spentToday")

        // Calculate remaining for today
        val remainingToday = dailyBudget.subtract(spentToday)
        Log.d(TAG, "remainingToday: $remainingToday (dailyBudget=$dailyBudget - spentToday=$spentToday)")

        // Calculate progress percentage
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
            Log.d(TAG, "BudgetState result: $it")
        }
    }

    /**
     * Calculate the end date of a budget period.
     */
    private fun calculatePeriodEnd(start: LocalDate, period: BudgetPeriod): LocalDate {
        return when (period) {
            BudgetPeriod.DAILY -> start
            BudgetPeriod.WEEKLY -> start.plusWeeks(1)
            BudgetPeriod.BIWEEKLY -> start.plusWeeks(2)
            BudgetPeriod.MONTHLY -> start.plusMonths(1)
        }
    }

    /**
     * Get the display label for a budget period.
     */
    fun getPeriodLabel(period: BudgetPeriod): String {
        return when (period) {
            BudgetPeriod.DAILY -> "Daily"
            BudgetPeriod.WEEKLY -> "Weekly"
            BudgetPeriod.BIWEEKLY -> "Bi-weekly"
            BudgetPeriod.MONTHLY -> "Monthly"
        }
    }
}
