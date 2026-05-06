package com.serranoie.app.minus.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
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
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiEffect
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiIntent
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetEditorIntent
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetSystemIntent
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.editor.AnimState
import com.serranoie.app.minus.presentation.editor.EditMode
import com.serranoie.app.minus.presentation.notification.NotificationHelper
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
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

	private val _uiState = MutableStateFlow(BudgetUiState.INITIAL)

	val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

	private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
	val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

	private val _budgetSettings = MutableStateFlow<BudgetSettings?>(null)
	val budgetSettings: StateFlow<BudgetSettings?> = _budgetSettings.asStateFlow()

	private val _budgetState = MutableStateFlow<BudgetState?>(null)
	val budgetState: StateFlow<BudgetState?> = _budgetState.asStateFlow()

	private val _categories =
		MutableStateFlow<List<com.serranoie.app.minus.domain.model.Category>>(emptyList())
	val categories: StateFlow<List<com.serranoie.app.minus.domain.model.Category>> =
		_categories.asStateFlow()

	private val _effects = MutableSharedFlow<BudgetUiEffect>()
	val effects: SharedFlow<BudgetUiEffect> = _effects.asSharedFlow()

	private val _numpadInput = MutableStateFlow("")
	private val _currentComment = MutableStateFlow("")
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
			combine(_numpadInput, _currentComment) { numpadInput, currentComment ->
				numpadInput to currentComment
			}.collect { (numpadInput, currentComment) ->
				_uiState.update {
					it.copy(
						numpadInput = numpadInput,
						isNumpadValid = validateNumpadInput(numpadInput),
						animState = if (numpadInput.isNotEmpty()) AnimState.EDITING else AnimState.IDLE,
						currentComment = currentComment
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

		return BudgetUiState(
			isLoading = false,
			budgetSettings = settingsWithRollover,
			budgetState = budgetState,
			transactions = transactions,
			selectedDate = LocalDate.now(),
			error = null,
			numpadInput = _numpadInput.value,
			isNumpadValid = validateNumpadInput(_numpadInput.value),
			editMode = _uiState.value.editMode,
			animState = if (_numpadInput.value.isNotEmpty()) AnimState.EDITING else AnimState.IDLE,
			currentComment = _currentComment.value,
			tags = _categories.value.map { it.name },
			isFirstLaunch = settings == null,
			isCreditEnabled = _uiState.value.isCreditEnabled,
			showCreditCutoffDialog = _uiState.value.showCreditCutoffDialog,
			pendingExpensesForNextPeriod = queuedTransactions,
			currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
			currentPeriodId = currentPeriodId,
			isCalculation = _uiState.value.isCalculation,
			dragProgress = _uiState.value.dragProgress,
			lockSwipeable = _uiState.value.lockSwipeable,
			lockDraggable = _uiState.value.lockDraggable,
		)
	}

	private suspend fun applyBaseState(baseState: BudgetUiState) {
		_uiState.update { current ->
			baseState.copy(
				numpadInput = _numpadInput.value,
				isNumpadValid = validateNumpadInput(_numpadInput.value),
				animState = if (_numpadInput.value.isNotEmpty()) AnimState.EDITING else AnimState.IDLE,
				currentComment = _currentComment.value,
				isCalculation = current.isCalculation,
				dragProgress = current.dragProgress,
				lockSwipeable = current.lockSwipeable,
				lockDraggable = current.lockDraggable,
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
		viewModelScope.launch {
			persistBudgetSettings(settings, forceNewPeriodBoundary = forceNewPeriodBoundary)
		}
	}

	fun updatePeriodEndNotificationTime(hour: Int, minute: Int) {
		viewModelScope.launch {
			updatePeriodEndNotificationTimeUseCase(hour, minute)
			logcat(TAG) { "Updated period end notification time to %02d:%02d".format(hour, minute) }
		}
	}

	fun finishBudgetEarly() {
		viewModelScope.launch {
			finishBudgetEarlyUseCase()
		}
	}

	fun clearEarlyFinishState() {
		viewModelScope.launch {
			clearEarlyFinishStateUseCase()
		}
	}

	fun markFirstLaunchComplete() {
		_uiState.update { it.copy(isFirstLaunch = false) }
		viewModelScope.launch {
			markOnboardingCompletedUseCase()
		}
	}


	fun triggerTestNotifications() {
		viewModelScope.launch {
			val settings = budgetRepository.getBudgetSettingsSync()
			val currency = settings?.currencyCode ?: "USD"

			try {
				notificationHelper.showPeriodEndNotification(
					remainingBudget = "150.00", currency = currency
				)
			} catch (e: Exception) {
				logcat(TAG) { e.asLog() }
			}

			try {
				notificationHelper.showRecurrentExpenseNotification(
					amount = "50.00",
					comment = "Test expense",
					frequency = "MONTHLY",
					currency = currency
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
		when (intent) {
			is BudgetNumpadIntent.NumberTapped -> handleNumberInput(intent.digit)
			is BudgetNumpadIntent.DotTapped -> handleDotInput()
			is BudgetNumpadIntent.BackspaceTapped -> handleBackspace()
			is BudgetNumpadIntent.ApplyTapped -> handleApply()
			is BudgetNumpadIntent.ResetInputTapped -> handleResetInput()
			is BudgetNumpadIntent.OperatorTapped -> handleOperatorInput(intent.operator)
			is BudgetNumpadIntent.EqualsTapped -> handleEqualsInput()
			is BudgetNumpadIntent.SetCalculationMode -> handleSetCalculationMode(intent.enabled)
			is BudgetNumpadIntent.SetDragProgress -> handleDragProgress(intent.progress)
			is BudgetNumpadIntent.TriggerTestNotifications -> triggerTestNotifications()
		}
	}

	private fun handleTransactionIntent(intent: BudgetTransactionIntent) {
		when (intent) {
			is BudgetTransactionIntent.DeleteTransactionTapped -> handleDeleteTransaction(intent.transaction)
			is BudgetTransactionIntent.RestoreTransactionTapped -> handleRestoreTransaction(intent.transaction)
			is BudgetTransactionIntent.EditTransactionTapped -> handleEditTransaction(intent.updatedTransaction)
		}
	}

	private fun handleEditorIntent(intent: BudgetEditorIntent) {
		when (intent) {
			is BudgetEditorIntent.DateSelected -> handleDateSelected(intent.date)
			is BudgetEditorIntent.UpdateSettings -> handleUpdateSettings(intent.settings)
			is BudgetEditorIntent.SetEditMode -> handleSetEditMode(intent.mode)
			is BudgetEditorIntent.SetAnimState -> handleSetAnimState(intent.state)
			is BudgetEditorIntent.CommentUpdated -> handleCommentUpdate(intent.comment)
			is BudgetEditorIntent.DeleteTag -> handleDeleteTag(intent.tag)
			is BudgetEditorIntent.SetRecurrentEnabled -> handleSetRecurrentEnabled(intent.enabled)
			is BudgetEditorIntent.SetCreditEnabled -> handleSetCreditEnabled(intent.enabled)
			is BudgetEditorIntent.DismissRecurrentDialog -> handleDismissRecurrentDialog()
			is BudgetEditorIntent.DismissCreditCutoffDialog -> handleDismissCreditCutoffDialog()
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
			is BudgetSystemIntent.SetLockSwipeable -> handleSetLockSwipeable(intent.locked)
			is BudgetSystemIntent.SetLockDraggable -> handleSetLockDraggable(intent.locked)
		}
	}


	private fun handleNumberInput(digit: String) {
		val currentInput = _numpadInput.value

		_numpadInput.value = currentInput + digit
	}

	private fun handleDotInput() {
		val currentInput = _numpadInput.value

		val lastChar = currentInput.lastOrNull()
		val isOperator = { c: Char? -> c != null && c in "+-×÷" }

		if (currentInput.isEmpty() || isOperator(lastChar)) {
			_numpadInput.value = currentInput + "0."
			return
		}

		val lastOperatorIndex = currentInput.indexOfLast { it in "+-×÷" }
		val currentSegment = currentInput.substring(lastOperatorIndex + 1)
		if (currentSegment.contains(".")) return

		_numpadInput.value = "$currentInput."
	}

	private fun handleBackspace() {
		val updatedInput = _numpadInput.value.dropLast(1)
		_numpadInput.value = updatedInput
		if (updatedInput.isEmpty() && _uiState.value.isCalculation) {
			_uiState.update { it.copy(isCalculation = false) }
		}
	}

	private fun handleOperatorInput(operator: Char) {
		val currentInput = _numpadInput.value
		if (currentInput.isEmpty()) return

		val lastChar = currentInput.lastOrNull() ?: return
		if (lastChar in "+-×÷" || lastChar == '.') return

		_numpadInput.value = "$currentInput$operator"
		if (!_uiState.value.isCalculation) {
			_uiState.update { it.copy(isCalculation = true) }
		}
	}

	private fun handleEqualsInput() {
		val currentInput = _numpadInput.value
		if (currentInput.isEmpty()) return

		val result = budgetExpressionEvaluator.evaluate(currentInput)
		if (result != null) {
			_numpadInput.value = result
			if (!_uiState.value.isCalculation) {
				_uiState.update { it.copy(isCalculation = true) }
			}
		}
	}


	private fun handleSetCalculationMode(enabled: Boolean) {
		_uiState.update { it.copy(isCalculation = enabled, dragProgress = 0f) }
	}

	private fun handleDragProgress(progress: Float) {
		_uiState.update { it.copy(dragProgress = progress) }
	}

	private fun handleSetLockSwipeable(locked: Boolean) {
		_uiState.update { it.copy(lockSwipeable = locked) }
	}

	private fun handleSetLockDraggable(locked: Boolean) {
		_uiState.update { it.copy(lockDraggable = locked) }
	}

	private fun handleApply() {
		viewModelScope.launch {
			when (val result = transactionHandler.applyTransaction(
				input = _numpadInput.value,
				isCalculation = _uiState.value.isCalculation,
				isRecurrentEnabled = _uiState.value.isRecurrentEnabled,
				isCreditEnabled = _uiState.value.isCreditEnabled,
				comment = _currentComment.value,
				budgetSettings = _uiState.value.budgetSettings,
				resolveActivePeriodId = ::resolveActivePeriodId,
			)) {
				is ApplyTransactionResult.InvalidInput -> Unit
				is ApplyTransactionResult.ShowRecurrentDialog -> {
					_numpadInput.value = result.normalizedInput
					_uiState.update {
						it.copy(
							showRecurrentDialog = true,
							pendingRecurrentAmount = result.amount,
							pendingRecurrentComment = _currentComment.value,
						)
					}
				}

				is ApplyTransactionResult.QueuedForNextPeriod -> {
					_numpadInput.value = ""
					_currentComment.value = ""
					_uiState.update { it.copy(isCalculation = false, isCreditEnabled = false) }
					_effects.emit(BudgetUiEffect.ShowMessage("Gasto en cola para el proximo periodo"))
				}

				is ApplyTransactionResult.Added -> {
					_numpadInput.value = ""
					_currentComment.value = ""
					_uiState.update { it.copy(isCalculation = false, isCreditEnabled = false) }
				}
			}
		}
	}

	private fun handleSetRecurrentEnabled(enabled: Boolean) {
		_uiState.update { it.copy(isRecurrentEnabled = enabled) }
	}

	private fun handleSetCreditEnabled(enabled: Boolean) {
		if (!enabled) {
			_uiState.update { it.copy(isCreditEnabled = false, showCreditCutoffDialog = false) }
			return
		}

		val hasCutoff = _uiState.value.budgetSettings?.creditCardCutoffDay != null
		_uiState.update {
			it.copy(
				isCreditEnabled = enabled,
				showCreditCutoffDialog = !hasCutoff
			)
		}
	}

	private fun handleDismissRecurrentDialog() {
		_uiState.update {
			it.copy(
				showRecurrentDialog = false,
				pendingRecurrentAmount = null,
				pendingRecurrentComment = ""
			)
		}
	}

	private fun handleDismissCreditCutoffDialog() {
		_uiState.update { it.copy(showCreditCutoffDialog = false, isCreditEnabled = false) }
	}

	private fun handleRecurrentExpenseApply(
		frequency: RecurrentFrequency, endDate: LocalDate, subscriptionDay: Int?
	) {
		viewModelScope.launch {
			val applied = transactionHandler.applyRecurrentExpense(
				pendingAmount = _uiState.value.pendingRecurrentAmount,
				pendingComment = _uiState.value.pendingRecurrentComment,
				frequency = frequency,
				endDate = endDate,
				subscriptionDay = subscriptionDay,
				resolveActivePeriodId = ::resolveActivePeriodId,
				isCredit = _uiState.value.isCreditEnabled,
			)
			if (!applied) return@launch

			_uiState.update {
				it.copy(
					showRecurrentDialog = false,
					pendingRecurrentAmount = null,
					pendingRecurrentComment = "",
					isRecurrentEnabled = false,
					isCreditEnabled = false,
				)
			}
			_numpadInput.value = ""
			_currentComment.value = ""
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
			_uiState.update { it.copy(showCreditCutoffDialog = false, isCreditEnabled = true) }
		}
	}

	private fun handleFinishBudgetEarly() {
		finishBudgetEarly()
	}

	private fun handleDeleteTransaction(transaction: Transaction) {
		logcat(TAG) { "handleDeleteTransaction called for transaction ${transaction.id}" }
		viewModelScope.launch {
			transactionHandler.deleteTransaction(transaction).onSuccess {
					logcat(TAG) { "Transaction ${transaction.id} deleted successfully" }
				}.onFailure { error ->
					logcat(TAG) { error.asLog() }
					_effects.emit(BudgetUiEffect.ShowMessage("Could not delete transaction"))
				}
		}
	}

	private fun handleRestoreTransaction(transaction: Transaction) {
		viewModelScope.launch {
			transactionHandler.restoreTransaction(transaction).onSuccess {
					logcat(TAG) { "Transaction ${transaction.id} restored successfully" }
				}.onFailure { error ->
					logcat(TAG) { error.asLog() }
					_effects.emit(BudgetUiEffect.ShowMessage("Could not restore transaction"))
				}
		}
	}

	private fun handleEditTransaction(updatedTransaction: Transaction) {
		viewModelScope.launch {
			transactionHandler.editTransaction(updatedTransaction)
		}
	}

	private fun handleDateSelected(date: LocalDate) {
		_uiState.update { it.copy(selectedDate = date) }
	}

	private fun handleUpdateSettings(settings: BudgetSettings) {
		logcat(TAG) { "handleUpdateSettings called with settings=$settings" }
		viewModelScope.launch {
			persistBudgetSettings(settings)
		}
	}

	private fun handleResetInput() {
		_numpadInput.value = ""
		if (_uiState.value.isCalculation) {
			_uiState.update { it.copy(isCalculation = false) }
		}
	}

	private fun handleSetEditMode(mode: EditMode) {
		_uiState.update { it.copy(editMode = mode) }
	}

	private fun handleSetAnimState(state: AnimState) {
		_uiState.update { it.copy(animState = state) }
	}

	private fun handleCommentUpdate(comment: String) {
		_currentComment.value = comment
	}

	private fun handleDeleteTag(tag: String) {
		_categories.update { categories ->
			categories.filterNot { it.name == tag }
		}
		_uiState.update { state ->
			state.copy(tags = state.tags.filterNot { it == tag })
		}
		viewModelScope.launch {
			budgetRepository.hideCategory(tag)
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
