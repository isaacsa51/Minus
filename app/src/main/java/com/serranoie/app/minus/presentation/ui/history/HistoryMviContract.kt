package com.serranoie.app.minus.presentation.ui.history

import com.serranoie.app.minus.domain.model.Transaction
import java.time.LocalDate

sealed interface HistoryUiIntent {
    data class ToggleExpandedDate(val date: LocalDate) : HistoryUiIntent
    data class SetEditingTransaction(val transaction: Transaction?) : HistoryUiIntent
    data class SetRecurrentToDelete(val transaction: Transaction?) : HistoryUiIntent
    data class SetRecurrentToEdit(val transaction: Transaction?) : HistoryUiIntent
    data class SetSelectedTransaction(val transaction: Transaction?) : HistoryUiIntent
    data object DismissDeleteRecurrentDialog : HistoryUiIntent
    data class TogglePastPeriod(val visible: Boolean) : HistoryUiIntent
    data class ToggleOutOfPeriodSubscriptions(val visible: Boolean) : HistoryUiIntent
    data class ToggleUpcomingRecurrentInPeriod(val visible: Boolean) : HistoryUiIntent
    data class SetDismissingDialog(val dismissing: Boolean) : HistoryUiIntent

    data class DeleteTransaction(val transaction: Transaction) : HistoryUiIntent
    data class SaveEditedTransaction(val transaction: Transaction) : HistoryUiIntent
    data class ConfirmDeleteRecurrent(val transaction: Transaction) : HistoryUiIntent

    data class SetLockSwipeable(val locked: Boolean) : HistoryUiIntent
}

sealed interface HistoryUiEffect {
    data class ShowSnackbar(val message: String) : HistoryUiEffect
}

data class HistoryUiState(
    val budgetSettings: com.serranoie.app.minus.domain.model.BudgetSettings? = null,
    val budgetState: com.serranoie.app.minus.domain.model.BudgetState? = null,
    val currentPeriodId: Long = 0L,
    val currentPeriodStartedAtMillis: Long = 0L,

    val transactions: List<Transaction> = emptyList(),

    val editingTransaction: Transaction? = null,
    val pendingRemovedTransactions: Map<Long, Transaction> = emptyMap(),
    val recurrentToDelete: Transaction? = null,
    val recurrentToEdit: Transaction? = null,
    val showDeleteRecurrentDialog: Boolean = false,
    val selectedTransaction: Transaction? = null,
    val isDismissingTransactionDialog: Boolean = false,

    val expandedDates: Set<LocalDate> = emptySet(),
    val showPastPeriod: Boolean = false,
    val showOutOfPeriodSubscriptions: Boolean = false,
    val showUpcomingRecurrentInPeriod: Boolean = true,
    val lockSwipeable: Boolean = true,

    val displayTransactions: List<Transaction> = emptyList(),
    val groupedCurrentTransactions: Map<LocalDate?, List<Transaction>> = emptyMap(),
    val groupedPastTransactions: Map<LocalDate?, List<Transaction>> = emptyMap(),
    val upcomingRecurrentInPeriod: List<com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem> = emptyList(),
    val futureRecurrentOutOfPeriod: List<com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem> = emptyList(),
)
