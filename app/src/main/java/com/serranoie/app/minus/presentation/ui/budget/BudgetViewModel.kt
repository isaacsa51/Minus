package com.serranoie.app.minus.presentation.ui.budget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.R
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.CreditCard
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.calculatePaymentDueDate
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
import com.serranoie.app.minus.presentation.ui.budget.controller.EditorLocalState
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
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
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
    @ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val notificationHelper: NotificationHelper,
    private val notificationScheduler: NotificationScheduler,
    private val transactionHandler: BudgetTransactionHandler,
    private val budgetStateCalculator: BudgetStateCalculator,
    private val budgetWidgetUpdater: BudgetWidgetUpdater,
    private val budgetExpressionEvaluator: BudgetExpressionEvaluator,
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

    private val _effects = MutableSharedFlow<BudgetUiEffect>()
    val effects: SharedFlow<BudgetUiEffect> = _effects.asSharedFlow()

    private val _pendingPeriodBoundaryOverride = MutableStateFlow<Pair<Long, Long>?>(null)

    val uiState: StateFlow<BudgetUiState> = combine(
        budgetRepository.getBudgetSettings(),
        budgetRepository.getTransactions(),
        buildPeriodBoundaryFlow(),
        budgetRepository.getQueuedTransactions(),
        observeCurrentPeriodRolloverUseCase(),
        numpadController.input,
        numpadController.isCalculation,
        numpadController.dragProgress,
        editorStateController.state,
        budgetRepository.getActiveCategories(),
        budgetRepository.getPaidRecurrentOccurrences()
    ) { params ->
        val settings = params[0] as BudgetSettings?
        val transactions = params[1] as List<Transaction>
        val (currentPeriodStartedAtMillis, currentPeriodId) = params[2] as Pair<Long, Long>
        val queuedTransactions = params[3] as List<Transaction>
        val (rolloverAmount, rolloverCarryForward) = params[4] as Pair<BigDecimal, Boolean>
        val numpadInput = params[5] as String
        val isCalculation = params[6] as Boolean
        val dragProgress = params[7] as Float
        val editorState = params[8] as EditorLocalState
        val categories = params[9] as List<Category>
        @Suppress("UNCHECKED_CAST")
        val paidOccurrences = params[10] as Set<PaidRecurrentOccurrence>

        val settingsWithRollover = settings?.copy(
            rollOverLimit = if (rolloverAmount > BigDecimal.ZERO) rolloverAmount else null,
            rollOverCarryForward = rolloverCarryForward,
        )

        val budgetState = settingsWithRollover?.let { s ->
            val periodTransactions = budgetStateCalculator.filterPeriodTransactions(
                transactions = transactions,
                settings = s,
                currentPeriodId = currentPeriodId,
                currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
            )
            budgetStateCalculator.calculateBudgetState(s, periodTransactions, LocalDate.now(), paidOccurrences)
        }

        val creditOwed = transactions.filter { it.isCredit && !it.isDeleted && !it.isCreditPaid }.sumOf { it.amount }
        val remainingBudget = budgetState?.remainingToday ?: BigDecimal.ZERO
        val debtAdjustedBalance = remainingBudget.subtract(creditOwed)

        BudgetUiState(
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
            tags = categories.map { it.name },
            isFirstLaunch = settings == null,
            isRecurrentEnabled = editorState.isRecurrentEnabled,
            isCreditEnabled = editorState.isCreditEnabled,
            showRecurrentDialog = editorState.showRecurrentDialog,
            showCreditCutoffDialog = editorState.showCreditCutoffDialog,
            pendingRecurrentAmount = editorState.pendingRecurrentAmount,
            pendingRecurrentComment = editorState.pendingRecurrentComment,
            currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
            currentPeriodId = currentPeriodId,
            isCalculation = isCalculation,
            dragProgress = dragProgress,
            lockSwipeable = editorState.lockSwipeable,
            lockDraggable = editorState.lockDraggable,
            pendingExpensesForNextPeriod = queuedTransactions,
            creditOwed = creditOwed,
            debtAdjustedBalance = debtAdjustedBalance,
            calculationPreview = calculateCalculationPreview(numpadInput, settings?.currencyCode ?: "USD"),
            numpadDraftAmount = parseNumpadDraftAmount(numpadInput),
        )
    }.catch { error ->
        logcat(TAG) { "Error in uiState pipeline: ${error.asLog()}" }
        emit(
            BudgetUiState(
                isLoading = false,
                error = error.message ?: "Unknown error",
                isFirstLaunch = true
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = BudgetUiState.INITIAL
    )

    init {
        viewModelScope.launch {
            uiState.collect { baseState ->
                budgetWidgetUpdater.update(baseState)
            }
        }
    }

    private fun buildPeriodBoundaryFlow() = observeCurrentPeriodBoundaryUseCase().map { boundary ->
        _pendingPeriodBoundaryOverride.value ?: boundary
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
                    comment = "Test expense",
                    currency = currency
                )
            } catch (e: Exception) {
                logcat(TAG) { e.asLog() }
            }

            try {
                val cutoffDay = settings?.creditCardCutoffDay
                if (cutoffDay != null) {
                    val today = LocalDate.now()
                    val card = CreditCard(cutoffDay = cutoffDay)
                    val dueDate = calculatePaymentDueDate(card, today)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
                    notificationHelper.showCreditCutoffNotification(
                        totalAmount = "123.45",
                        dueDateText = dueDate.format(formatter),
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
        numpadController.process(
            intent = controllerIntent,
            currentIsCalculation = uiState.value.isCalculation,
        )

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
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.SetAnimState -> editorStateController.process(
                EditorIntent.SetAnimState(intent.state),
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.CommentUpdated -> editorStateController.process(
                EditorIntent.CommentUpdated(intent.comment),
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.SetRecurrentEnabled -> editorStateController.process(
                EditorIntent.SetRecurrentEnabled(intent.enabled),
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.SetCreditEnabled -> editorStateController.process(
                EditorIntent.SetCreditEnabled(intent.enabled),
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.DismissRecurrentDialog -> editorStateController.process(
                EditorIntent.DismissRecurrentDialog,
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.DismissCreditCutoffDialog -> editorStateController.process(
                EditorIntent.DismissCreditCutoffDialog,
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.DateSelected -> editorStateController.process(
                EditorIntent.DateSelected(intent.date),
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetEditorIntent.UpdateSettings -> handleUpdateSettings(intent.settings)
            is BudgetEditorIntent.DeleteTag -> handleDeleteTag(intent.tag)
            is BudgetEditorIntent.RecurrentExpenseApplied -> handleRecurrentExpenseApply(
                intent.frequency,
                intent.endDate,
                intent.subscriptionDay,
                intent.fallbackComment,
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
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )

            is BudgetSystemIntent.SetLockDraggable -> editorStateController.process(
                EditorIntent.SetLockDraggable(intent.locked),
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )
        }
    }

    private fun handleApply() {
        viewModelScope.launch {
            val actions = transactionActionsController.apply(
                input = numpadController.input.value,
                isCalculation = numpadController.isCalculation.value,
                isRecurrentEnabled = uiState.value.isRecurrentEnabled,
                isCreditEnabled = uiState.value.isCreditEnabled,
                comment = uiState.value.currentComment,
                budgetSettings = uiState.value.budgetSettings,
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
        fallbackComment: String,
    ) {
        viewModelScope.launch {
            val actions = transactionActionsController.applyRecurrent(
                frequency = frequency,
                endDate = endDate,
                subscriptionDay = subscriptionDay,
                pendingAmount = uiState.value.pendingRecurrentAmount,
                pendingComment = uiState.value.pendingRecurrentComment,
                resolveActivePeriodId = ::resolveActivePeriodId,
                isCredit = uiState.value.isCreditEnabled,
                fallbackComment = fallbackComment,
            )
            if (actions.isNotEmpty()) {
                editorStateController.applyRecurrentDialog()
                numpadController.setInput("")
                numpadController.process(
                    NumpadIntent.SetCalculationMode(false),
                    uiState.value.isCalculation
                )
            }
        }
    }

    private fun handleCreditCutoffDayConfirmed(cutoffDay: Int) {
        logcat(TAG) { "handleCreditCutoffDayConfirmed: day=$cutoffDay" }
        if (cutoffDay !in 1..31) {
            logcat(TAG) { "handleCreditCutoffDayConfirmed: INVALID day=$cutoffDay" }
            return
        }

        editorStateController.applyCreditCutoffDay()

        val currentSettings = uiState.value.budgetSettings
        if (currentSettings == null) {
            logcat(TAG) { "handleCreditCutoffDayConfirmed: currentSettings is NULL, skipping persistence" }
            return
        }

        viewModelScope.launch {
            logcat(TAG) { "handleCreditCutoffDayConfirmed: launching persistence for day=$cutoffDay" }
            persistBudgetSettings(
                currentSettings.copy(creditCardCutoffDay = cutoffDay),
                forceNewPeriodBoundary = false,
            )
        }
    }

    private fun handleFinishBudgetEarly() {
        finishBudgetEarly()
    }

    private fun handleDeleteTag(tag: String) {
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
                    numpadController.process(
                        NumpadIntent.SetCalculationMode(false),
                        uiState.value.isCalculation
                    )
                    editorStateController.process(
                        EditorIntent.SetCreditEnabled(false),
                        hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null
                    )
                }

                TransactionAction.TransactionAdded -> Unit
                TransactionAction.TransactionQueuedForNextPeriod -> Unit

                is TransactionAction.OpenRecurrentDialog -> {
                    editorStateController.showRecurrentDialog(
                        amount = action.amount,
                        comment = action.comment,
                    )
                    numpadController.setInput(action.normalizedInput)
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
            numpadController.setInput("")
            numpadController.process(
                NumpadIntent.SetCalculationMode(false),
                uiState.value.isCalculation
            )
            editorStateController.process(
                EditorIntent.CommentUpdated(""),
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null,
            )
            editorStateController.process(
                EditorIntent.SetCreditEnabled(false),
                hasCreditCardCutoffDay = uiState.value.budgetSettings?.creditCardCutoffDay != null
            )
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

        yield()

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

        return uiState.value.currentPeriodId
    }

    private fun calculateCalculationPreview(input: String, currencyCode: String): String? {
        if (input.isEmpty()) return null
        val startsWithPlus = input.startsWith("+")
        val startsWithMinus = input.startsWith("-")
        if (!startsWithPlus && !startsWithMinus) return null

        val result = budgetExpressionEvaluator.evaluate(input) ?: return null

        val amount = try {
            BigDecimal(result).abs()
        } catch (_: Exception) {
            return null
        }
        val formattedAmount = symbolOnlyCurrencyFormat(currencyCode).format(amount)

        return if (startsWithPlus) {
            context.getString(R.string.budget_pill_calc_added, formattedAmount)
        } else {
            context.getString(R.string.budget_pill_calc_subtracted, formattedAmount)
        }
    }

    private fun parseNumpadDraftAmount(input: String): BigDecimal? {
        val evaluated = budgetExpressionEvaluator.evaluate(input) ?: return null
        val magnitude = evaluated.toBigDecimalOrNull()?.abs()?.takeIf { it.signum() != 0 } ?: return null
        return if (input.startsWith("+")) magnitude.negate() else magnitude
    }

    private fun validateNumpadInput(input: String): Boolean {
        if (input.isEmpty()) return false
        if (input == ".") return false
        return try {
            val value = BigDecimal(input)
            value.compareTo(BigDecimal.ZERO) != 0
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
        fallbackComment: String,
    ): Boolean = delegate.applyRecurrentExpense(
        pendingAmount = pendingAmount,
        pendingComment = pendingComment,
        frequency = frequency,
        endDate = endDate,
        subscriptionDay = subscriptionDay,
        resolveActivePeriodId = this.resolveActivePeriodId,
        isCredit = isCredit,
        fallbackComment = fallbackComment,
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
