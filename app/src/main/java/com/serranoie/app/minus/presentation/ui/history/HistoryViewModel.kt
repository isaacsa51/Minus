package com.serranoie.app.minus.presentation.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.R
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "HistoryViewModel"

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val budgetTransactionHandler: BudgetTransactionHandler,
    private val settingsRepository: SettingsRepository,
    private val budgetStateCalculator: BudgetStateCalculator,
    private val observeCurrentPeriodBoundaryUseCase: ObserveCurrentPeriodBoundaryUseCase,
    private val persistBudgetSettingsUseCase: PersistBudgetSettingsUseCase,
    private val getCurrentPeriodIdUseCase: GetCurrentPeriodIdUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _expandedDates = MutableStateFlow(emptySet<LocalDate>())
    private val _editingTransaction = MutableStateFlow<Transaction?>(null)
    private val _recurrentToDelete = MutableStateFlow<Transaction?>(null)
    private val _recurrentToEdit = MutableStateFlow<Transaction?>(null)
    private val _showDeleteRecurrentDialog = MutableStateFlow(false)
    private val _showPastPeriod = MutableStateFlow(false)
    private val _showOutOfPeriodSubscriptions = MutableStateFlow(false)
    private val _showUpcomingRecurrentInPeriod = MutableStateFlow(true)
    private val _lockSwipeable = MutableStateFlow(true)
    private val _expandedTransactionId = MutableStateFlow<Long?>(null)
    private val _pendingRemovedTransactions = MutableStateFlow(emptyMap<Long, Transaction>())

    private val _effects = MutableSharedFlow<HistoryUiEffect>()
    val effects: SharedFlow<HistoryUiEffect> = _effects.asSharedFlow()

    private var autoDismissJob: Job? = null

    private val uiInputs = combine(
        listOf(
            _expandedDates,
            _editingTransaction,
            _recurrentToDelete,
            _recurrentToEdit,
            _showDeleteRecurrentDialog,
            _showPastPeriod,
            _showOutOfPeriodSubscriptions,
            _showUpcomingRecurrentInPeriod,
            _lockSwipeable,
            _expandedTransactionId,
            _pendingRemovedTransactions
        )
    ) { array ->
        UIInputs(
            expandedDates = array[0] as Set<LocalDate>,
            editingTransaction = array[1] as Transaction?,
            recurrentToDelete = array[2] as Transaction?,
            recurrentToEdit = array[3] as Transaction?,
            showDeleteRecurrentDialog = array[4] as Boolean,
            showPastPeriod = array[5] as Boolean,
            showOutOfPeriodSubscriptions = array[6] as Boolean,
            showUpcomingRecurrentInPeriod = array[7] as Boolean,
            lockSwipeable = array[8] as Boolean,
            expandedTransactionId = array[9] as Long?,
            pendingRemovedTransactions = array[10] as Map<Long, Transaction>
        )
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        budgetTransactionHandler.budgetRepository.getTransactions(),
        budgetTransactionHandler.budgetRepository.getBudgetSettings(),
        observeCurrentPeriodBoundaryUseCase(),
        settingsRepository.observeSettings(),
        budgetTransactionHandler.budgetRepository.getActiveCategories(),
        budgetTransactionHandler.budgetRepository.getPaidRecurrentOccurrences(),
        uiInputs
    ) { array ->
        val transactions = array[0] as List<Transaction>
        val budgetSettings = array[1] as BudgetSettings?
        @Suppress("UNCHECKED_CAST")
        val periodBoundary = array[2] as Pair<Long, Long>
        val userSettings = array[3] as UserSettings?
        @Suppress("UNCHECKED_CAST")
        val categories = array[4] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val paidOccurrences = array[5] as Set<PaidRecurrentOccurrence>
        val inputs = array[6] as UIInputs

        calculateHistoryUiState(
            transactions = transactions,
            budgetSettings = budgetSettings,
            currentPeriodStartedAtMillis = periodBoundary.first,
            currentPeriodId = periodBoundary.second,
            userSettings = userSettings,
            categories = categories,
            paidOccurrences = paidOccurrences,
            inputs = inputs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HistoryUiState()
    )

    fun processIntent(intent: HistoryUiIntent) {
        when (intent) {
            is HistoryUiIntent.ToggleExpandedDate -> toggleExpandedDate(intent.date)
            is HistoryUiIntent.SetEditingTransaction -> _editingTransaction.value = intent.transaction
            is HistoryUiIntent.SetRecurrentToDelete -> {
                _recurrentToDelete.value = intent.transaction
                _showDeleteRecurrentDialog.value = intent.transaction != null
            }

            is HistoryUiIntent.SetRecurrentToEdit -> _recurrentToEdit.value = intent.transaction
            is HistoryUiIntent.DismissDeleteRecurrentDialog -> {
                _recurrentToDelete.value = null
                _showDeleteRecurrentDialog.value = false
            }

            is HistoryUiIntent.TogglePastPeriod -> _showPastPeriod.value = intent.visible
            is HistoryUiIntent.ToggleOutOfPeriodSubscriptions -> _showOutOfPeriodSubscriptions.value = intent.visible
            is HistoryUiIntent.ToggleUpcomingRecurrentInPeriod -> _showUpcomingRecurrentInPeriod.value = intent.visible
            is HistoryUiIntent.DeleteTransaction -> deleteTransaction(intent.transaction)
            is HistoryUiIntent.SaveEditedTransaction -> saveEditedTransaction(intent.transaction)
            is HistoryUiIntent.ConfirmDeleteRecurrent -> confirmDeleteRecurrent(intent.transaction)
            is HistoryUiIntent.MarkTransactionAsPaid -> markTransactionAsPaid(intent.transaction)
            is HistoryUiIntent.SetLockSwipeable -> _lockSwipeable.value = intent.locked
            is HistoryUiIntent.ToggleExpandedTransaction -> toggleExpandedTransaction(intent.transactionId)
            is HistoryUiIntent.UpdateCreditCutoffDay -> updateCreditCutoffDay(intent.day)
        }
    }

    private fun toggleExpandedTransaction(id: Long?) {
        _expandedTransactionId.update { currentId ->
            if (currentId == id) null else id
        }
    }

    private fun updateCreditCutoffDay(day: Int) {
        val currentSettings = uiState.value.budgetSettings ?: return
        if (day !in 1..31) return

        viewModelScope.launch {
            persistBudgetSettingsUseCase(
                settings = currentSettings.copy(creditCardCutoffDay = day),
                forceNewPeriodBoundary = false,
            )
        }
    }

    private fun deleteTransaction(transaction: Transaction) {
        autoDismissJob?.cancel()
        _pendingRemovedTransactions.update { it + (transaction.id to transaction) }
        autoDismissJob = viewModelScope.launch {
            delay(EXIT_ANIMATION_DURATION_MS)
            val result = budgetTransactionHandler.deleteTransaction(transaction)
            _pendingRemovedTransactions.update { it - transaction.id }
            if (result.isFailure) {
                logcat(TAG) { "deleteTransaction failed for id=${transaction.id}: ${result.exceptionOrNull()}" }
                _effects.emit(
                    HistoryUiEffect.ShowSnackbar(
                        context.getString(R.string.history_snackbar_delete_transaction_failed)
                    )
                )
            }
        }
    }

    private fun saveEditedTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val result = budgetTransactionHandler.editTransaction(transaction)
            if (result.isSuccess) {
                _editingTransaction.value = null
            } else {
                logcat(TAG) { "saveEditedTransaction failed for id=${transaction.id}: ${result.exceptionOrNull()}" }
                _effects.emit(
                    HistoryUiEffect.ShowSnackbar(
                        context.getString(R.string.history_snackbar_save_transaction_failed)
                    )
                )
            }
        }
    }

    private fun confirmDeleteRecurrent(transaction: Transaction) {
        viewModelScope.launch {
            _recurrentToDelete.value = null
            _showDeleteRecurrentDialog.value = false
            val result = budgetTransactionHandler.deleteTransaction(transaction)
            if (result.isFailure) {
                logcat(TAG) { "confirmDeleteRecurrent failed for id=${transaction.id}: ${result.exceptionOrNull()}" }
                _effects.emit(
                    HistoryUiEffect.ShowSnackbar(
                        context.getString(R.string.history_snackbar_delete_recurrent_failed)
                    )
                )
            }
        }
    }

    private fun markTransactionAsPaid(transaction: Transaction) {
        viewModelScope.launch {
            val activePeriodId = getCurrentPeriodIdUseCase().takeIf { it > 0L }
                ?: uiState.value.currentPeriodId
            val result = budgetTransactionHandler.markRecurrentOccurrencePaid(transaction, activePeriodId)
            if (result.isFailure) {
                _effects.emit(
                    HistoryUiEffect.ShowSnackbar(
                        context.getString(R.string.history_snackbar_mark_paid_failed)
                    )
                )
            }
        }
    }

    private fun toggleExpandedDate(date: LocalDate) {
        _expandedDates.update { expanded ->
            val currentExpanded = if (expanded.isEmpty()) {
                uiState.value.expandedDates
            } else {
                expanded
            }
            if (currentExpanded.contains(date)) currentExpanded - date else currentExpanded + date
        }
    }

    private fun calculateHistoryUiState(
        transactions: List<Transaction>,
        budgetSettings: BudgetSettings?,
        currentPeriodStartedAtMillis: Long,
        currentPeriodId: Long,
        userSettings: UserSettings?,
        categories: List<Category>,
        paidOccurrences: Set<PaidRecurrentOccurrence>,
        inputs: UIInputs
    ): HistoryUiState {
        val displayTx = buildDisplayTransactions(transactions, inputs.pendingRemovedTransactions)

        val startDate = budgetSettings?.startDate ?: LocalDate.now().minusDays(30)
        val endDate = budgetSettings?.getPeriodEndDate() ?: LocalDate.now()
        val today = LocalDate.now()
        val previousPeriodId = currentPeriodId - 1

        val (currentPeriodTx, pastPeriodTx) = splitPeriodTransactions(
            transactions = displayTx,
            budgetStartDate = startDate,
            budgetEndDate = endDate,
            currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
            currentPeriodId = currentPeriodId,
            previousPeriodId = previousPeriodId,
        )

        val budgetState = budgetSettings?.let { s ->
            val periodTransactions = budgetStateCalculator.filterPeriodTransactions(
                transactions = transactions,
                settings = s,
                currentPeriodId = currentPeriodId,
                currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
            )
            budgetStateCalculator.calculateBudgetState(s, periodTransactions, today, paidOccurrences)
        }

        val (upcomingInPeriod, futureOutOfPeriod) = buildUpcomingRecurrentItems(
            transactions = displayTx,
            budgetStartDate = startDate,
            budgetEndDate = endDate,
            today = today,
            paidOccurrences = paidOccurrences,
        )

        val groupedCurrent = buildGroupedCurrentTransactions(
            currentPeriodTransactions = currentPeriodTx,
            displayTransactions = displayTx,
            budgetStartDate = startDate,
            budgetEndDate = endDate,
            today = today,
            paidOccurrences = paidOccurrences,
        )

        val groupedPast = if (userSettings?.showPastTransactions == false) {
            emptyMap()
        } else {
            groupTransactionsByDate(pastPeriodTx)
        }

        val creditOwed = transactions.filter { it.isCredit && !it.isDeleted && !it.isCreditPaid }
            .sumOf { it.amount }
        val remainingBudget = budgetState?.remainingToday ?: BigDecimal.ZERO
        val debtAdjustedBalance = remainingBudget.subtract(creditOwed)

        // Auto-expand first date group on initial load
        val autoExpanded = if (inputs.expandedDates.isEmpty()) {
            groupedCurrent.keys.filterNotNull().sortedDescending().take(1).toSet()
        } else {
            inputs.expandedDates
        }

        return HistoryUiState(
            budgetSettings = budgetSettings,
            budgetState = budgetState,
            currentPeriodId = currentPeriodId,
            currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
            isCreditQuickToggleEnabled = userSettings?.isCreditQuickToggleEnabled ?: false,
            showPastTransactionsSetting = userSettings?.showPastTransactions ?: true,
            tags = categories.map { it.name },
            transactions = transactions,
            editingTransaction = inputs.editingTransaction,
            pendingRemovedTransactions = inputs.pendingRemovedTransactions,
            recurrentToDelete = inputs.recurrentToDelete,
            recurrentToEdit = inputs.recurrentToEdit,
            showDeleteRecurrentDialog = inputs.showDeleteRecurrentDialog,
            expandedTransactionId = inputs.expandedTransactionId,
            expandedDates = autoExpanded,
            showPastPeriod = inputs.showPastPeriod,
            showOutOfPeriodSubscriptions = inputs.showOutOfPeriodSubscriptions,
            showUpcomingRecurrentInPeriod = inputs.showUpcomingRecurrentInPeriod,
            lockSwipeable = inputs.lockSwipeable,
            recurrentPaymentsViewMode = userSettings?.recurrentPaymentsViewMode
                ?: RecurrentPaymentsViewMode.VERTICAL_LIST,
            displayTransactions = displayTx,
            groupedCurrentTransactions = groupedCurrent,
            groupedPastTransactions = groupedPast,
            upcomingRecurrentInPeriod = upcomingInPeriod,
            futureRecurrentOutOfPeriod = futureOutOfPeriod,
            creditOwed = creditOwed,
            debtAdjustedBalance = debtAdjustedBalance,
        )
    }

    private data class UIInputs(
        val expandedDates: Set<LocalDate>,
        val editingTransaction: Transaction?,
        val recurrentToDelete: Transaction?,
        val recurrentToEdit: Transaction?,
        val showDeleteRecurrentDialog: Boolean,
        val showPastPeriod: Boolean,
        val showOutOfPeriodSubscriptions: Boolean,
        val showUpcomingRecurrentInPeriod: Boolean,
        val lockSwipeable: Boolean,
        val expandedTransactionId: Long?,
        val pendingRemovedTransactions: Map<Long, Transaction>
    )

    companion object {
        private const val EXIT_ANIMATION_DURATION_MS = 600L
    }

    override fun onCleared() {
        super.onCleared()
        autoDismissJob?.cancel()
    }
}
