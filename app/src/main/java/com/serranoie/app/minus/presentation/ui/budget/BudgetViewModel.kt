package com.serranoie.app.minus.presentation.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.FinishBudgetEarlyUseCase
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.domain.usecase.MarkOnboardingCompletedUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodRolloverUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.domain.usecase.UpdatePeriodEndNotificationTimeUseCase
import com.serranoie.app.minus.presentation.notification.NotificationHelper
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.ui.budget.controller.EditorIntent
import com.serranoie.app.minus.presentation.ui.budget.controller.EditorStateController
import com.serranoie.app.minus.presentation.ui.budget.controller.NumpadController
import com.serranoie.app.minus.presentation.ui.budget.controller.NumpadIntent
import com.serranoie.app.minus.presentation.ui.budget.controller.PeriodActions
import com.serranoie.app.minus.presentation.ui.budget.controller.PeriodActionsController
import com.serranoie.app.minus.presentation.ui.budget.controller.PeriodActionsController.PeriodAction
import com.serranoie.app.minus.presentation.ui.budget.controller.TransactionActionsController
import com.serranoie.app.minus.presentation.ui.budget.controller.TransactionActionsController.TransactionAction
import com.serranoie.app.minus.presentation.ui.budget.controller.TransactionHandler
import com.serranoie.app.minus.presentation.ui.budget.mvi.BudgetUiEffect
import com.serranoie.app.minus.presentation.ui.budget.mvi.BudgetUiIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetEditorIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetSystemIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

