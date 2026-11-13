package com.serranoie.app.minus.presentation.ui.editor.dialogs

import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

sealed interface PlannedPreview {
    val isNegative: Boolean get() = false

    data class Daily(val before: BigDecimal, val after: BigDecimal) : PlannedPreview {
        override val isNegative: Boolean get() = after.signum() < 0
    }

    data class LeftInPeriod(val before: BigDecimal, val after: BigDecimal) : PlannedPreview {
        override val isNegative: Boolean get() = after.signum() < 0
    }

    data class OtherPeriod(val day: LocalDate) : PlannedPreview
}

fun plannedPreview(
    settings: BudgetSettings?,
    state: BudgetState?,
    amount: BigDecimal?,
    day: LocalDate,
): PlannedPreview? {
    if (settings == null || state == null || amount == null) return null
    if (day.isAfter(settings.getPeriodEndDate())) return PlannedPreview.OtherPeriod(day)
    if (settings.splitMode == BudgetSplitMode.DYNAMIC && state.daysRemaining > 0) {
        val held = amount.divide(BigDecimal(state.daysRemaining), 2, RoundingMode.HALF_UP)
        return PlannedPreview.Daily(state.dailyBudget, state.dailyBudget.subtract(held))
    }
    val left = state.totalBudget.subtract(state.totalSpentInPeriod)
    return PlannedPreview.LeftInPeriod(left, left.subtract(amount))
}
