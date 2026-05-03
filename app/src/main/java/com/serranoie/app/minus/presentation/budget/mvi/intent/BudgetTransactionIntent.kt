package com.serranoie.app.minus.presentation.budget.mvi.intent

import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiIntent

sealed interface BudgetTransactionIntent : BudgetUiIntent {
    data class DeleteTransactionTapped(val transaction: Transaction) : BudgetTransactionIntent
    data class RestoreTransactionTapped(val transaction: Transaction) : BudgetTransactionIntent
    data class EditTransactionTapped(val updatedTransaction: Transaction) : BudgetTransactionIntent
}
