package com.serranoie.app.minus.presentation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val budgetTransactionHandler: BudgetTransactionHandler,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<HistoryUiEffect>()
    val effects: SharedFlow<HistoryUiEffect> = _effects.asSharedFlow()

    private var autoDismissJob: Job? = null

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                budgetTransactionHandler.budgetRepository.getTransactions(),
                budgetTransactionHandler.budgetRepository.getBudgetSettings(),
                settingsRepository.observeSettings(),
            ) { transactions, settings, userSettings ->
                Triple(transactions, settings, userSettings)
            }.collect { (transactions, settings, userSettings) ->
                recomputeDerivedState(
                    transactions = transactions,
                    budgetSettings = settings,
                    budgetState = null,
                    userSettings = userSettings,
                )
            }
        }
    }

    fun processIntent(intent: HistoryUiIntent) {
        when (intent) {
            is HistoryUiIntent.ToggleExpandedDate -> toggleExpandedDate(intent.date)
            is HistoryUiIntent.SetEditingTransaction -> _uiState.value =
                _uiState.value.copy(editingTransaction = intent.transaction)

            is HistoryUiIntent.SetRecurrentToDelete -> _uiState.value = _uiState.value.copy(
                recurrentToDelete = intent.transaction,
                showDeleteRecurrentDialog = intent.transaction != null,
            )

            is HistoryUiIntent.SetRecurrentToEdit -> _uiState.value =
                _uiState.value.copy(recurrentToEdit = intent.transaction)

            is HistoryUiIntent.SetSelectedTransaction -> _uiState.value = _uiState.value.copy(
                selectedTransaction = intent.transaction,
                isDismissingTransactionDialog = false,
            )

            is HistoryUiIntent.DismissDeleteRecurrentDialog -> _uiState.value = _uiState.value.copy(
                recurrentToDelete = null,
                showDeleteRecurrentDialog = false,
            )

            is HistoryUiIntent.TogglePastPeriod -> _uiState.value =
                _uiState.value.copy(showPastPeriod = intent.visible)

            is HistoryUiIntent.ToggleOutOfPeriodSubscriptions -> _uiState.value =
                _uiState.value.copy(showOutOfPeriodSubscriptions = intent.visible)

            is HistoryUiIntent.ToggleUpcomingRecurrentInPeriod -> _uiState.value =
                _uiState.value.copy(showUpcomingRecurrentInPeriod = intent.visible)

            is HistoryUiIntent.SetDismissingDialog -> _uiState.value =
                _uiState.value.copy(isDismissingTransactionDialog = intent.dismissing)

            is HistoryUiIntent.DeleteTransaction -> deleteTransaction(intent.transaction)
            is HistoryUiIntent.SaveEditedTransaction -> saveEditedTransaction(intent.transaction)
            is HistoryUiIntent.ConfirmDeleteRecurrent -> confirmDeleteRecurrent(intent.transaction)
            is HistoryUiIntent.SetLockSwipeable -> _uiState.value =
                _uiState.value.copy(lockSwipeable = intent.locked)
        }
    }

    private fun deleteTransaction(transaction: Transaction) {
        autoDismissJob?.cancel()
        _uiState.value = _uiState.value.copy(
            pendingRemovedTransactions = _uiState.value.pendingRemovedTransactions + (transaction.id to transaction),
        )
        autoDismissJob = viewModelScope.launch {
            delay(EXIT_ANIMATION_DURATION_MS)
            budgetTransactionHandler.deleteTransaction(transaction)
            _uiState.value = _uiState.value.copy(pendingRemovedTransactions = emptyMap())
        }
    }

    private fun saveEditedTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val success = budgetTransactionHandler.editTransaction(transaction)
            if (success) {
                _uiState.value = _uiState.value.copy(editingTransaction = null)
            } else {
                _effects.emit(HistoryUiEffect.ShowSnackbar("Could not save transaction"))
            }
        }
    }

    private fun confirmDeleteRecurrent(transaction: Transaction) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(recurrentToDelete = null, showDeleteRecurrentDialog = false)
            budgetTransactionHandler.deleteTransaction(transaction)
        }
    }

    private fun toggleExpandedDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            expandedDates = _uiState.value.expandedDates.let { expanded ->
                if (expanded.contains(date)) expanded - date else expanded + date
            },
        )
    }

    private fun recomputeDerivedState(
        transactions: List<Transaction>,
        budgetSettings: BudgetSettings?,
        budgetState: BudgetState?,
        userSettings: com.serranoie.app.minus.domain.model.UserSettings?,
    ) {
        val current = _uiState.value
        val pendingRemoved = current.pendingRemovedTransactions
        val displayTx = buildDisplayTransactions(transactions, pendingRemoved)

        val startDate = budgetSettings?.startDate ?: LocalDate.now().minusDays(30)
        val endDate = budgetSettings?.getPeriodEndDate() ?: LocalDate.now()
        val today = LocalDate.now()
        val currentPeriodId = budgetSettings?.let { computeCurrentPeriodId(transactions) } ?: 0L
        val previousPeriodId = currentPeriodId - 1

        val (currentPeriodTx, pastPeriodTx) = splitPeriodTransactions(
            transactions = displayTx,
            budgetStartDate = startDate,
            budgetEndDate = endDate,
            currentPeriodStartedAtMillis = current.currentPeriodStartedAtMillis,
            currentPeriodId = currentPeriodId,
            previousPeriodId = previousPeriodId,
        )

        val (upcomingInPeriod, futureOutOfPeriod) = buildUpcomingRecurrentItems(
            transactions = displayTx,
            budgetStartDate = startDate,
            budgetEndDate = endDate,
            today = today,
        )

        val groupedCurrent = buildGroupedCurrentTransactions(
            currentPeriodTransactions = currentPeriodTx,
            displayTransactions = displayTx,
            budgetStartDate = startDate,
            budgetEndDate = endDate,
            today = today,
        )

        val groupedPast = groupTransactionsByDate(pastPeriodTx)

        // Auto-expand first date group on initial load
        val autoExpanded = if (current.expandedDates.isEmpty()) {
            groupedCurrent.keys.filterNotNull().sortedDescending().take(1).toSet()
        } else {
            current.expandedDates
        }

        _uiState.value = current.copy(
            transactions = transactions,
            budgetSettings = budgetSettings,
            budgetState = budgetState,
            currentPeriodId = currentPeriodId,
            displayTransactions = displayTx,
            groupedCurrentTransactions = groupedCurrent,
            groupedPastTransactions = groupedPast,
            upcomingRecurrentInPeriod = upcomingInPeriod,
            futureRecurrentOutOfPeriod = futureOutOfPeriod,
            expandedDates = autoExpanded,
            recurrentPaymentsViewMode = userSettings?.recurrentPaymentsViewMode
                ?: RecurrentPaymentsViewMode.VERTICAL_LIST,
        )
    }

    private fun computeCurrentPeriodId(transactions: List<Transaction>): Long {
        return transactions.filter { it.periodId > 0L }.maxByOrNull { it.periodId }?.periodId ?: 0L
    }


    companion object {
        private const val EXIT_ANIMATION_DURATION_MS = 600L
    }

    override fun onCleared() {
        super.onCleared()
        autoDismissJob?.cancel()
    }
}
