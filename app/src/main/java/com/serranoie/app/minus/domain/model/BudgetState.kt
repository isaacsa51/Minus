package com.serranoie.app.minus.domain.model

import java.math.BigDecimal


data class BudgetState(
    val remainingToday: BigDecimal,
    val totalSpentToday: BigDecimal,
    val dailyBudget: BigDecimal,
    val daysRemaining: Int,
    val progress: Float,
    val isOverBudget: Boolean,
    val totalBudget: BigDecimal,
    val totalSpentInPeriod: BigDecimal,
    val totalSpentThisWeek: BigDecimal = BigDecimal.ZERO,
    val totalSpentThisBiweek: BigDecimal = BigDecimal.ZERO,
    val totalSpentThisMonth: BigDecimal = BigDecimal.ZERO,
    val dailyAllocation: BigDecimal = BigDecimal.ZERO,
    val weeklyAllocation: BigDecimal = BigDecimal.ZERO,
    val biweeklyAllocation: BigDecimal = BigDecimal.ZERO,
    val monthlyAllocation: BigDecimal = BigDecimal.ZERO,
    val isTodayOverDailyAllocation: Boolean = false,
    val nextDailyAllocation: BigDecimal = BigDecimal.ZERO,
    val nextWeeklyAllocation: BigDecimal = BigDecimal.ZERO,
    val nextBiweeklyAllocation: BigDecimal = BigDecimal.ZERO,
    val nextMonthlyAllocation: BigDecimal = BigDecimal.ZERO,
    val periodTotalDays: Int = 0,
) {
    companion object {
        val EMPTY = BudgetState(
            remainingToday = BigDecimal.ZERO,
            totalSpentToday = BigDecimal.ZERO,
            dailyBudget = BigDecimal.ZERO,
            daysRemaining = 0,
            progress = 0f,
            isOverBudget = false,
            totalBudget = BigDecimal.ZERO,
            totalSpentInPeriod = BigDecimal.ZERO,
        )
    }

    fun allocationFor(period: BudgetPeriod): BigDecimal = when (period) {
        BudgetPeriod.DAILY -> dailyAllocation
        BudgetPeriod.WEEKLY -> weeklyAllocation
        BudgetPeriod.BIWEEKLY -> biweeklyAllocation
        BudgetPeriod.MONTHLY -> monthlyAllocation
    }

    fun nextAllocationFor(period: BudgetPeriod): BigDecimal = when (period) {
        BudgetPeriod.DAILY -> nextDailyAllocation
        BudgetPeriod.WEEKLY -> nextWeeklyAllocation
        BudgetPeriod.BIWEEKLY -> nextBiweeklyAllocation
        BudgetPeriod.MONTHLY -> nextMonthlyAllocation
    }
}
