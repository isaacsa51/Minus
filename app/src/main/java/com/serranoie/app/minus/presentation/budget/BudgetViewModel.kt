package com.serranoie.app.minus.presentation.budget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.BUDGET_END_DATE_KEY
import com.serranoie.app.minus.CURRENT_PERIOD_ID_KEY
import com.serranoie.app.minus.CURRENT_PERIOD_STARTED_AT_KEY
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_HOUR
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_MINUTE
import com.serranoie.app.minus.EARLY_FINISH_ACTIVE_KEY
import com.serranoie.app.minus.EARLY_FINISH_ACTUAL_DATE_KEY
import com.serranoie.app.minus.EARLY_FINISH_ORIGINAL_END_DATE_KEY
import com.serranoie.app.minus.NOTIFICATION_HOUR_KEY
import com.serranoie.app.minus.NOTIFICATION_MINUTE_KEY
import com.serranoie.app.minus.domain.calculator.RecurringExpenseCalculator
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.domain.usecase.AddTransactionUseCase
import com.serranoie.app.minus.domain.usecase.DeleteTransactionUseCase
import com.serranoie.app.minus.presentation.editor.AnimState
import com.serranoie.app.minus.presentation.editor.EditMode
import com.serranoie.app.minus.presentation.notification.NotificationHelper
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.widget.updateExpenseWidget
import com.serranoie.app.minus.presentation.widget.updateBudgetOverviewWidget
import com.serranoie.app.minus.presentation.widget.updateDaysCountdownWidget
import com.serranoie.app.minus.presentation.widget.updateHeatmapWidget
import com.serranoie.app.minus.presentation.widget.DailySpending
import com.serranoie.app.minus.presentation.widget.MonthHeatmapData
import com.serranoie.app.minus.presentation.widget.updateMonthHeatmapWidget
import com.serranoie.app.minus.settingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiEffect
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiIntent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val TAG = "BudgetViewModel - ISAAC"

