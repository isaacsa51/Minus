package com.serranoie.app.minus.presentation.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.dialogs.DeleteRecurrentExpenseDialog
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
        }.getOrNull() ?: VERTICAL_LIST
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    onCollapseDragDelta: ((Float) -> Unit)? = null,
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

    SharedTransitionLayout {
        AnimatedVisibility(visible = true) {
            History(
                uiState = uiState,
                modifier = modifier,
                readOnly = readOnly,
                onCollapseDragDelta = onCollapseDragDelta,
                onQueueDeleteWithUndo = onQueueDeleteWithUndo,
                onCancelPendingDelete = onCancelPendingDelete,
                onShowInfoSnackbar = onShowInfoSnackbar,
                onProcessIntent = viewModel::processIntent,
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun History(
    uiState: HistoryUiState,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    readOnly: Boolean = false,
    disableAnimations: Boolean = false,
    onCollapseDragDelta: ((Float) -> Unit)? = null,
    onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit = { _, _, _ -> },
    onCancelPendingDelete: () -> Unit = {},
    onShowInfoSnackbar: (message: String) -> Unit = {},
    onProcessIntent: (HistoryUiIntent) -> Unit = {},
) {
    val resources = LocalResources.current
    val scrollState = rememberLazyListState()
    val currencyCode = uiState.budgetSettings?.currencyCode ?: "USD"
    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }

    LaunchedEffect(uiState.groupedCurrentTransactions.keys, readOnly) {
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

    val currentOnCollapseDragDelta = rememberUpdatedState(onCollapseDragDelta)
    val density = LocalDensity.current
    val threshold = remember(density) { with(density) { 128.dp.toPx() } }
    val nestedScrollConnection = remember(scrollState, threshold) {
        var accumulatedCollapseDelta = 0f
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val collapseDelta = currentOnCollapseDragDelta.value
                if (collapseDelta != null &&
                    source == NestedScrollSource.UserInput &&
                    available.y < 0f &&
                    !scrollState.canScrollForward
                ) {
                    accumulatedCollapseDelta += available.y
                    if (accumulatedCollapseDelta < -threshold) {
                        collapseDelta(available.y)
                        return available
                    }
                    return available
                } else if (available.y > 0f || scrollState.canScrollForward) {
                    accumulatedCollapseDelta = 0f
                }
                return Offset.Zero
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .then(if (!disableAnimations) Modifier.animateContentSize() else Modifier)
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            budgetDisplaySection(
                budgetState = uiState.budgetState,
                budgetSettings = uiState.budgetSettings,
                currencyCode = currencyCode,
                creditOwed = uiState.creditOwed,
            )

            currentPeriodRecurrentSection(
                upcomingRecurrentInPeriod = uiState.upcomingRecurrentInPeriod,
                showUpcomingRecurrentInPeriod = uiState.showUpcomingRecurrentInPeriod,
                expandedTransactionId = uiState.expandedTransactionId,
                onToggleShowUpcomingRecurrentInPeriod = {
                    onProcessIntent(
                        HistoryUiIntent.ToggleUpcomingRecurrentInPeriod(!uiState.showUpcomingRecurrentInPeriod)
                    )
                },
                recurrentPaymentsViewMode = uiState.recurrentPaymentsViewMode,
                currencyFormat = currencyFormat,
                onDelete = { expense ->
                    if (expense.isRecurrent) {
                        onProcessIntent(HistoryUiIntent.SetRecurrentToDelete(expense))
                    } else {
                        onQueueDeleteWithUndo(
                            expense,
                            resources.getString(
                                R.string.expense_deleted_format,
                                expense.comment.ifEmpty { resources.getString(R.string.generic_expense) }
                            ),
                        ) { onCancelPendingDelete() }
                        onProcessIntent(HistoryUiIntent.DeleteTransaction(expense))
                    }
                },
                onEdit = { expense -> onProcessIntent(HistoryUiIntent.SetEditingTransaction(expense)) },
                onMarkAsPaid = { expense ->
                    onShowInfoSnackbar(resources.getString(R.string.mark_as_paid_success))
                },
                onClick = { expense ->
                    onProcessIntent(
                        HistoryUiIntent.ToggleExpandedTransaction(
                            expense.id
                        )
                    )
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                creditCardCutoffDay = uiState.budgetSettings?.creditCardCutoffDay,
            )

            transactionDateSections(
                groupedTransactions = uiState.groupedCurrentTransactions,
                expandedDates = uiState.expandedDates,
                expandedTransactionId = uiState.expandedTransactionId,
                deletingTransactionIds = uiState.pendingRemovedTransactions.keys,
                currencyCode = currencyCode,
                currencyFormat = currencyFormat,
                readOnly = readOnly,
                disableAnimations = disableAnimations,
                keyPrefix = "date",
                onToggleDate = { date -> onProcessIntent(HistoryUiIntent.ToggleExpandedDate(date)) },
                onDelete = { expense ->
                    onQueueDeleteWithUndo(
                        expense,
                        resources.getString(
                            R.string.expense_deleted_format,
                            expense.comment.ifEmpty { resources.getString(R.string.generic_expense) }
                        ),
                    ) {
                        onCancelPendingDelete()
                    }
                    onProcessIntent(HistoryUiIntent.DeleteTransaction(expense))
                },
                onEdit = { expense -> onProcessIntent(HistoryUiIntent.SetEditingTransaction(expense)) },
                onMarkAsPaid = { expense ->
                    onShowInfoSnackbar(resources.getString(R.string.mark_as_paid_success))
                },
                onClick = { expense ->
                    onProcessIntent(
                        HistoryUiIntent.ToggleExpandedTransaction(
                            expense.id
                        )
                    )
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                creditCardCutoffDay = uiState.budgetSettings?.creditCardCutoffDay,
            )

            futureRecurrentSection(
                futureRecurrentOutOfPeriod = uiState.futureRecurrentOutOfPeriod,
                showOutOfPeriodSubscriptions = uiState.showOutOfPeriodSubscriptions,
                expandedTransactionId = uiState.expandedTransactionId,
                onToggleShowOutOfPeriodSubscriptions = {
                    onProcessIntent(
                        HistoryUiIntent.ToggleOutOfPeriodSubscriptions(!uiState.showOutOfPeriodSubscriptions)
                    )
                },
                recurrentPaymentsViewMode = uiState.recurrentPaymentsViewMode,
                currencyFormat = currencyFormat,
                onDelete = { expense -> onProcessIntent(HistoryUiIntent.SetRecurrentToDelete(expense)) },
                onEdit = { expense -> onProcessIntent(HistoryUiIntent.SetRecurrentToEdit(expense)) },
                onMarkAsPaid = { expense ->
                    onShowInfoSnackbar(resources.getString(R.string.mark_as_paid_success))
                },
                onClick = { expense ->
                    onProcessIntent(
                        HistoryUiIntent.ToggleExpandedTransaction(
                            expense.id
                        )
                    )
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                creditCardCutoffDay = uiState.budgetSettings?.creditCardCutoffDay,
            )

            if (uiState.showPastTransactionsSetting) {
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
                    expandedTransactionId = uiState.expandedTransactionId,
                    deletingTransactionIds = uiState.pendingRemovedTransactions.keys,
                    currencyCode = currencyCode,
                    currencyFormat = currencyFormat,
                    readOnly = readOnly,
                    onToggleDate = { date -> onProcessIntent(HistoryUiIntent.ToggleExpandedDate(date)) },
                    onDelete = { expense ->
                        onQueueDeleteWithUndo(
                            expense,
                            resources.getString(
                                R.string.expense_deleted_format,
                                expense.comment.ifEmpty { resources.getString(R.string.generic_expense) }
                            ),
                        ) {
                            onCancelPendingDelete()
                        }
                        onProcessIntent(HistoryUiIntent.DeleteTransaction(expense))
                    },
                    onEdit = { expense ->
                        onProcessIntent(
                            HistoryUiIntent.SetEditingTransaction(
                                expense
                            )
                        )
                    },
                    onMarkAsPaid = { expense ->
                        onShowInfoSnackbar(resources.getString(R.string.mark_as_paid_success))
                    },
                    onClick = { expense ->
                        onProcessIntent(
                            HistoryUiIntent.ToggleExpandedTransaction(
                                expense.id
                            )
                        )
                    },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    creditCardCutoffDay = uiState.budgetSettings?.creditCardCutoffDay,
                )
            }

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
    }

    TransactionEditDialog(
        transaction = uiState.editingTransaction,
        budgetStartDate = uiState.budgetSettings?.startDate ?: LocalDate.now().minusDays(30),
        budgetEndDate = uiState.budgetSettings?.getPeriodEndDate() ?: LocalDate.now(),
        currencyCode = currencyCode,
        tags = uiState.tags,
        isCreditQuickToggleEnabled = uiState.isCreditQuickToggleEnabled,
        creditCardCutoffDay = uiState.budgetSettings?.creditCardCutoffDay,
        onUpdateCreditCutoffDay = { day -> onProcessIntent(HistoryUiIntent.UpdateCreditCutoffDay(day)) },
        onCancel = { onProcessIntent(HistoryUiIntent.SetEditingTransaction(null)) },
        onSave = { expense ->
            onProcessIntent(HistoryUiIntent.SaveEditedTransaction(expense))
            onShowInfoSnackbar(
                resources.getString(
                    R.string.expense_modified_format,
                    expense.comment.ifEmpty { resources.getString(R.string.generic_expense) }
                )
            )
            onProcessIntent(HistoryUiIntent.SetEditingTransaction(null))
        },
    )

    DeleteRecurrentExpenseDialog(
        transaction = uiState.recurrentToDelete.takeIf { uiState.showDeleteRecurrentDialog },
        onDismiss = { onProcessIntent(HistoryUiIntent.DismissDeleteRecurrentDialog) },
        onConfirm = { expense ->
            onProcessIntent(HistoryUiIntent.ConfirmDeleteRecurrent(expense))
        },
    )

    TransactionEditDialog(
        transaction = uiState.recurrentToEdit,
        budgetStartDate = uiState.budgetSettings?.startDate ?: LocalDate.now().minusDays(30),
        budgetEndDate = uiState.budgetSettings?.getPeriodEndDate() ?: LocalDate.now(),
        currencyCode = currencyCode,
        tags = uiState.tags,
        onCancel = { onProcessIntent(HistoryUiIntent.SetRecurrentToEdit(null)) },
        onSave = { expense ->
            onProcessIntent(HistoryUiIntent.SaveEditedTransaction(expense))
            onProcessIntent(HistoryUiIntent.SetRecurrentToEdit(null))
        },
    )
}
