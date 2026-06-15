package com.serranoie.app.minus.presentation.ui.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.RECURRENT_PAYMENTS_VIEW_MODE_KEY
import com.serranoie.app.minus.presentation.settingsDataStore
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetSystemIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.NoTransactionsView
import com.serranoie.app.minus.presentation.util.Utils.weakHapticFeedback
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

enum class RecurrentPaymentsViewMode {
	HORIZONTAL_LIST, VERTICAL_LIST;

	companion object {
		fun fromName(value: String?): RecurrentPaymentsViewMode = runCatching {
			value?.let(::valueOf)
		}.getOrNull() ?: HORIZONTAL_LIST
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun History(
	modifier: Modifier = Modifier,
	viewModel: BudgetViewModel = hiltViewModel(),
	readOnly: Boolean = false,
	onClose: () -> Unit = {},
	onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit = { _, _, _ -> },
	onCancelPendingDelete: () -> Unit = {},
	onShowInfoSnackbar: (message: String) -> Unit = {},
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	History(
		uiState = uiState,
		modifier = modifier,
		readOnly = readOnly,
		onClose = onClose,
		onQueueDeleteWithUndo = onQueueDeleteWithUndo,
		onCancelPendingDelete = onCancelPendingDelete,
		onShowInfoSnackbar = onShowInfoSnackbar,
		onProcessIntent = viewModel::processIntent,
		onProcessSystemIntent = viewModel::processIntent
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun History(
	uiState: BudgetUiState,
	modifier: Modifier = Modifier,
	readOnly: Boolean = false,
	onClose: () -> Unit = {},
	onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit = { _, _, _ -> },
	onCancelPendingDelete: () -> Unit = {},
	onShowInfoSnackbar: (message: String) -> Unit = {},
	onProcessIntent: (BudgetTransactionIntent) -> Unit = {},
	onProcessSystemIntent: (BudgetSystemIntent) -> Unit = {},
) {
	val context = LocalContext.current
	val resources = LocalResources.current
	val view = LocalView.current
	val preferences by context.settingsDataStore.data.collectAsStateWithLifecycle(initialValue = emptyPreferences())
	val recurrentPaymentsViewMode = remember(preferences) {
		RecurrentPaymentsViewMode.fromName(preferences[RECURRENT_PAYMENTS_VIEW_MODE_KEY])
	}
	val scrollState = rememberLazyListState()

	val isAtEndOfList =
		remember(scrollState.canScrollForward, scrollState.layoutInfo.visibleItemsInfo) {
			!scrollState.canScrollForward && scrollState.layoutInfo.visibleItemsInfo.lastOrNull() != null
		}

	LaunchedEffect(isAtEndOfList) {
		onProcessSystemIntent(BudgetSystemIntent.SetLockSwipeable(!isAtEndOfList))
	}

	LaunchedEffect(isAtEndOfList) {
		if (isAtEndOfList) {
			// When at end, we could add additional handling here.
			// The lockSwipeable=false already enables swipe gestures.
			// The actual overscroll-to-dismiss is handled by the swipeable modifier.
		}
	}

	var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
	var pendingRemovedTransactions by remember { mutableStateOf<Map<Long, Transaction>>(emptyMap()) }
	var recurrentToDelete by remember { mutableStateOf<Transaction?>(null) }
	var recurrentToEdit by remember { mutableStateOf<Transaction?>(null) }
	var showDeleteRecurrentDialog by remember { mutableStateOf(false) }
	var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
	var isDismissingTransactionDialog by remember { mutableStateOf(false) }

	val budgetSettings = uiState.budgetSettings
	val budgetStartDate = budgetSettings?.startDate ?: LocalDate.now().minusDays(30)
	val budgetEndDate = budgetSettings?.getPeriodEndDate() ?: LocalDate.now()
	val currencyCode = budgetSettings?.currencyCode ?: "USD"
	val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }

	var expandedDates by rememberSaveable { mutableStateOf<Set<LocalDate>>(emptySet()) }
	var showPastPeriod by rememberSaveable { mutableStateOf(false) }
	var showOutOfPeriodSubscriptions by rememberSaveable { mutableStateOf(false) }
	var showUpcomingRecurrentInPeriod by rememberSaveable { mutableStateOf(true) }

	val deletingTransactionIds = remember(pendingRemovedTransactions) {
		pendingRemovedTransactions.keys.toSet()
	}

	val displayTransactions = remember(uiState.transactions, pendingRemovedTransactions) {
		buildDisplayTransactions(uiState.transactions, pendingRemovedTransactions)
	}

	val previousPeriodId = uiState.currentPeriodId - 1
	val (currentPeriodTransactions, pastPeriodTransactions) = remember(
		displayTransactions,
		budgetStartDate,
		budgetEndDate,
		uiState.currentPeriodStartedAtMillis,
		uiState.currentPeriodId,
		previousPeriodId,
	) {
		splitPeriodTransactions(
			transactions = displayTransactions,
			budgetStartDate = budgetStartDate,
			budgetEndDate = budgetEndDate,
			currentPeriodStartedAtMillis = uiState.currentPeriodStartedAtMillis,
			currentPeriodId = uiState.currentPeriodId,
			previousPeriodId = previousPeriodId,
		)
	}

	val (upcomingRecurrentInPeriod, futureRecurrentOutOfPeriod) = remember(
		displayTransactions,
		budgetStartDate,
		budgetEndDate,
	) {
		buildUpcomingRecurrentItems(
			transactions = displayTransactions,
			budgetStartDate = budgetStartDate,
			budgetEndDate = budgetEndDate,
			today = LocalDate.now(),
		)
	}

	val groupedCurrentTransactions = remember(
		currentPeriodTransactions,
		displayTransactions,
		budgetStartDate,
		budgetEndDate,
	) {
		buildGroupedCurrentTransactions(
			currentPeriodTransactions = currentPeriodTransactions,
			displayTransactions = displayTransactions,
			budgetStartDate = budgetStartDate,
			budgetEndDate = budgetEndDate,
			today = LocalDate.now(),
		)
	}

	val groupedPastTransactions = remember(pastPeriodTransactions) {
		groupTransactionsByDate(pastPeriodTransactions)
	}

	LaunchedEffect(groupedCurrentTransactions.keys, readOnly) {
		val sortedDates = groupedCurrentTransactions.keys.filterNotNull().sortedDescending()
		val defaultExpanded = sortedDates.take(1).toSet()
		expandedDates = expandedDates + defaultExpanded
	}

	LaunchedEffect(pendingRemovedTransactions.keys) {
		if (pendingRemovedTransactions.isNotEmpty()) {
			delay(330)
			pendingRemovedTransactions = emptyMap()
		}
	}

	fun queueDeleteWithUndo(transaction: Transaction) {
		pendingRemovedTransactions = pendingRemovedTransactions + (transaction.id to transaction)
		onQueueDeleteWithUndo(
			transaction,
			resources.getString(
				R.string.expense_deleted_format,
				transaction.comment.ifEmpty { resources.getString(R.string.generic_expense) },
			),
		) {
			pendingRemovedTransactions = pendingRemovedTransactions - transaction.id
			onCancelPendingDelete()
		}
	}

	fun toggleExpandedDate(date: LocalDate) {
		expandedDates = if (expandedDates.contains(date)) {
			expandedDates.minus(date)
		} else {
			expandedDates.plus(date)
		}
	}

	fun saveEditedTransaction(updatedTransaction: Transaction, onSaved: () -> Unit) {
		onProcessIntent(
			BudgetTransactionIntent.EditTransactionTapped(updatedTransaction)
		)
		onShowInfoSnackbar(
			resources.getString(
				R.string.expense_modified_format,
				updatedTransaction.comment.ifEmpty { resources.getString(R.string.generic_expense) },
			),
		)
		onSaved()
	}

	Box(modifier = modifier.fillMaxSize()) {
		LazyColumn(
			state = scrollState,
			modifier = Modifier
				.fillMaxSize()
				.animateContentSize()
				.statusBarsPadding()
				.padding(horizontal = 16.dp),
		) {
			budgetDisplaySection(
				budgetState = uiState.budgetState,
				budgetSettings = budgetSettings,
				currencyCode = currencyCode,
			)

			currentPeriodRecurrentSection(
				upcomingRecurrentInPeriod = upcomingRecurrentInPeriod,
				showUpcomingRecurrentInPeriod = showUpcomingRecurrentInPeriod,
				onToggleShowUpcomingRecurrentInPeriod = {
					showUpcomingRecurrentInPeriod = !showUpcomingRecurrentInPeriod
				},
				recurrentPaymentsViewMode = recurrentPaymentsViewMode,
				currencyFormat = currencyFormat,
				onDelete = { transaction ->
					recurrentToDelete = transaction
					showDeleteRecurrentDialog = true
				},
				onEdit = { transaction -> recurrentToEdit = transaction },
				onClick = { transaction -> selectedTransaction = transaction },
			)

			transactionDateSections(
				groupedTransactions = groupedCurrentTransactions,
				expandedDates = expandedDates,
				deletingTransactionIds = deletingTransactionIds,
				currencyCode = currencyCode,
				currencyFormat = currencyFormat,
				readOnly = readOnly,
				keyPrefix = "date",
				onToggleDate = ::toggleExpandedDate,
				onDelete = ::queueDeleteWithUndo,
				onEdit = { transaction -> editingTransaction = transaction },
				onClick = { transaction ->
					view.weakHapticFeedback()
					selectedTransaction = transaction
				},
			)

			futureRecurrentSection(
				futureRecurrentOutOfPeriod = futureRecurrentOutOfPeriod,
				showOutOfPeriodSubscriptions = showOutOfPeriodSubscriptions,
				onToggleShowOutOfPeriodSubscriptions = {
					showOutOfPeriodSubscriptions = !showOutOfPeriodSubscriptions
				},
				recurrentPaymentsViewMode = recurrentPaymentsViewMode,
				currencyFormat = currencyFormat,
				onDelete = { transaction ->
					recurrentToDelete = transaction
					showDeleteRecurrentDialog = true
				},
				onEdit = { transaction -> recurrentToEdit = transaction },
				onClick = { transaction -> selectedTransaction = transaction },
			)

			pastPeriodToggleSection(
				groupedPastTransactions = groupedPastTransactions,
				showPastPeriod = showPastPeriod,
				onToggleShowPastPeriod = { showPastPeriod = !showPastPeriod },
			)

			pastTransactionDateSections(
				showPastPeriod = showPastPeriod,
				groupedPastTransactions = groupedPastTransactions,
				expandedDates = expandedDates,
				deletingTransactionIds = deletingTransactionIds,
				currencyCode = currencyCode,
				currencyFormat = currencyFormat,
				readOnly = readOnly,
				onToggleDate = ::toggleExpandedDate,
				onDelete = ::queueDeleteWithUndo,
				onEdit = { transaction -> editingTransaction = transaction },
				onClick = { transaction -> selectedTransaction = transaction },
			)

			item("spacer-bottom") {
				Spacer(modifier = Modifier.height(32.dp))
			}
		}

		if (uiState.transactions.isEmpty()) {
			NoTransactionsView(
				modifier = Modifier
					.align(Alignment.Center)
					.padding(32.dp),
			)
		}

		selectedTransaction?.let {
			isDismissingTransactionDialog = false
		}
		TransactionDetailDialog(
			transaction = selectedTransaction,
			currencyFormat = currencyFormat,
			readOnly = readOnly,
			isDismissingTransactionDialog = isDismissingTransactionDialog,
			onDismissStart = { isDismissingTransactionDialog = true },
			onDismiss = { selectedTransaction = null },
			onMarkAsPaid = { selectedTransaction = null },
			onEdit = { transaction ->
				selectedTransaction = null
				editingTransaction = transaction
			},
			onDelete = { transaction ->
				selectedTransaction = null
				if (transaction.isRecurrent) {
					recurrentToDelete = transaction
					showDeleteRecurrentDialog = true
				} else {
					queueDeleteWithUndo(transaction)
				}
			},
		)
	}

	TransactionEditDialog(
		transaction = editingTransaction,
		budgetStartDate = budgetStartDate,
		budgetEndDate = budgetEndDate,
		currencyCode = currencyCode,
		onCancel = { editingTransaction = null },
		onSave = { updatedTransaction ->
			saveEditedTransaction(updatedTransaction) {
				editingTransaction = null
			}
		},
	)

	DeleteRecurrentExpenseDialog(
		transaction = recurrentToDelete.takeIf { showDeleteRecurrentDialog },
		onDismiss = {
			showDeleteRecurrentDialog = false
			recurrentToDelete = null
		},
		onConfirm = { transaction ->
			onProcessIntent(BudgetTransactionIntent.DeleteTransactionTapped(transaction))
			showDeleteRecurrentDialog = false
			recurrentToDelete = null
		},
	)

	TransactionEditDialog(
		transaction = recurrentToEdit,
		budgetStartDate = budgetStartDate,
		budgetEndDate = budgetEndDate,
		currencyCode = currencyCode,
		onCancel = { recurrentToEdit = null },
		onSave = { updatedTransaction ->
			saveEditedTransaction(updatedTransaction) {
				recurrentToEdit = null
			}
		},
	)
}

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
	MinusTheme {
		History(
			uiState = BudgetUiState(
				budgetSettings = BudgetSettings(
					totalBudget = BigDecimal("1000.00"),
					period = BudgetPeriod.MONTHLY,
					startDate = LocalDate.now().minusDays(15),
					currencyCode = "USD"
				),
				transactions = listOf(
					Transaction(
						id = 1L,
						amount = BigDecimal("50.00"),
						comment = "Groceries",
						date = LocalDateTime.now().minusDays(1)
					),
					Transaction(
						id = 2L,
						amount = BigDecimal("15.00"),
						comment = "Netflix",
						date = LocalDateTime.now().minusDays(2),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.MONTHLY
					)
				)
			)
		)
	}
}
