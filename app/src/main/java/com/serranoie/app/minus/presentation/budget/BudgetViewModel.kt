package com.serranoie.app.minus.presentation.budget

import android.content.Context
import android.util.Log
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
import com.serranoie.app.minus.presentation.widget.updateBudgetDetailWidget
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

	private val _effects = MutableSharedFlow<BudgetUiEffect>()
	val effects: SharedFlow<BudgetUiEffect> = _effects.asSharedFlow()

	private val _numpadInput = MutableStateFlow("")
	private val _currentComment = MutableStateFlow("")

	private var lastPeriodEndDate: LocalDate? = null

	init {
		viewModelScope.launch {
			val periodBoundaryFlow = context.settingsDataStore.data.map { prefs ->
				Pair(
					prefs[CURRENT_PERIOD_STARTED_AT_KEY] ?: 0L,
					prefs[CURRENT_PERIOD_ID_KEY] ?: 0L
				)
			}
			combine(
				budgetRepository.getBudgetSettings(),
				budgetRepository.getTransactions(),
				_numpadInput,
				_currentComment,
				periodBoundaryFlow
			) { settings, transactions, numpadInput, currentComment, periodBoundary ->
				val currentPeriodStartedAtMillis = periodBoundary.first
				val currentPeriodId = periodBoundary.second
				val budgetState = settings?.let { s ->
					val today = LocalDate.now()
					val periodEnd = s.getPeriodEndDate()
					val periodTransactions = transactions.filter { transaction ->
						if (currentPeriodId > 0L && transaction.periodId > 0L) {
							return@filter transaction.periodId == currentPeriodId
						}
						val txDate = transaction.date?.toLocalDate() ?: return@filter false
						if (txDate.isBefore(s.startDate) || txDate.isAfter(periodEnd)) {
							return@filter false
						}
						if (txDate.isEqual(s.startDate) && currentPeriodStartedAtMillis > 0L) {
							return@filter transaction.createdAt >= currentPeriodStartedAtMillis
						}
						true
					}
					calculateBudgetState(s, periodTransactions, today)
				}

				val showRollover = checkAndUpdateRollover(settings, budgetState)

				// Extract unique tags from transaction comments
				val tags = extractTagsFromTransactions(transactions)

				BudgetUiState(
					isLoading = false,
					budgetSettings = settings,
					budgetState = budgetState,
					transactions = transactions,
					selectedDate = LocalDate.now(),
					error = null,
					numpadInput = numpadInput,
					isNumpadValid = validateNumpadInput(numpadInput),
					editMode = _uiState.value.editMode,
					animState = if (numpadInput.isNotEmpty()) AnimState.EDITING else AnimState.IDLE,
					currentComment = currentComment,
					tags = tags,
					showRolloverDialog = showRollover.first,
					remainingFromPreviousPeriod = showRollover.second,
					isFirstLaunch = settings == null,
					currentPeriodStartedAtMillis = currentPeriodStartedAtMillis,
					currentPeriodId = currentPeriodId
				)
			}.catch { error ->
				emit(
					BudgetUiState(
						isLoading = false,
						error = error.message ?: "Unknown error",
						isFirstLaunch = true
					)
				)
			}.collect { state ->
				_uiState.value = state

				// Update all widgets when data changes
				state.budgetState?.let { budget ->
					val currency = state.budgetSettings?.currencyCode ?: "USD"
					val totalSpent = budget.totalSpentInPeriod.toInt()
					val totalBudget = state.budgetSettings?.totalBudget?.toInt() ?: 1
					val remaining = budget.remainingToday.toInt()
					val daysLeft = budget.daysRemaining

					// Convert LocalDate to Date for widget
					val startDate = state.budgetSettings?.startDate?.let {
						java.util.Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
					} ?: java.util.Date()
					val endDate = state.budgetSettings?.getPeriodEndDate()?.let {
						java.util.Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
					} ?: java.util.Date()
					val budgetString = "$currency${budget.totalBudget.toInt()}"

					// Update widgets
					updateExpenseWidget(context, totalSpent, totalBudget, currency)
					updateBudgetOverviewWidget(context, budgetString, startDate, endDate, daysLeft)
					updateDaysCountdownWidget(context, daysLeft, budget.totalBudget.toInt(), "days left")
					updateBudgetDetailWidget(context, totalSpent, remaining, daysLeft, currency, "Food", 0)
				}
			}
		}

		viewModelScope.launch {
			val settings = budgetRepository.getBudgetSettingsSync()
			if (settings == null) {
				_uiState.update { it.copy(isFirstLaunch = true) }
			} else {
				checkRolloverOnPeriodStart(settings)
			}
		}
	}

	private suspend fun checkRolloverOnPeriodStart(settings: BudgetSettings) {
		val today = LocalDate.now()
		val periodEnd = settings.getPeriodEndDate()

		if (today.isAfter(periodEnd) || today.isEqual(periodEnd)) {
			val transactions = budgetRepository.getTransactions().first()
			val periodTransactions = transactions.filter {
				val txDate = it.date?.toLocalDate()
				!txDate?.isBefore(settings.startDate)!! && !txDate?.isAfter(periodEnd)!!
			}
			val totalSpent = periodTransactions.filter { !it.isDeleted }.sumOf { it.amount }
			val remaining = settings.totalBudget.subtract(totalSpent)

			if (remaining > BigDecimal.ZERO) {
				_uiState.update {
					it.copy(
						showRolloverDialog = true, remainingFromPreviousPeriod = remaining
					)
				}
			} else {
				startNewPeriod(settings)
			}

			lastPeriodEndDate = periodEnd
		}
	}

	private fun checkAndUpdateRollover(
		settings: BudgetSettings?, budgetState: com.serranoie.app.minus.domain.model.BudgetState?
	): Pair<Boolean, BigDecimal> {
		if (settings == null || budgetState == null) {
			return Pair(false, BigDecimal.ZERO)
		}

		val today = LocalDate.now()
		val periodEnd = settings.getPeriodEndDate()

		if (lastPeriodEndDate != null && (today.isAfter(lastPeriodEndDate) || today.isEqual(
				lastPeriodEndDate
			))
		) {
			val remaining = budgetState.remainingToday

			if (remaining > BigDecimal.ZERO && !_uiState.value.showRolloverDialog) {
				return Pair(true, remaining)
			}
		}

		return Pair(_uiState.value.showRolloverDialog, _uiState.value.remainingFromPreviousPeriod)
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

	fun saveBudgetSettings(settings: BudgetSettings) {
		Log.d(TAG, "saveBudgetSettings called: settings=$settings")
		viewModelScope.launch {
			persistBudgetSettings(settings)
		}
	}
	
	fun updatePeriodEndNotificationTime(hour: Int, minute: Int) {
		viewModelScope.launch {
			context.settingsDataStore.edit { prefs ->
				prefs[NOTIFICATION_HOUR_KEY] = hour
				prefs[NOTIFICATION_MINUTE_KEY] = minute
			}
			Log.d(TAG, "Updated period end notification time to %02d:%02d".format(hour, minute))
			budgetRepository.getBudgetSettingsSync()?.let { settings ->
				notificationScheduler.schedulePeriodEndNotification(settings.getPeriodEndDate())
			}
		}
	}

	/**
	 * Marks the current period as finished early.
	 * Keeps current settings and transactions intact so Analytics can show the finished period snapshot.
	 */
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

	/**
	 * Trigger test notifications - called when user presses . eight times then apply.
	 * Shows both period end and recurrent expense notifications for testing.
	 */
	fun triggerTestNotifications() {
		viewModelScope.launch {
			Log.d(TAG, "=== TRIGGERING TEST NOTIFICATIONS ===")
			
			val settings = budgetRepository.getBudgetSettingsSync()
			val currency = settings?.currencyCode ?: "USD"
			
			Log.d(TAG, "Currency: $currency, Settings: $settings")
			
			// Test period end notification
			try {
				notificationHelper.showPeriodEndNotification(
					remainingBudget = "150.00",
					currency = currency
				)
				Log.d(TAG, "✓ Test period end notification triggered")
			} catch (e: Exception) {
				Log.e(TAG, "✗ Error showing period end notification", e)
			}
			
			// Test recurrent expense notification
			try {
				notificationHelper.showRecurrentExpenseNotification(
					amount = "50.00",
					comment = "Test expense",
					frequency = "MONTHLY",
					currency = currency
				)
				Log.d(TAG, "✓ Test recurrent expense notification triggered")
			} catch (e: Exception) {
				Log.e(TAG, "✗ Error showing recurrent expense notification", e)
			}
			
			Log.d(TAG, "=== TEST NOTIFICATIONS COMPLETE ===")
		}
	}

	fun processIntent(intent: BudgetUiIntent) {
		when (intent) {
			is BudgetUiIntent.NumberTapped -> handleNumberInput(intent.digit)
			is BudgetUiIntent.DotTapped -> handleDotInput()
			is BudgetUiIntent.BackspaceTapped -> handleBackspace()
			is BudgetUiIntent.ApplyTapped -> handleApply()
			is BudgetUiIntent.DeleteTransactionTapped -> handleDeleteTransaction(intent.transaction)
			is BudgetUiIntent.EditTransactionTapped -> handleEditTransaction(intent.updatedTransaction)
			is BudgetUiIntent.DateSelected -> handleDateSelected(intent.date)
			is BudgetUiIntent.UpdateSettings -> handleUpdateSettings(intent.settings)
			is BudgetUiIntent.ResetInputTapped -> handleResetInput()
			is BudgetUiIntent.SetEditMode -> handleSetEditMode(intent.mode)
			is BudgetUiIntent.SetAnimState -> handleSetAnimState(intent.state)
			is BudgetUiIntent.CommentUpdated -> handleCommentUpdate(intent.comment)
			is BudgetUiIntent.RolloverSplitEqually -> handleRolloverSplitEqually(intent.remaining)
			is BudgetUiIntent.RolloverCarryToNextDay -> handleRolloverCarryToNextDay(intent.remaining)
			is BudgetUiIntent.DismissRolloverDialog -> handleDismissRolloverDialog()
			is BudgetUiIntent.MarkFirstLaunchComplete -> markFirstLaunchComplete()
			is BudgetUiIntent.SetRecurrentEnabled -> handleSetRecurrentEnabled(intent.enabled)
			is BudgetUiIntent.DismissRecurrentDialog -> handleDismissRecurrentDialog()
			is BudgetUiIntent.RecurrentExpenseApplied -> handleRecurrentExpenseApply(intent.frequency, intent.endDate, intent.subscriptionDay)
			is BudgetUiIntent.FinishBudgetEarly -> handleFinishBudgetEarly()
			is BudgetUiIntent.TriggerTestNotifications -> triggerTestNotifications()
		}
	}


	private fun handleNumberInput(digit: String) {
		val currentInput = _numpadInput.value
		if (currentInput.length >= 10) return

		_numpadInput.value = currentInput + digit
	}

	private fun handleDotInput() {
		val currentInput = _numpadInput.value
		// Only add decimal point if not already present
		if (currentInput.contains(".")) return
		if (currentInput.length >= 10) return
		// If input is empty, add "0." for better UX
		_numpadInput.value = if (currentInput.isEmpty()) "0." else "$currentInput."
	}

	private fun handleBackspace() {
		_numpadInput.value = _numpadInput.value.dropLast(1)
	}

	private fun handleApply() {
		val input = _numpadInput.value
		if (!validateNumpadInput(input)) return

		val amount = try {
			BigDecimal(input)
		} catch (e: NumberFormatException) {
			return
		}

		// Check if recurrent is enabled - if so, show dialog instead of saving immediately
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

		viewModelScope.launch {
			val transaction = Transaction.create(
				amount = amount,
				comment = _currentComment.value,
				date = LocalDateTime.now(),
				periodId = _uiState.value.currentPeriodId
			)
			addTransactionUseCase(transaction)
			_numpadInput.value = ""
			_currentComment.value = ""
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
		val comment = _uiState.value.pendingRecurrentComment

		viewModelScope.launch {
			val transaction = Transaction.create(
				amount = amount,
				comment = comment,
				date = LocalDateTime.now(),
				periodId = _uiState.value.currentPeriodId,
				isRecurrent = true,
				recurrentFrequency = frequency,
				recurrentEndDate = endDate.atStartOfDay(),
				subscriptionDay = subscriptionDay
			)
			addTransactionUseCase(transaction)

			// Clear pending state and input
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
		Log.d(TAG, "handleDeleteTransaction called for transaction ${transaction.id}")
		viewModelScope.launch {
			try {
				deleteTransactionUseCase(transaction)
				Log.d(TAG, "Transaction ${transaction.id} deleted successfully")
			} catch (e: Exception) {
				Log.e(TAG, "Error deleting transaction ${transaction.id}", e)
				_effects.emit(BudgetUiEffect.ShowMessage("Could not delete transaction"))
			}
		}
	}

	private fun handleEditTransaction(updatedTransaction: Transaction) {
		viewModelScope.launch {
			budgetRepository.updateTransaction(updatedTransaction)
		}
	}

	private fun handleDateSelected(date: LocalDate) {
		_uiState.update { it.copy(selectedDate = date) }
	}

	private fun handleUpdateSettings(settings: BudgetSettings) {
		viewModelScope.launch {
			persistBudgetSettings(settings)
		}
	}

	private fun handleResetInput() {
		_numpadInput.value = ""
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

	private fun extractTagsFromTransactions(transactions: List<Transaction>): List<String> {
		// Extract all non-empty, unique comments from transactions
		return transactions
			.map { it.comment }
			.filter { it.isNotBlank() }
			.distinct()
			.take(20) // Limit to 20 most recent tags
	}

	private fun handleRolloverSplitEqually(remaining: BigDecimal) {
		viewModelScope.launch {
			applyRolloverSplitEqually(remaining)
		}
	}

	private suspend fun applyRolloverSplitEqually(remaining: BigDecimal) {
		val settings = budgetRepository.getBudgetSettingsSync() ?: return

		val newTotalBudget = settings.totalBudget.add(remaining)
		val updatedSettings = settings.copy(
			totalBudget = newTotalBudget,
			startDate = LocalDate.now(),
			rollOverCarryForward = false,
			rollOverLimit = null,
			remainingBudgetStrategy = com.serranoie.app.minus.domain.model.RemainingBudgetStrategy.SPLIT_EQUALLY
		)

		persistBudgetSettings(updatedSettings, forceNewPeriodBoundary = true)

		_uiState.update {
			it.copy(
				showRolloverDialog = false, remainingFromPreviousPeriod = BigDecimal.ZERO
			)
		}
		lastPeriodEndDate = settings.getPeriodEndDate()
	}

	private fun handleRolloverCarryToNextDay(remaining: BigDecimal) {
		viewModelScope.launch {
			val settings = budgetRepository.getBudgetSettingsSync() ?: return@launch

			val updatedSettings = settings.copy(
				totalBudget = settings.totalBudget,
				startDate = LocalDate.now(),
				rollOverCarryForward = true,
				rollOverLimit = remaining,
				remainingBudgetStrategy = com.serranoie.app.minus.domain.model.RemainingBudgetStrategy.ADD_TO_FIRST_DAY
			)

			persistBudgetSettings(updatedSettings, forceNewPeriodBoundary = true)

			_uiState.update {
				it.copy(
					showRolloverDialog = false, remainingFromPreviousPeriod = BigDecimal.ZERO
				)
			}
			lastPeriodEndDate = settings.getPeriodEndDate()
		}
	}

	private fun handleDismissRolloverDialog() {
		viewModelScope.launch {
			val remaining = _uiState.value.remainingFromPreviousPeriod
			if (remaining > BigDecimal.ZERO) {
				applyRolloverSplitEqually(remaining)
			} else {
				_uiState.update {
					it.copy(
						showRolloverDialog = false,
						remainingFromPreviousPeriod = BigDecimal.ZERO
					)
				}
				val settings = budgetRepository.getBudgetSettingsSync()
				if (settings != null) {
					startNewPeriod(settings)
				}
			}
		}
	}

	private fun handleMarkFirstLaunchComplete() {
		_uiState.update { it.copy(isFirstLaunch = false) }
	}
	
	private suspend fun persistBudgetSettings(
		settings: BudgetSettings,
		forceNewPeriodBoundary: Boolean = false,
	) {
		clearEarlyFinishStateSync()
		val previousSettings = budgetRepository.getBudgetSettingsSync()
		val previousPrefs = context.settingsDataStore.data.first()
		budgetRepository.saveBudgetSettings(settings)
		Log.d(TAG, "Budget settings saved to repository")
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
		val periodEndDate = settings.getPeriodEndDate()
		val millis = periodEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
		Log.d(TAG, "Saving end date to DataStore: $periodEndDate -> $millis")
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
		notificationScheduler.schedulePeriodEndNotification(periodEndDate)
		Log.d(TAG, "Period end notification scheduled for $periodEndDate")
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
		Log.d(TAG, "calculateBudgetState: periodEnd=$periodEnd (from endDate=${settings.endDate} or period calculation)")

		val daysRemaining = ChronoUnit.DAYS.between(currentDate, periodEnd).toInt() + 1
		Log.d(TAG, "daysRemaining=$daysRemaining (from $currentDate to $periodEnd)")

		val originalTotalDays = ChronoUnit.DAYS.between(settings.startDate, periodEnd).toInt() + 1
		Log.d(TAG, "originalTotalDays=$originalTotalDays (from ${settings.startDate} to $periodEnd)")

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
		Log.d(TAG, "originalDailyBudget=$originalDailyBudget (baseTotalBudget=${settings.totalBudget} / originalTotalDays=$originalTotalDays)")

		// Calculate regular spent today
		val regularSpentToday =
			transactions.filter { !it.isDeleted && it.date?.toLocalDate() == currentDate }
				.sumOf { it.amount }
		
		// Calculate recurring charges due today
		val recurringDueToday = recurringExpenseCalculator.calculateRecurringDueToday(transactions, currentDate)
		
		val spentToday = regularSpentToday.add(recurringDueToday)
		Log.d(TAG, "spentToday=$spentToday (regular=$regularSpentToday + recurring=$recurringDueToday)")

		val remainingToday = originalDailyBudget.add(carryForFirstDay).subtract(spentToday)
		Log.d(TAG, "remainingToday=$remainingToday (originalDailyBudget=$originalDailyBudget + carryForFirstDay=$carryForFirstDay - spentToday=$spentToday)")

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
			totalSpentInPeriod = totalSpentInPeriod.add(recurringDueToday) // Include recurring in period total
		).also {
			Log.d(TAG, "BudgetState created: $it")
		}
	}
	
}
