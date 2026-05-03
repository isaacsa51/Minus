package com.serranoie.app.minus.presentation.budget.mvi.intent

import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiIntent

sealed interface BudgetNumpadIntent : BudgetUiIntent {
    data class NumberTapped(val digit: String) : BudgetNumpadIntent
    data object DotTapped : BudgetNumpadIntent
    data object BackspaceTapped : BudgetNumpadIntent
    data object ApplyTapped : BudgetNumpadIntent
    data object ResetInputTapped : BudgetNumpadIntent
    data class OperatorTapped(val operator: Char) : BudgetNumpadIntent
    data object EqualsTapped : BudgetNumpadIntent
    data class SetCalculationMode(val enabled: Boolean) : BudgetNumpadIntent
    data class SetDragProgress(val progress: Float) : BudgetNumpadIntent
    data object TriggerTestNotifications : BudgetNumpadIntent
}