private const val TAG = "BudgetViewModel - ISAAC"

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val notificationHelper: NotificationHelper,
    private val notificationScheduler: NotificationScheduler,
    private val transactionHandler: BudgetTransactionHandler,
    private val budgetStateCalculator: BudgetStateCalculator,
    private val budgetWidgetUpdater: BudgetWidgetUpdater,
    budgetExpressionEvaluator: BudgetExpressionEvaluator,
    private val observeCurrentPeriodBoundaryUseCase: ObserveCurrentPeriodBoundaryUseCase,
    private val observeCurrentPeriodRolloverUseCase: ObserveCurrentPeriodRolloverUseCase,
    private val getCurrentPeriodIdUseCase: GetCurrentPeriodIdUseCase,
    private val persistBudgetSettingsUseCase: PersistBudgetSettingsUseCase,
    private val updatePeriodEndNotificationTimeUseCase: UpdatePeriodEndNotificationTimeUseCase,
    private val finishBudgetEarlyUseCase: FinishBudgetEarlyUseCase,
    private val clearEarlyFinishStateUseCase: ClearEarlyFinishStateUseCase,
    private val markOnboardingCompletedUseCase: MarkOnboardingCompletedUseCase,
) : ViewModel() {

    private val numpadController = NumpadController(budgetExpressionEvaluator)

    private val editorStateController = EditorStateController()

    private val transactionActionsController = TransactionActionsController(
        handler = TransactionHandlerImpl(
            delegate = transactionHandler,
            resolveActivePeriodId = ::resolveActivePeriodId,
        ),
    )

    private val periodActionsController = PeriodActionsController(NoopPeriodActions)

    private val _uiState = MutableStateFlow(BudgetUiState.INITIAL)
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _budgetSettings = MutableStateFlow<BudgetSettings?>(null)
    val budgetSettings: StateFlow<BudgetSettings?> = _budgetSettings.asStateFlow()

    private val _budgetState = MutableStateFlow<BudgetState?>(null)
    val budgetState: StateFlow<BudgetState?> = _budgetState.asStateFlow()

    private val _categories =
        MutableStateFlow<List<Category>>(emptyList())

    private val _effects = MutableSharedFlow<BudgetUiEffect>()
    val effects: SharedFlow<BudgetUiEffect> = _effects.asSharedFlow()

    private val _pendingPeriodBoundaryOverride = MutableStateFlow<Pair<Long, Long>?>(null)

    init {
        observeBudgetData()
        observeEditorState()
        observeInitialBudgetCheck()
        observeCategories()
    }

    private fun observeBudgetData() {
        viewModelScope.launch {
            combine(
                budgetRepository.getBudgetSettings(),
                budgetRepository.getTransactions(),
                buildPeriodBoundaryFlow(),
                budgetRepository.getQueuedTransactions(),
                observeCurrentPeriodRolloverUseCase()
            ) { settings, transactions, periodBoundary, queuedTransactions, rolloverInfo ->
                createBaseUiState(
                    settings = settings,
                    transactions = transactions,
                    periodBoundary = periodBoundary,
                    queuedTransactions = queuedTransactions,
                    rolloverAmount = rolloverInfo.first,
                    rolloverCarryForward = rolloverInfo.second,
                )
            }.catch { error ->
                emit(
                    BudgetUiState(
                        isLoading = false,
                        error = error.message ?: "Unknown error",
                        isFirstLaunch = true
                    )
                )
            }.collect { baseState ->
                applyBaseState(baseState)
            }
        }
    }

    private fun observeEditorState() {
        viewModelScope.launch {
            combine(
                numpadController.input,
                editorStateController.state,
            ) { numpadInput, editorState ->
                numpadInput to editorState
            }.collect { (numpadInput, editorState) ->
                _uiState.update {
                    it.copy(
                        numpadInput = numpadInput,
                        isNumpadValid = validateNumpadInput(numpadInput),
                        animState = if (numpadInput.isNotEmpty()) AnimState.EDITING else AnimState.IDLE,
                        currentComment = editorState.currentComment,
                        editMode = editorState.editMode,
                        lockSwipeable = editorState.lockSwipeable,
                        lockDraggable = editorState.lockDraggable,
                        isRecurrentEnabled = editorState.isRecurrentEnabled,
                        isCreditEnabled = editorState.isCreditEnabled,
                        showRecurrentDialog = editorState.showRecurrentDialog,
                        showCreditCutoffDialog = editorState.showCreditCutoffDialog,
                        pendingRecurrentAmount = editorState.pendingRecurrentAmount,
                        pendingRecurrentComment = editorState.pendingRecurrentComment,
                        selectedDate = editorState.selectedDate,
                    )
                }
            }
        }
    }

    private fun observeInitialBudgetCheck() {
        viewModelScope.launch {
            val settings = budgetRepository.getBudgetSettingsSync()
            if (settings == null) {
                _uiState.update { it.copy(isFirstLaunch = true) }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            budgetRepository.getActiveCategories().collect { categories ->
                _categories.value = categories
                _uiState.update { it.copy(tags = categories.map { c -> c.name }) }
            }
        }
    }

    private fun buildPeriodBoundaryFlow() = observeCurrentPeriodBoundaryUseCase().map { boundary ->
        _pendingPeriodBoundaryOverride.value ?: boundary
    }

    private fun createBaseUiState(
        settings: BudgetSettings?,
        transactions: List<Transaction>,
        periodBoundary: Pair<Long, Long>,
        queuedTransactions: List<Transaction>,
        rolloverAmount: BigDecimal,
        rolloverCarryForward: Boolean,
    ): BudgetUiState {
        val currentPeriodStartedAtMillis = periodBoundary.first
        val currentPeriodId = periodBoundary.second
        val settingsWithRollover = settings?.copy(
            rollOverLimit = if (rolloverAmount > BigDecimal.ZERO) rolloverAmount else null,
            rollOverCarryForward = rolloverCarryForward,
        )
        val budgetState = settingsWithRollover?.let { s ->
            val today = LocalDate.now()
            val periodTransactions = budgetStateCalculator.filterPeriodTransactions(
                transactions = transactions,
                settings = s,
                currentPeriodId = currentPeriodId,
                currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
            )
            budgetStateCalculator.calculateBudgetState(s, periodTransactions, today)
        }

        val numpadInput = numpadController.input.value
        val editorState = editorStateController.state.value

        return BudgetUiState(
            isLoading = false,
            budgetSettings = settingsWithRollover,
            budgetState = budgetState,
            transactions = transactions,
            selectedDate = editorState.selectedDate,
            error = null,
            numpadInput = numpadInput,
            isNumpadValid = validateNumpadInput(numpadInput),
            editMode = editorState.editMode,
            animState = if (numpadInput.isNotEmpty()) AnimState.EDITING else AnimState.IDLE,
            currentComment = editorState.currentComment,
            tags = _categories.value.map { it.name },
            isFirstLaunch = settings == null,
            isCreditEnabled = editorState.isCreditEnabled,
            showCreditCutoffDialog = editorState.showCreditCutoffDialog,
            pendingExpensesForNextPeriod = queuedTransactions,
            currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
            currentPeriodId = currentPeriodId,
            isCalculation = numpadController.isCalculation.value,
            dragProgress = numpadController.dragProgress.value,
            lockSwipeable = editorState.lockSwipeable,
            lockDraggable = editorState.lockDraggable,
        )
    }

    private suspend fun applyBaseState(baseState: BudgetUiState) {
        _uiState.update { current ->
            baseState.copy(
                numpadInput = numpadController.input.value,
                isNumpadValid = validateNumpadInput(numpadController.input.value),
                animState = if (numpadController.input.value.isNotEmpty()) AnimState.EDITING else AnimState.IDLE,
                currentComment = editorStateController.state.value.currentComment,
                editMode = editorStateController.state.value.editMode,
                lockSwipeable = editorStateController.state.value.lockSwipeable,
                lockDraggable = editorStateController.state.value.lockDraggable,
                isRecurrentEnabled = editorStateController.state.value.isRecurrentEnabled,
                isCreditEnabled = editorStateController.state.value.isCreditEnabled,
                showRecurrentDialog = editorStateController.state.value.showRecurrentDialog,
                showCreditCutoffDialog = editorStateController.state.value.showCreditCutoffDialog,
                pendingRecurrentAmount = editorStateController.state.value.pendingRecurrentAmount,
                pendingRecurrentComment = editorStateController.state.value.pendingRecurrentComment,
                isCalculation = current.isCalculation,
                dragProgress = current.dragProgress,
                pendingExpensesForNextPeriod = baseState.pendingExpensesForNextPeriod,
            )
        }

        _transactions.value = baseState.transactions
        _budgetSettings.value = baseState.budgetSettings
        _budgetState.value = baseState.budgetState

        budgetWidgetUpdater.update(baseState)
    }

    fun saveBudgetSettings(
        settings: BudgetSettings,
        forceNewPeriodBoundary: Boolean = false,
    ) {
        logcat(TAG) {
            "saveBudgetSettings called: settings=$settings forceNewPeriodBoundary=$forceNewPeriodBoundary"
        }
        val actions = periodActionsController.saveBudgetSettings(settings)
        for (action in actions) {
            when (action) {
                is PeriodAction.PersistBudgetSettings -> viewModelScope.launch {
                    persistBudgetSettings(
                        action.settings,
                        forceNewPeriodBoundary = forceNewPeriodBoundary
                    )
                }

                PeriodAction.MarkOnboardingCompleted -> markFirstLaunchComplete()
                else -> Unit
            }
        }
    }

    fun updatePeriodEndNotificationTime(hour: Int, minute: Int) {
        for (action in periodActionsController.updatePeriodEndNotificationTime(hour, minute)) {
            if (action is PeriodAction.UpdatePeriodEndNotification) {
                viewModelScope.launch {
                    updatePeriodEndNotificationTimeUseCase(action.hour, action.minute)
                    logcat(TAG) {
                        "Updated period end notification time to %02d:%02d".format(
                            action.hour,
                            action.minute
                        )
                    }
                }
            }
        }
    }

    fun updateRecurrentNotificationTime(hour: Int, minute: Int) {
        for (action in periodActionsController.updateRecurrentNotificationTime(hour, minute)) {
            if (action is PeriodAction.UpdateRecurrentNotification) {
                viewModelScope.launch {
                    updatePeriodEndNotificationTimeUseCase.updateRecurrentNotificationTime(
                        action.hour,
                        action.minute
                    )
                    logcat(TAG) {
                        "Updated recurrent notification time to %02d:%02d".format(
                            action.hour,
                            action.minute
                        )
                    }
                }
            }
        }
    }

    fun finishBudgetEarly() {
        for (action in periodActionsController.finishBudgetEarly()) {
            if (action is PeriodAction.FinishBudgetEarly) {
                viewModelScope.launch { finishBudgetEarlyUseCase() }
            }
        }
    }

    fun clearEarlyFinishState() {
        for (action in periodActionsController.clearEarlyFinishState()) {
            if (action is PeriodAction.ClearEarlyFinish) {
                viewModelScope.launch { clearEarlyFinishStateUseCase() }
            }
        }
    }

    fun markFirstLaunchComplete() {
        for (action in periodActionsController.markFirstLaunchComplete()) {
            if (action is PeriodAction.MarkOnboardingCompleted) {
                _uiState.update { it.copy(isFirstLaunch = false) }
                viewModelScope.launch { markOnboardingCompletedUseCase() }
            }
        }
    }

    fun triggerTestNotifications() {
        viewModelScope.launch {
            val settings = budgetRepository.getBudgetSettingsSync()
            val currency = settings?.currencyCode ?: "USD"

            try {
                notificationHelper.showPeriodEndNotification(
                    remainingBudget = "150.00",
                    currency = currency
                )
            } catch (e: Exception) {
                logcat(TAG) { e.asLog() }
            }

            try {
                notificationHelper.showRecurrentExpenseNotification(
                    amount = "50.00",
                    comment = "Test expense"
                )
            } catch (e: Exception) {
                logcat(TAG) { e.asLog() }
            }

            try {
                val cutoffDay = settings?.creditCardCutoffDay
                if (cutoffDay != null) {
                    val today = LocalDate.now()
                    val cutoffDate = runCatching { today.withDayOfMonth(cutoffDay) }
                        .getOrElse { today.withDayOfMonth(today.lengthOfMonth()) }
                    val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
                    notificationHelper.showCreditCutoffNotification(
                        totalAmount = "123.45",
                        cutoffDateText = cutoffDate.format(formatter),
                        currency = currency
                    )
                }
            } catch (e: Exception) {
                logcat(TAG) { e.asLog() }
            }

            try {
                notificationScheduler.runRecurrentExpenseCheckNow()
            } catch (e: Exception) {
                logcat(TAG) { e.asLog() }
            }
        }
    }

    fun processIntent(intent: BudgetUiIntent) {
        when (intent) {
            is BudgetNumpadIntent -> handleNumpadIntent(intent)
            is BudgetTransactionIntent -> handleTransactionIntent(intent)
            is BudgetEditorIntent -> handleEditorIntent(intent)
            is BudgetSystemIntent -> handleSystemIntent(intent)
        }
    }

    private fun handleNumpadIntent(intent: BudgetNumpadIntent) {
        val controllerIntent = when (intent) {
            is BudgetNumpadIntent.NumberTapped -> NumpadIntent.NumberTapped(intent.digit)
            BudgetNumpadIntent.DotTapped -> NumpadIntent.DotTapped
            BudgetNumpadIntent.BackspaceTapped -> NumpadIntent.BackspaceTapped
            BudgetNumpadIntent.ApplyTapped -> NumpadIntent.ApplyTapped
            BudgetNumpadIntent.ResetInputTapped -> NumpadIntent.ResetInputTapped
            is BudgetNumpadIntent.OperatorTapped -> NumpadIntent.OperatorTapped(intent.operator)
            BudgetNumpadIntent.EqualsTapped -> NumpadIntent.EqualsTapped
            is BudgetNumpadIntent.SetCalculationMode -> NumpadIntent.SetCalculationMode(intent.enabled)
            is BudgetNumpadIntent.SetDragProgress -> NumpadIntent.SetDragProgress(intent.progress)
            BudgetNumpadIntent.TriggerTestNotifications -> NumpadIntent.TriggerTestNotifications
        }
        val changes = numpadController.process(
            intent = controllerIntent,
            currentIsCalculation = _uiState.value.isCalculation,
        )
        for (change in changes) {
            when (change) {
                is NumpadController.NumpadChange.InputChanged -> _uiState.update {
                    it.copy(
                        numpadInput = change.newInput,
                        isNumpadValid = validateNumpadInput(change.newInput),
                        animState = if (change.newInput.isNotEmpty()) AnimState.EDITING else AnimState.IDLE,
                    )
                }

                is NumpadController.NumpadChange.CalculationModeChanged -> _uiState.update {
                    it.copy(isCalculation = change.enabled)
                }

                is NumpadController.NumpadChange.DragProgressChanged -> _uiState.update {
                    it.copy(dragProgress = change.progress)
                }
            }
        }

        if (intent is BudgetNumpadIntent.ApplyTapped) handleApply()
        if (intent is BudgetNumpadIntent.TriggerTestNotifications) triggerTestNotifications()
    }

    private fun handleTransactionIntent(intent: BudgetTransactionIntent) {
        when (intent) {
            is BudgetTransactionIntent.DeleteTransactionTapped -> {
                viewModelScope.launch {
                    applyTransactionActions(transactionActionsController.delete(intent.transaction))
                }
            }

            is BudgetTransactionIntent.RestoreTransactionTapped -> {
                viewModelScope.launch {
                    applyTransactionActions(transactionActionsController.restore(intent.transaction))
                }
            }

            is BudgetTransactionIntent.EditTransactionTapped -> {
                viewModelScope.launch {
                    transactionActionsController.edit(intent.updatedTransaction)
                }
            }
        }
    }

    private fun handleEditorIntent(intent: BudgetEditorIntent) {
        when (intent) {
            is BudgetEditorIntent.SetEditMode -> editorStateController.process(
                EditorIntent.SetEditMode(intent.mode),
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.SetAnimState -> editorStateController.process(
                EditorIntent.SetAnimState(intent.state),
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.CommentUpdated -> editorStateController.process(
                EditorIntent.CommentUpdated(intent.comment),
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.SetRecurrentEnabled -> editorStateController.process(
                EditorIntent.SetRecurrentEnabled(intent.enabled),
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.SetCreditEnabled -> editorStateController.process(
                EditorIntent.SetCreditEnabled(intent.enabled),
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.DismissRecurrentDialog -> editorStateController.process(
                EditorIntent.DismissRecurrentDialog,
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.DismissCreditCutoffDialog -> editorStateController.process(
                EditorIntent.DismissCreditCutoffDialog,
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.DateSelected -> editorStateController.process(
                EditorIntent.DateSelected(intent.date),
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.UpdateSettings -> handleUpdateSettings(intent.settings)
            is BudgetEditorIntent.DeleteTag -> handleDeleteTag(intent.tag)
            is BudgetEditorIntent.RecurrentExpenseApplied -> handleRecurrentExpenseApply(
                intent.frequency,
                intent.endDate,
                intent.subscriptionDay,
            )

            is BudgetEditorIntent.CreditCutoffDayConfirmed -> handleCreditCutoffDayConfirmed(intent.cutoffDay)
            is BudgetEditorIntent.FinishBudgetEarly -> handleFinishBudgetEarly()
        }
    }

    private fun handleSystemIntent(intent: BudgetSystemIntent) {
        when (intent) {
            is BudgetSystemIntent.MarkFirstLaunchComplete -> markFirstLaunchComplete()
            is BudgetSystemIntent.SetLockSwipeable -> editorStateController.process(
                EditorIntent.SetLockSwipeable(intent.locked),
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetSystemIntent.SetLockDraggable -> editorStateController.process(
                EditorIntent.SetLockDraggable(intent.locked),
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )
        }
    }

    private fun handleApply() {
        viewModelScope.launch {
            val actions = transactionActionsController.apply(
                input = numpadController.input.value,
                isCalculation = numpadController.isCalculation.value,
                isRecurrentEnabled = editorStateController.state.value.isRecurrentEnabled,
                isCreditEnabled = editorStateController.state.value.isCreditEnabled,
                comment = editorStateController.state.value.currentComment,
                budgetSettings = _uiState.value.budgetSettings,
                resolveActivePeriodId = ::resolveActivePeriodId,
            )
            applyTransactionActions(actions)
        }
    }

    private fun handleUpdateSettings(settings: BudgetSettings) {
        logcat(TAG) { "handleUpdateSettings called with settings=$settings" }
        viewModelScope.launch {
            persistBudgetSettings(settings)
        }
    }

    private fun handleRecurrentExpenseApply(
        frequency: RecurrentFrequency,
        endDate: LocalDate,
        subscriptionDay: Int?,
    ) {
        viewModelScope.launch {
            val actions = transactionActionsController.applyRecurrent(
                frequency = frequency,
                endDate = endDate,
                subscriptionDay = subscriptionDay,
                pendingAmount = editorStateController.state.value.pendingRecurrentAmount,
                pendingComment = editorStateController.state.value.pendingRecurrentComment,
                resolveActivePeriodId = ::resolveActivePeriodId,
                isCredit = editorStateController.state.value.isCreditEnabled,
            )
            if (actions.isNotEmpty()) {
                editorStateController.applyRecurrentDialog()
                _uiState.update {
                    it.copy(
                        numpadInput = "",
                        currentComment = "",
                        isCalculation = false,
                        isRecurrentEnabled = false,
                        isCreditEnabled = false,
                    )
                }
            }
        }
    }

    private fun handleCreditCutoffDayConfirmed(cutoffDay: Int) {
        if (cutoffDay !in 1..31) return
        val currentSettings = _uiState.value.budgetSettings ?: return
        viewModelScope.launch {
            persistBudgetSettings(
                currentSettings.copy(creditCardCutoffDay = cutoffDay),
                forceNewPeriodBoundary = false,
            )
            editorStateController.applyCreditCutoffDay()
            _uiState.update { it.copy(showCreditCutoffDialog = false, isCreditEnabled = true) }
        }
    }

    private fun handleFinishBudgetEarly() {
        finishBudgetEarly()
    }

    private fun handleDeleteTag(tag: String) {
        _categories.update { categories -> categories.filterNot { it.name == tag } }
        _uiState.update { state -> state.copy(tags = state.tags.filterNot { it == tag }) }
        viewModelScope.launch { budgetRepository.hideCategory(tag) }
    }

    private suspend fun applyTransactionActions(actions: List<TransactionAction>) {
        var needClear = false
        for (action in actions) {
            when (action) {
                TransactionAction.ClearInput -> {
                    needClear = true
                }

                TransactionAction.ClearEditorFlags -> {
                    _uiState.update { it.copy(isCalculation = false, isCreditEnabled = false) }
                }

                TransactionAction.TransactionAdded -> {
                    // The transaction list is refreshed by the observation
                    // pipeline, so no explicit refresh is needed.
                }

                TransactionAction.TransactionQueuedForNextPeriod -> {
                    // Same — refreshed by observation.
                }

                is TransactionAction.OpenRecurrentDialog -> {
                    editorStateController.showRecurrentDialog(
                        amount = action.amount,
                        comment = action.comment,
                    )
                    numpadController.clearInput()
                    _uiState.update {
                        it.copy(
                            numpadInput = action.normalizedInput,
                            isNumpadValid = validateNumpadInput(action.normalizedInput),
                        )
                    }
                }

                is TransactionAction.ShowMessage -> {
                    _effects.emit(BudgetUiEffect.ShowMessage(action.message))
                }

                TransactionAction.DeleteFailed -> {
                    _effects.emit(BudgetUiEffect.ShowMessage("Could not delete transaction"))
                }

                TransactionAction.RestoreFailed -> {
                    _effects.emit(BudgetUiEffect.ShowMessage("Could not restore transaction"))
                }
            }
        }
        if (needClear) {
            numpadController.clearInput()
            editorStateController.process(
                EditorIntent.CommentUpdated(""),
                hasCreditCardCutoffDay = _uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )
            _uiState.update {
                it.copy(
                    numpadInput = "",
                    currentComment = "",
                    isCalculation = false,
                    isCreditEnabled = false,
                )
            }
        }
    }

    private suspend fun persistBudgetSettings(
        settings: BudgetSettings,
        forceNewPeriodBoundary: Boolean = false,
    ) {
        logcat(TAG) {
            "persistBudgetSettings START settings=$settings forceNewPeriodBoundary=$forceNewPeriodBoundary"
        }
        val periodBoundary = persistBudgetSettingsUseCase(
            settings = settings,
            forceNewPeriodBoundary = forceNewPeriodBoundary,
        )
        _pendingPeriodBoundaryOverride.value =
            periodBoundary.periodStartMillis to periodBoundary.periodId
        _uiState.update {
            it.copy(
                currentPeriodStartedAtMillis = periodBoundary.periodStartMillis,
                currentPeriodId = periodBoundary.periodId,
            )
        }
        _pendingPeriodBoundaryOverride.value = null
        logcat(TAG) {
            "persistBudgetSettings END periodStartMillis=${periodBoundary.periodStartMillis} periodId=${periodBoundary.periodId}"
        }
    }

    private suspend fun resolveActivePeriodId(): Long {
        _pendingPeriodBoundaryOverride.value?.second?.let { pendingPeriodId ->
            if (pendingPeriodId > 0L) return pendingPeriodId
        }

        val dataStorePeriodId = getCurrentPeriodIdUseCase()
        if (dataStorePeriodId > 0L) return dataStorePeriodId

        return _uiState.value.currentPeriodId
    }

    private fun validateNumpadInput(input: String): Boolean {
        if (input.isEmpty()) return false
        if (input == ".") return false
        return try {
            val value = BigDecimal(input)
            value > BigDecimal.ZERO
        } catch (_: NumberFormatException) {
            false
        }
    }
}

private class TransactionHandlerImpl(
    private val delegate: BudgetTransactionHandler,
    private val resolveActivePeriodId: suspend () -> Long,
) : TransactionHandler {
    override suspend fun apply(
        input: String,
        isCalculation: Boolean,
        isRecurrentEnabled: Boolean,
        isCreditEnabled: Boolean,
        comment: String,
        budgetSettings: BudgetSettings?,
        resolveActivePeriodId: suspend () -> Long,
    ): ApplyTransactionResult = delegate.applyTransaction(
        input = input,
        isCalculation = isCalculation,
        isRecurrentEnabled = isRecurrentEnabled,
        isCreditEnabled = isCreditEnabled,
        comment = comment,
        budgetSettings = budgetSettings,
        resolveActivePeriodId = this.resolveActivePeriodId,
    )

    override suspend fun applyRecurrent(
        pendingAmount: BigDecimal?,
        pendingComment: String,
        frequency: RecurrentFrequency,
        endDate: LocalDate,
        subscriptionDay: Int?,
        resolveActivePeriodId: suspend () -> Long,
        isCredit: Boolean,
    ): Boolean = delegate.applyRecurrentExpense(
        pendingAmount = pendingAmount,
        pendingComment = pendingComment,
        frequency = frequency,
        endDate = endDate,
        subscriptionDay = subscriptionDay,
        resolveActivePeriodId = this.resolveActivePeriodId,
        isCredit = isCredit,
    )

    override suspend fun delete(transaction: Transaction): kotlin.Result<Unit> =
        delegate.deleteTransaction(transaction)

    override suspend fun restore(transaction: Transaction): kotlin.Result<Unit> =
        delegate.restoreTransaction(transaction)

    override suspend fun edit(transaction: Transaction) {
        delegate.editTransaction(transaction)
    }
}

private object NoopPeriodActions : PeriodActions {
    override suspend fun noop() = Unit
}
