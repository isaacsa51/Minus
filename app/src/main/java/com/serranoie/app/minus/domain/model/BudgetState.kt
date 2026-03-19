package com.serranoie.app.minus.domain.model

import java.math.BigDecimal

/**
 * Calculated budget state for display in the UI.
 * This is a read-only data class representing the current budget snapshot.
 */
data class BudgetState(
    val remainingToday: BigDecimal,
    val totalSpentToday: BigDecimal,
    val dailyBudget: BigDecimal,
    val daysRemaining: Int,
    val progress: Float, // 0.0 to 1.0 (percentage of budget spent)
    val isOverBudget: Boolean,
    val totalBudget: BigDecimal,
    val totalSpentInPeriod: BigDecimal
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
            totalSpentInPeriod = BigDecimal.ZERO
        )
    }
}
