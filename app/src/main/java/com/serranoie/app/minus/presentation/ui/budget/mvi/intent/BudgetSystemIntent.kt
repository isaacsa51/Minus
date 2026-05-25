package com.serranoie.app.minus.presentation.ui.budget.mvi.intent

import com.serranoie.app.minus.presentation.ui.budget.mvi.BudgetUiIntent

sealed interface BudgetSystemIntent : BudgetUiIntent {
    data object MarkFirstLaunchComplete : BudgetSystemIntent
    data class SetLockSwipeable(val locked: Boolean) : BudgetSystemIntent
    data class SetLockDraggable(val locked: Boolean) : BudgetSystemIntent
}