@HiltViewModel
class BudgetViewModel @Inject constructor(
	private val budgetRepository: BudgetRepository,
	@ApplicationContext private val context: Context,
	private val notificationScheduler: NotificationScheduler,
	private val notificationHelper: NotificationHelper,
	private val timeProvider: TimeProvider,
	private val addTransactionUseCase: AddTransactionUseCase,
	private val deleteTransactionUseCase: DeleteTransactionUseCase,
	private val recurringExpenseCalculator: RecurringExpenseCalculator
) : ViewModel() {

	private val _uiState = MutableStateFlow(BudgetUiState.INITIAL)

	val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

	private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
	val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

	private val _budgetSettings = MutableStateFlow<BudgetSettings?>(null)
	val budgetSettings: StateFlow<BudgetSettings?> = _budgetSettings.asStateFlow()

	private val _budgetState = MutableStateFlow<BudgetState?>(null)
	val budgetState: StateFlow<BudgetState?> = _budgetState.asStateFlow()

	private val _categories = MutableStateFlow<List<com.serranoie.app.minus.domain.model.Category>>(emptyList())
	val categories: StateFlow<List<com.serranoie.app.minus.domain.model.Category>> = _categories.asStateFlow()

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
				budgetRepository.getQueuedTransactions()
			) { settings, transactions, periodBoundary, queuedTransactions ->
				createBaseUiState(settings, transactions, periodBoundary, queuedTransactions)
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

	private fun buildPeriodBoundaryFlow() = context.settingsDataStore.data.map { prefs ->
		_pendingPeriodBoundaryOverride.value ?: Pair(
			prefs[CURRENT_PERIOD_STARTED_AT_KEY] ?: 0L,
			prefs[CURRENT_PERIOD_ID_KEY] ?: 0L
		)
	}

	private fun createBaseUiState(
		settings: BudgetSettings?,
		transactions: List<Transaction>,
		periodBoundary: Pair<Long, Long>,
		queuedTransactions: List<Transaction>,
	): BudgetUiState {
		val currentPeriodStartedAtMillis = periodBoundary.first
		val currentPeriodId = periodBoundary.second
		val budgetState = settings?.let { s ->
			val today = LocalDate.now()
			val periodTransactions = filterPeriodTransactions(
				transactions = transactions,
				settings = s,
				currentPeriodId = currentPeriodId,
				currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
			)
			calculateBudgetState(s, periodTransactions, today)
		}

		return BudgetUiState(
			isLoading = false,
			budgetSettings = settings,
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
			pendingExpensesForNextPeriod = queuedTransactions,
			currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
			currentPeriodId = currentPeriodId,
			isCalculation = _uiState.value.isCalculation,
			dragProgress = _uiState.value.dragProgress,
			lockSwipeable = _uiState.value.lockSwipeable,
			lockDraggable = _uiState.value.lockDraggable,
		)
	}

	private fun filterPeriodTransactions(
		transactions: List<Transaction>,
		settings: BudgetSettings,
		currentPeriodId: Long,
		currentPeriodStartedAtMillis: Long,
	): List<Transaction> {
		val periodEnd = settings.getPeriodEndDate()
		return transactions.filter { transaction ->
			if (currentPeriodId > 0L && transaction.periodId > 0L) {
				return@filter transaction.periodId == currentPeriodId
			}
			val txDate = transaction.date?.toLocalDate() ?: return@filter false
			if (txDate.isBefore(settings.startDate) || txDate.isAfter(periodEnd)) {
				return@filter false
			}
			if (txDate.isEqual(settings.startDate) && currentPeriodStartedAtMillis > 0L) {
				return@filter transaction.createdAt >= currentPeriodStartedAtMillis
			}
			true
		}
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

		updateWidgets(baseState)
	}

	private suspend fun updateWidgets(baseState: BudgetUiState) {
		val budget = baseState.budgetState ?: return
		val currency = baseState.budgetSettings?.currencyCode ?: "USD"
		val totalSpent = budget.totalSpentInPeriod.toInt()
		val totalBudget = baseState.budgetSettings?.totalBudget?.toInt() ?: 1
		val daysLeft = budget.daysRemaining
		val budgetAmount = budget.totalBudget.toInt()
		val (startDate, endDate) = resolveBudgetPeriodDates(baseState)
		val heatmapData = buildHeatmapData(baseState)

		updateExpenseWidget(context, totalSpent, totalBudget, currency)
		updateBudgetOverviewWidget(context, budgetAmount, currency, startDate, endDate, daysLeft)
		updateDaysCountdownWidget(context, daysLeft, budget.totalBudget.toInt(), "days left")
		updateHeatmapWidget(context, heatmapData.monthHeatmapData)
		updateMonthHeatmapWidget(context, heatmapData.currentMonthHeatmap, heatmapData.currentMonthTotalSpent)
	}

	private fun resolveBudgetPeriodDates(baseState: BudgetUiState): Pair<java.util.Date, java.util.Date> {
		val startDate = baseState.budgetSettings?.startDate?.let {
			java.util.Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
		} ?: java.util.Date()
		val endDate = baseState.budgetSettings?.getPeriodEndDate()?.let {
			java.util.Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
		} ?: java.util.Date()
		return startDate to endDate
	}

	private data class WidgetHeatmapData(
		val monthHeatmapData: List<MonthHeatmapData>,
		val currentMonthHeatmap: MonthHeatmapData,
		val currentMonthTotalSpent: Int,
	)

	private fun buildHeatmapData(baseState: BudgetUiState): WidgetHeatmapData {
		val now = LocalDate.now()
		val groupedByDate = groupTransactionsForHeatmap(baseState.transactions, now)
		val defaultBudget = baseState.budgetSettings?.totalBudget ?: BigDecimal.ONE
		val monthHeatmapData = buildMultiMonthHeatmapData(now, groupedByDate, defaultBudget)
		val (currentMonthHeatmap, currentMonthTotalSpent) = buildCurrentMonthHeatmap(now, groupedByDate, defaultBudget)
		return WidgetHeatmapData(
			monthHeatmapData = monthHeatmapData,
			currentMonthHeatmap = currentMonthHeatmap,
			currentMonthTotalSpent = currentMonthTotalSpent,
		)
	}

	private fun groupTransactionsForHeatmap(
		transactions: List<Transaction>,
		now: LocalDate,
	): Map<LocalDate, List<Transaction>> {
		val startMonth = now.minusMonths(3).withDayOfMonth(1)
		val endMonth = now.withDayOfMonth(now.lengthOfMonth())
		return transactions
			.filter { tx ->
				!tx.isDeleted && tx.date != null &&
					!tx.date!!.toLocalDate().isBefore(startMonth) &&
					!tx.date!!.toLocalDate().isAfter(endMonth)
			}
			.groupBy { it.date!!.toLocalDate() }
	}

	private fun buildMultiMonthHeatmapData(
		now: LocalDate,
		groupedByDate: Map<LocalDate, List<Transaction>>,
		defaultBudget: BigDecimal,
	): List<MonthHeatmapData> {
		return (0L..3L).map { monthOffset ->
			val month = now.minusMonths(3 - monthOffset)
			val days = (1..month.lengthOfMonth()).map { dayOfMonth ->
				val day = month.withDayOfMonth(dayOfMonth)
				val txs = groupedByDate[day].orEmpty()
				DailySpending(
					dayOfMonth = dayOfMonth,
					spending = txs.sumOf { it.amount },
					budget = defaultBudget,
					transactionCount = txs.size,
				)
			}
			MonthHeatmapData(
				year = month.year,
				month = month.monthValue,
				days = days,
			)
		}
	}

	private fun buildCurrentMonthHeatmap(
		now: LocalDate,
		groupedByDate: Map<LocalDate, List<Transaction>>,
		defaultBudget: BigDecimal,
	): Pair<MonthHeatmapData, Int> {
		val currentMonthDays = (1..now.lengthOfMonth()).map { dayOfMonth ->
			val day = now.withDayOfMonth(dayOfMonth)
			val txs = groupedByDate[day].orEmpty()
			DailySpending(
				dayOfMonth = dayOfMonth,
				spending = txs.sumOf { it.amount },
				budget = defaultBudget,
				transactionCount = txs.size,
			)
		}
		val currentMonthTotalSpent = currentMonthDays.sumOf { it.spending }.toInt()
		val currentMonthHeatmap = MonthHeatmapData(
			year = now.year,
			month = now.monthValue,
			days = currentMonthDays,
		)
		return currentMonthHeatmap to currentMonthTotalSpent
	}

	private suspend fun startNewPeriod(settings: BudgetSettings) {
		val newStartDate = LocalDate.now()
		val updatedSettings = settings.copy(
			startDate = newStartDate,
			rollOverCarryForward = false,
			rollOverLimit = null
		)
		persistBudgetSettings(updatedSettings, forceNewPeriodBoundary = true)
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
			context.settingsDataStore.edit { prefs ->
				prefs[NOTIFICATION_HOUR_KEY] = hour
				prefs[NOTIFICATION_MINUTE_KEY] = minute
			}
			logcat(TAG) { "Updated period end notification time to %02d:%02d".format(hour, minute) }
			budgetRepository.getBudgetSettingsSync()?.let { settings ->
				notificationScheduler.schedulePeriodEndNotification(settings.getPeriodEndDate())
			}
		}
	}


	fun finishBudgetEarly() {
		viewModelScope.launch {
			val settings = budgetRepository.getBudgetSettingsSync() ?: return@launch
			val originalEndDate = settings.getPeriodEndDate()
			val now = LocalDate.now()

			context.settingsDataStore.edit { prefs ->
				prefs[EARLY_FINISH_ACTIVE_KEY] = true
				prefs[EARLY_FINISH_ACTUAL_DATE_KEY] = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
				prefs[EARLY_FINISH_ORIGINAL_END_DATE_KEY] = originalEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
			}
		}
	}

	fun clearEarlyFinishState() {
		viewModelScope.launch {
			clearEarlyFinishStateSync()
		}
	}

	private suspend fun clearEarlyFinishStateSync() {
		context.settingsDataStore.edit { prefs ->
			prefs[EARLY_FINISH_ACTIVE_KEY] = false
			prefs.remove(EARLY_FINISH_ACTUAL_DATE_KEY)
			prefs.remove(EARLY_FINISH_ORIGINAL_END_DATE_KEY)
		}
	}

	fun markFirstLaunchComplete() {
		_uiState.update { it.copy(isFirstLaunch = false) }

		viewModelScope.launch {
			val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
			context.settingsDataStore.edit { prefs ->
				prefs[ONBOARDING_COMPLETED_KEY] = true
			}
		}
	}


	fun triggerTestNotifications() {
		viewModelScope.launch {
			logcat(TAG) { "=== TRIGGERING TEST NOTIFICATIONS ===" }
			
			val settings = budgetRepository.getBudgetSettingsSync()
			val currency = settings?.currencyCode ?: "USD"
			
			logcat(TAG) { "Currency: $currency, Settings: $settings" }
			
			try {
				notificationHelper.showPeriodEndNotification(
					remainingBudget = "150.00",
					currency = currency
				)
				logcat(TAG) { "? Test period end notification triggered" }
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
				logcat(TAG) { "? Test recurrent expense notification triggered" }
			} catch (e: Exception) {
				logcat(TAG) { e.asLog() }
			}
			
			logcat(TAG) { "=== TEST NOTIFICATIONS COMPLETE ===" }
		}
	}

	fun processIntent(intent: BudgetUiIntent) {
		when (intent) {
			is BudgetUiIntent.NumberTapped -> handleNumberInput(intent.digit)
			is BudgetUiIntent.DotTapped -> handleDotInput()
			is BudgetUiIntent.BackspaceTapped -> handleBackspace()
			is BudgetUiIntent.ApplyTapped -> handleApply()
			is BudgetUiIntent.DeleteTransactionTapped -> handleDeleteTransaction(intent.transaction)
			is BudgetUiIntent.RestoreTransactionTapped -> handleRestoreTransaction(intent.transaction)
			is BudgetUiIntent.EditTransactionTapped -> handleEditTransaction(intent.updatedTransaction)
			is BudgetUiIntent.DateSelected -> handleDateSelected(intent.date)
			is BudgetUiIntent.UpdateSettings -> handleUpdateSettings(intent.settings)
			is BudgetUiIntent.ResetInputTapped -> handleResetInput()
			is BudgetUiIntent.SetEditMode -> handleSetEditMode(intent.mode)
			is BudgetUiIntent.SetAnimState -> handleSetAnimState(intent.state)
			is BudgetUiIntent.CommentUpdated -> handleCommentUpdate(intent.comment)
			is BudgetUiIntent.DeleteTag -> handleDeleteTag(intent.tag)
			is BudgetUiIntent.MarkFirstLaunchComplete -> markFirstLaunchComplete()
			is BudgetUiIntent.SetRecurrentEnabled -> handleSetRecurrentEnabled(intent.enabled)
			is BudgetUiIntent.DismissRecurrentDialog -> handleDismissRecurrentDialog()
			is BudgetUiIntent.RecurrentExpenseApplied -> handleRecurrentExpenseApply(intent.frequency, intent.endDate, intent.subscriptionDay)
			is BudgetUiIntent.FinishBudgetEarly -> handleFinishBudgetEarly()
			is BudgetUiIntent.TriggerTestNotifications -> triggerTestNotifications()
			is BudgetUiIntent.OperatorTapped -> handleOperatorInput(intent.operator)
			is BudgetUiIntent.EqualsTapped -> handleEqualsInput()
			is BudgetUiIntent.SetCalculationMode -> handleSetCalculationMode(intent.enabled)
			is BudgetUiIntent.SetDragProgress -> handleDragProgress(intent.progress)
			is BudgetUiIntent.SetLockSwipeable -> handleSetLockSwipeable(intent.locked)
			is BudgetUiIntent.SetLockDraggable -> handleSetLockDraggable(intent.locked)
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
			_numpadInput.value = "$currentInput" + "0."
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
		if (updatedInput.none { it in "+-×÷" } && _uiState.value.isCalculation) {
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
		
		val result = evaluateCalculation(currentInput)
		if (result != null) {
			_numpadInput.value = result
			if (!_uiState.value.isCalculation) {
				_uiState.update { it.copy(isCalculation = true) }
			}
		}
	}


	private fun evaluateCalculation(input: String): String? {
		if (input.isBlank()) return null

		return try {
			val normalized = input.trim()
				.replace("×", "*")
				.replace("÷", "/")

			normalized.lastOrNull()?.let { if (it in "+-*/") return null }

			val hasOperator = normalized.any { it in "+-*/" }

			if (!hasOperator) {
				val num = normalized.toBigDecimalOrNull() ?: return null
				return if (num.scale() <= 0 || num.stripTrailingZeros().scale() <= 0) {
					num.toBigInteger().toString()
				} else {
					num.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
				}
			}

			val tokenPattern = Regex("([+\\-*/])")
			val parts = tokenPattern.split(normalized).filter { it.isNotEmpty() }
			val operators = tokenPattern.findAll(normalized).map { it.value }.toList()

			if (parts.isEmpty() || parts[0].isEmpty()) return null

			if (operators.size > parts.size - 1) return null

			var result = parts[0].toBigDecimalOrNull() ?: return null

			for (i in operators.indices) {
				if (i + 1 >= parts.size) break
				val operator = operators[i]
				val nextNum = parts[i + 1].toBigDecimalOrNull() ?: return null

				result = when (operator) {
					"+" -> result + nextNum
					"-" -> result - nextNum
					"*" -> result * nextNum
					"/" -> {
						if (nextNum.compareTo(BigDecimal.ZERO) == 0) return null // Division by zero
						result.divide(nextNum, 2, java.math.RoundingMode.HALF_UP)
					}
					else -> return null
				}
			}

			if (result.scale() <= 0 || result.stripTrailingZeros().scale() <= 0) {
				result.toBigInteger().toString()
			} else {
				result.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
			}
		} catch (e: Exception) {
			null
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
		var input = _numpadInput.value
		
		if (_uiState.value.isCalculation && input.any { it in "+-×÷" }) {
			val calculatedResult = evaluateCalculation(input)
			if (calculatedResult != null) {
				input = calculatedResult
				_numpadInput.value = calculatedResult
			}
		}
		
		if (!validateNumpadInput(input)) return

		val amount = try {
			BigDecimal(input)
		} catch (e: NumberFormatException) {
			return
		}

		if (amount < BigDecimal.ZERO) {
			_numpadInput.value = ""
			return
		}

		if (_uiState.value.isRecurrentEnabled) {
			_uiState.update {
				it.copy(
					showRecurrentDialog = true,
					pendingRecurrentAmount = amount,
					pendingRecurrentComment = _currentComment.value
				)
			}
			return
		}

		// Check if the period has ended - if so, show dialog instead of immediately saving
		val settings = _uiState.value.budgetSettings
		val today = LocalDate.now()
		if (settings != null) {
			val periodEndDate = settings.getPeriodEndDate()
			if (today.isAfter(periodEndDate)) {
				viewModelScope.launch {
					val categoryId: Long? = if (_currentComment.value.isNotBlank()) {
						budgetRepository.findOrCreateCategory(_currentComment.value.trim()).id
					} else null
					val pendingTransaction = Transaction.create(
						amount = amount,
						comment = _currentComment.value,
						date = LocalDateTime.now(),
						periodId = 0L,
						categoryId = categoryId
					)
					budgetRepository.addQueuedTransaction(pendingTransaction)
					_numpadInput.value = ""
					_currentComment.value = ""
					_uiState.update { it.copy(isCalculation = false) }
					_effects.emit(BudgetUiEffect.ShowMessage("Gasto en cola para el proximo periodo"))
				}
				return
			}
		}

		viewModelScope.launch {
			val activePeriodId = resolveActivePeriodId()
			val categoryId: Long? = if (_currentComment.value.isNotBlank()) {
				budgetRepository.findOrCreateCategory(_currentComment.value.trim()).id
			} else null

			val transaction = Transaction.create(
				amount = amount,
				comment = _currentComment.value,
				date = LocalDateTime.now(),
				periodId = activePeriodId,
				categoryId = categoryId
			)
			addTransactionUseCase(transaction)
			_numpadInput.value = ""
			_currentComment.value = ""
			_uiState.update { it.copy(isCalculation = false) }
		}
	}

	private fun handleSetRecurrentEnabled(enabled: Boolean) {
		_uiState.update { it.copy(isRecurrentEnabled = enabled) }
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

	private fun handleRecurrentExpenseApply(frequency: RecurrentFrequency, endDate: LocalDate, subscriptionDay: Int?) {
		val amount = _uiState.value.pendingRecurrentAmount ?: return
		val rawComment = _uiState.value.pendingRecurrentComment.trim()
		val now = LocalDateTime.now()

		val fallbackComment = when (frequency) {
			RecurrentFrequency.WEEKLY -> "Subscripción semanal sin nombre"
			RecurrentFrequency.BIWEEKLY -> "Subscripción quincenal sin nombre"
			RecurrentFrequency.MONTHLY -> "Subscripción mensual sin nombre"
		}

		val finalComment = rawComment.ifEmpty { fallbackComment }

		viewModelScope.launch {
			val activePeriodId = resolveActivePeriodId()
			val categoryId: Long? = if (finalComment.isNotBlank()) {
				budgetRepository.findOrCreateCategory(finalComment.trim()).id
			} else null

			val transaction = Transaction.create(
				amount = amount,
				comment = finalComment,
				date = now,
				periodId = activePeriodId,
				isRecurrent = true,
				recurrentFrequency = frequency,
				// Preserve current hour/minute instead of forcing 00:00
				recurrentEndDate = endDate.atTime(now.toLocalTime()),
				subscriptionDay = subscriptionDay,
				categoryId = categoryId
			)
			addTransactionUseCase(transaction)

			_uiState.update {
				it.copy(
					showRecurrentDialog = false,
					pendingRecurrentAmount = null,
					pendingRecurrentComment = "",
					isRecurrentEnabled = false // Reset toggle after saving
				)
			}
			_numpadInput.value = ""
			_currentComment.value = ""
		}
	}

	private fun handleFinishBudgetEarly() {
		finishBudgetEarly()
	}

	private fun handleDeleteTransaction(transaction: Transaction) {
		logcat(TAG) { "handleDeleteTransaction called for transaction ${transaction.id}" }
		viewModelScope.launch {
			try {
				deleteTransactionUseCase(transaction)
				logcat(TAG) { "Transaction ${transaction.id} deleted successfully" }
			} catch (e: Exception) {
				logcat(TAG) { e.asLog() }
				_effects.emit(BudgetUiEffect.ShowMessage("Could not delete transaction"))
			}
		}
	}

	private fun handleRestoreTransaction(transaction: Transaction) {
		viewModelScope.launch {
			try {
				addTransactionUseCase(transaction)
				logcat(TAG) { "Transaction ${transaction.id} restored successfully" }
			} catch (e: Exception) {
				logcat(TAG) { e.asLog() }
				_effects.emit(BudgetUiEffect.ShowMessage("Could not restore transaction"))
			}
		}
	}

	private fun handleEditTransaction(updatedTransaction: Transaction) {
		if (updatedTransaction.amount < BigDecimal.ZERO) {
			return
		}
		viewModelScope.launch {
			budgetRepository.updateTransaction(updatedTransaction)
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

	private fun handleMarkFirstLaunchComplete() {
		_uiState.update { it.copy(isFirstLaunch = false) }
	}
	
	private suspend fun persistBudgetSettings(
		settings: BudgetSettings,
		forceNewPeriodBoundary: Boolean = false,
	) {
		logcat(TAG) { "persistBudgetSettings START settings=$settings forceNewPeriodBoundary=$forceNewPeriodBoundary" }
		clearEarlyFinishStateSync()
		val previousSettings = budgetRepository.getBudgetSettingsSync()
		val previousPrefs = context.settingsDataStore.data.first()
		logcat(TAG) { "persistBudgetSettings previousSettings=$previousSettings" }
		logcat(TAG) { "persistBudgetSettings previousPrefs currentPeriodStartedAt=${previousPrefs[CURRENT_PERIOD_STARTED_AT_KEY]} currentPeriodId=${previousPrefs[CURRENT_PERIOD_ID_KEY]}" }
		budgetRepository.saveBudgetSettings(settings)
		logcat(TAG) { "Budget settings saved to repository" }
		val shouldCreateNewPeriodBoundary = forceNewPeriodBoundary ||
			previousSettings == null ||
			previousSettings.startDate != settings.startDate
		val periodStartMillis = if (shouldCreateNewPeriodBoundary) {
			timeProvider.nowEpochMillis()
		} else {
			previousPrefs[CURRENT_PERIOD_STARTED_AT_KEY]
				?: settings.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
		}
		val periodId = if (shouldCreateNewPeriodBoundary) {
			periodStartMillis
		} else {
			previousPrefs[CURRENT_PERIOD_ID_KEY] ?: periodStartMillis
		}
		_pendingPeriodBoundaryOverride.value = periodStartMillis to periodId
		_uiState.update {
			it.copy(
				currentPeriodStartedAtMillis = periodStartMillis,
				currentPeriodId = periodId
			)
		}
		logcat(TAG) {
			"persistBudgetSettings boundaryDecision shouldCreateNewPeriodBoundary=$shouldCreateNewPeriodBoundary periodStartMillis=$periodStartMillis periodId=$periodId"
		}
		val periodEndDate = settings.getPeriodEndDate()
		val millis = periodEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
		logcat(TAG) { "Saving end date to DataStore: $periodEndDate -> $millis" }
		context.settingsDataStore.edit { prefs ->
			prefs[BUDGET_END_DATE_KEY] = millis
			prefs[CURRENT_PERIOD_STARTED_AT_KEY] = periodStartMillis
			prefs[CURRENT_PERIOD_ID_KEY] = periodId
			if (!prefs.contains(NOTIFICATION_HOUR_KEY)) {
				prefs[NOTIFICATION_HOUR_KEY] = DEFAULT_NOTIFICATION_HOUR
			}
			if (!prefs.contains(NOTIFICATION_MINUTE_KEY)) {
				prefs[NOTIFICATION_MINUTE_KEY] = DEFAULT_NOTIFICATION_MINUTE
			}
		}
		if (shouldCreateNewPeriodBoundary) {
			budgetRepository.assignQueuedTransactionsToPeriod(periodId)
		}
		val afterPrefs = context.settingsDataStore.data.first()
		_pendingPeriodBoundaryOverride.value = null
		logcat(TAG) {
			"persistBudgetSettings END savedSettingsPeriod=${settings.period} savedSettingsDays=${settings.daysInPeriod} savedSettingsStart=${settings.startDate} savedSettingsEnd=${settings.endDate} datastorePeriodStart=${afterPrefs[CURRENT_PERIOD_STARTED_AT_KEY]} datastorePeriodId=${afterPrefs[CURRENT_PERIOD_ID_KEY]}"
		}
		notificationScheduler.schedulePeriodEndNotification(periodEndDate)
		logcat(TAG) { "Period end notification scheduled for $periodEndDate" }
	}

	private suspend fun resolveActivePeriodId(): Long {
		_pendingPeriodBoundaryOverride.value?.second?.let { pendingPeriodId ->
			if (pendingPeriodId > 0L) return pendingPeriodId
		}

		val dataStorePeriodId = context.settingsDataStore.data.first()[CURRENT_PERIOD_ID_KEY] ?: 0L
		if (dataStorePeriodId > 0L) return dataStorePeriodId

		return _uiState.value.currentPeriodId
	}

	private fun validateNumpadInput(input: String): Boolean {
		if (input.isEmpty()) return false
		if (input == ".") return false
		return try {
			val value = BigDecimal(input)
			value > BigDecimal.ZERO
		} catch (e: NumberFormatException) {
			false
		}
	}

	private fun calculateBudgetState(
		settings: BudgetSettings, transactions: List<Transaction>, currentDate: LocalDate
	): BudgetState {
		val periodEnd = settings.getPeriodEndDate()
		logcat(TAG) { "calculateBudgetState: periodEnd=$periodEnd (from endDate=${settings.endDate} or period calculation)" }

		val daysRemaining = ChronoUnit.DAYS.between(currentDate, periodEnd).toInt() + 1
		logcat(TAG) { "daysRemaining=$daysRemaining (from $currentDate to $periodEnd)" }

		val originalTotalDays = ChronoUnit.DAYS.between(settings.startDate, periodEnd).toInt() + 1
		logcat(TAG) { "originalTotalDays=$originalTotalDays (from ${settings.startDate} to $periodEnd)" }

		val totalSpentInPeriod = transactions.filter { !it.isDeleted }.sumOf { it.amount }

		val carryForFirstDay = if (
			settings.rollOverCarryForward && currentDate.isEqual(settings.startDate)
		) {
			settings.rollOverLimit ?: BigDecimal.ZERO
		} else {
			BigDecimal.ZERO
		}
		val rolloverAmount = if (settings.rollOverCarryForward) {
			settings.rollOverLimit ?: BigDecimal.ZERO
		} else {
			BigDecimal.ZERO
		}
		val effectiveTotalBudget = settings.totalBudget.add(rolloverAmount)

		val remainingBudget = effectiveTotalBudget.subtract(totalSpentInPeriod)

		val originalDailyBudget = if (originalTotalDays > 0) {
			settings.totalBudget.divide(
				BigDecimal(originalTotalDays), 2, RoundingMode.HALF_UP
			)
		} else {
			BigDecimal.ZERO
		}
		logcat(TAG) { "originalDailyBudget=$originalDailyBudget (baseTotalBudget=${settings.totalBudget} / originalTotalDays=$originalTotalDays)" }

		val regularSpentToday =
			transactions.filter { !it.isDeleted && it.date?.toLocalDate() == currentDate }
				.sumOf { it.amount }
		
		val recurringDueToday = recurringExpenseCalculator.calculateRecurringDueToday(transactions, currentDate)
		
		val spentToday = regularSpentToday.add(recurringDueToday)
		logcat(TAG) { "spentToday=$spentToday (regular=$regularSpentToday + recurring=$recurringDueToday)" }

		val remainingToday = originalDailyBudget.add(carryForFirstDay).subtract(spentToday)
		logcat(TAG) { "remainingToday=$remainingToday (originalDailyBudget=$originalDailyBudget + carryForFirstDay=$carryForFirstDay - spentToday=$spentToday)" }

		val progress = if (effectiveTotalBudget > BigDecimal.ZERO) {
			totalSpentInPeriod.divide(effectiveTotalBudget, 4, RoundingMode.HALF_UP).toFloat()
				.coerceIn(0f, 1f)
		} else {
			0f
		}

		return BudgetState(
			remainingToday = remainingToday,
			totalSpentToday = spentToday,
			dailyBudget = originalDailyBudget,
			daysRemaining = daysRemaining.coerceAtLeast(0),
			progress = progress,
			isOverBudget = remainingBudget < BigDecimal.ZERO,
			totalBudget = effectiveTotalBudget,
			totalSpentInPeriod = totalSpentInPeriod.add(recurringDueToday)
		).also {
			logcat(TAG) { "BudgetState created: $it" }
		}
	}
	
}
