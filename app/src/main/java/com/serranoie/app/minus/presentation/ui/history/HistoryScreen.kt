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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.dialogs.DeleteRecurrentExpenseDialog
import com.serranoie.app.minus.presentation.ui.history.dialogs.TransactionDetailDialog
import com.serranoie.app.minus.presentation.ui.history.dialogs.TransactionEditDialog
import com.serranoie.app.minus.presentation.ui.history.sections.budgetDisplaySection
import com.serranoie.app.minus.presentation.ui.history.sections.currentPeriodRecurrentSection
import com.serranoie.app.minus.presentation.ui.history.sections.futureRecurrentSection
import com.serranoie.app.minus.presentation.ui.history.sections.pastPeriodToggleSection
import com.serranoie.app.minus.presentation.ui.history.sections.pastTransactionDateSections
import com.serranoie.app.minus.presentation.ui.history.sections.transactionDateSections
import com.serranoie.app.minus.presentation.ui.theme.component.expense.NoTransactionsView
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.time.LocalDate

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
fun HistoryScreen(
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit = { _, _, _ -> },
    onCancelPendingDelete: () -> Unit = {},
    onShowInfoSnackbar: (message: String) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryUiEffect.ShowSnackbar -> onShowInfoSnackbar(effect.message)
            }
        }
    }

    History(
        uiState = uiState,
        modifier = modifier,
        readOnly = readOnly,
        onQueueDeleteWithUndo = onQueueDeleteWithUndo,
        onCancelPendingDelete = onCancelPendingDelete,
        onShowInfoSnackbar = onShowInfoSnackbar,
        onProcessIntent = viewModel::processIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun History(
    uiState: HistoryUiState,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit = { _, _, _ -> },
    onCancelPendingDelete: () -> Unit = {},
    onShowInfoSnackbar: (message: String) -> Unit = {},
    onProcessIntent: (HistoryUiIntent) -> Unit = {},
) {
    val resources = LocalResources.current
    val scrollState = rememberLazyListState()
    val currencyCode = uiState.budgetSettings?.currencyCode ?: "USD"
    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }

    LaunchedEffect(uiState.groupedCurrentTransactions.keys, readOnly, onProcessIntent) {
        val sortedDates = uiState.groupedCurrentTransactions.keys.filterNotNull().sortedDescending()
        val current = uiState.expandedDates
        if (current.isEmpty()) {
            onProcessIntent(
                HistoryUiIntent.ToggleExpandedDate(
                    sortedDates.firstOrNull() ?: return@LaunchedEffect
                )
            )
        }
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
                budgetSettings = uiState.budgetSettings,
                currencyCode = currencyCode,
            )

            currentPeriodRecurrentSection(
                upcomingRecurrentInPeriod = uiState.upcomingRecurrentInPeriod,
                showUpcomingRecurrentInPeriod = uiState.showUpcomingRecurrentInPeriod,
                onToggleShowUpcomingRecurrentInPeriod = {
                    onProcessIntent(
                        HistoryUiIntent.ToggleUpcomingRecurrentInPeriod(!uiState.showUpcomingRecurrentInPeriod)
                    )
                },
                recurrentPaymentsViewMode = uiState.recurrentPaymentsViewMode,
                currencyFormat = currencyFormat,
                onDelete = { tx -> onProcessIntent(HistoryUiIntent.SetRecurrentToDelete(tx)) },
                onEdit = { tx -> onProcessIntent(HistoryUiIntent.SetRecurrentToEdit(tx)) },
                onClick = { tx -> onProcessIntent(HistoryUiIntent.SetSelectedTransaction(tx)) },
            )

            transactionDateSections(
                groupedTransactions = uiState.groupedCurrentTransactions,
                expandedDates = uiState.expandedDates,
                deletingTransactionIds = uiState.pendingRemovedTransactions.keys,
                currencyCode = currencyCode,
                currencyFormat = currencyFormat,
                readOnly = readOnly,
                keyPrefix = "date",
                onToggleDate = { date -> onProcessIntent(HistoryUiIntent.ToggleExpandedDate(date)) },
                onDelete = { tx ->
                    onQueueDeleteWithUndo(
                        tx,
                        resources.getString(
                            R.string.expense_deleted_format,
                            tx.comment.ifEmpty { resources.getString(R.string.generic_expense) }
                        ),
                    ) {
                        onCancelPendingDelete()
                    }
                    onProcessIntent(HistoryUiIntent.DeleteTransaction(tx))
                },
                onEdit = { tx -> onProcessIntent(HistoryUiIntent.SetEditingTransaction(tx)) },
                onClick = { tx -> onProcessIntent(HistoryUiIntent.SetSelectedTransaction(tx)) },
            )

            futureRecurrentSection(
                futureRecurrentOutOfPeriod = uiState.futureRecurrentOutOfPeriod,
                showOutOfPeriodSubscriptions = uiState.showOutOfPeriodSubscriptions,
                onToggleShowOutOfPeriodSubscriptions = {
                    onProcessIntent(
                        HistoryUiIntent.ToggleOutOfPeriodSubscriptions(!uiState.showOutOfPeriodSubscriptions)
                    )
                },
                recurrentPaymentsViewMode = uiState.recurrentPaymentsViewMode,
                currencyFormat = currencyFormat,
                onDelete = { tx -> onProcessIntent(HistoryUiIntent.SetRecurrentToDelete(tx)) },
                onEdit = { tx -> onProcessIntent(HistoryUiIntent.SetRecurrentToEdit(tx)) },
                onClick = { tx -> onProcessIntent(HistoryUiIntent.SetSelectedTransaction(tx)) },
            )

            pastPeriodToggleSection(
                groupedPastTransactions = uiState.groupedPastTransactions,
                showPastPeriod = uiState.showPastPeriod,
                onToggleShowPastPeriod = {
                    onProcessIntent(HistoryUiIntent.TogglePastPeriod(!uiState.showPastPeriod))
                },
            )

            pastTransactionDateSections(
                showPastPeriod = uiState.showPastPeriod,
                groupedPastTransactions = uiState.groupedPastTransactions,
                expandedDates = uiState.expandedDates,
                deletingTransactionIds = uiState.pendingRemovedTransactions.keys,
                currencyCode = currencyCode,
                currencyFormat = currencyFormat,
                readOnly = readOnly,
                onToggleDate = { date -> onProcessIntent(HistoryUiIntent.ToggleExpandedDate(date)) },
                onDelete = { tx ->
                    onQueueDeleteWithUndo(
                        tx,
                        resources.getString(
                            R.string.expense_deleted_format,
                            tx.comment.ifEmpty { resources.getString(R.string.generic_expense) }
                        ),
                    ) {
                        onCancelPendingDelete()
                    }
                    onProcessIntent(HistoryUiIntent.DeleteTransaction(tx))
                },
                onEdit = { tx -> onProcessIntent(HistoryUiIntent.SetEditingTransaction(tx)) },
                onClick = { tx -> onProcessIntent(HistoryUiIntent.SetSelectedTransaction(tx)) },
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

        TransactionDetailDialog(
            transaction = uiState.selectedTransaction,
            currencyFormat = currencyFormat,
            readOnly = readOnly,
            isDismissingTransactionDialog = uiState.isDismissingTransactionDialog,
            onDismissStart = { onProcessIntent(HistoryUiIntent.SetDismissingDialog(true)) },
            onDismiss = { onProcessIntent(HistoryUiIntent.SetSelectedTransaction(null)) },
            onMarkAsPaid = { onProcessIntent(HistoryUiIntent.SetSelectedTransaction(null)) },
            onEdit = { tx ->
                onProcessIntent(HistoryUiIntent.SetSelectedTransaction(null))
                onProcessIntent(HistoryUiIntent.SetEditingTransaction(tx))
            },
            onDelete = { tx ->
                onProcessIntent(HistoryUiIntent.SetSelectedTransaction(null))
                if (tx.isRecurrent) {
                    onProcessIntent(HistoryUiIntent.SetRecurrentToDelete(tx))
                } else {
                    onQueueDeleteWithUndo(
                        tx,
                        resources.getString(
                            R.string.expense_deleted_format,
                            tx.comment.ifEmpty { resources.getString(R.string.generic_expense) }
                        ),
                    ) { onCancelPendingDelete() }
                    onProcessIntent(HistoryUiIntent.DeleteTransaction(tx))
                }
            },
        )
    }

    TransactionEditDialog(
        transaction = uiState.editingTransaction,
        budgetStartDate = uiState.budgetSettings?.startDate ?: LocalDate.now().minusDays(30),
        budgetEndDate = uiState.budgetSettings?.getPeriodEndDate() ?: LocalDate.now(),
        currencyCode = currencyCode,
        onCancel = { onProcessIntent(HistoryUiIntent.SetEditingTransaction(null)) },
        onSave = { tx ->
            onProcessIntent(HistoryUiIntent.SaveEditedTransaction(tx))
            onShowInfoSnackbar(
                resources.getString(
                    R.string.expense_modified_format,
                    tx.comment.ifEmpty { resources.getString(R.string.generic_expense) }
                )
            )
            onProcessIntent(HistoryUiIntent.SetEditingTransaction(null))
        },
    )

    DeleteRecurrentExpenseDialog(
        transaction = uiState.recurrentToDelete.takeIf { uiState.showDeleteRecurrentDialog },
        onDismiss = { onProcessIntent(HistoryUiIntent.DismissDeleteRecurrentDialog) },
        onConfirm = { tx ->
            onProcessIntent(HistoryUiIntent.ConfirmDeleteRecurrent(tx))
        },
    )

    TransactionEditDialog(
        transaction = uiState.recurrentToEdit,
        budgetStartDate = uiState.budgetSettings?.startDate ?: LocalDate.now().minusDays(30),
        budgetEndDate = uiState.budgetSettings?.getPeriodEndDate() ?: LocalDate.now(),
        currencyCode = currencyCode,
        onCancel = { onProcessIntent(HistoryUiIntent.SetRecurrentToEdit(null)) },
        onSave = { tx ->
            onProcessIntent(HistoryUiIntent.SaveEditedTransaction(tx))
            onProcessIntent(HistoryUiIntent.SetRecurrentToEdit(null))
        },
    )
}
