package com.serranoie.app.minus.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class ArchivedBudget(
    val periodId: Long,
    val totalBudget: BigDecimal,
    val spentAmount: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val currencyCode: String,
    val periodType: BudgetPeriod,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isOverBudget: Boolean get() = spentAmount > totalBudget
    val savedAmount: BigDecimal get() = totalBudget.subtract(spentAmount).coerceAtLeast(BigDecimal.ZERO)
}
