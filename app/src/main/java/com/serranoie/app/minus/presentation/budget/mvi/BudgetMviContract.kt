package com.serranoie.app.minus.presentation.budget.mvi

interface BudgetUiIntent

sealed interface BudgetUiEffect {
    data class ShowMessage(val message: String) : BudgetUiEffect
}
