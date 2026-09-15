package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effects.collectAsStateWithLifecycle()
    val expandedTransactionId by viewModel.expandedTransactionId.collectAsStateWithLifecycle()
    val editingTransaction by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val recurrentToDelete by viewModel.recurrentToDelete.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    SharedTransitionLayout {
        AnimatedVisibility(visible = true) {
            SubscriptionsScreenContent(
                uiState = uiState,
                effect = effect,
                expandedTransactionId = expandedTransactionId,
                editingTransaction = editingTransaction,
                recurrentToDelete = recurrentToDelete,
                tags = tags,
                onBack = onBack,
                onConfirmPaid = viewModel::onConfirmPaid,
                onSkip = viewModel::onSkip,
                onToggleExpanded = viewModel::onToggleExpanded,
                onEditRequested = viewModel::onEditRequested,
                onEditCancelled = viewModel::onEditCancelled,
                onSaveEdited = viewModel::onSaveEdited,
                onDeleteRequested = viewModel::onDeleteRequested,
                onDeleteCancelled = viewModel::onDeleteCancelled,
                onConfirmDelete = viewModel::onConfirmDelete,
                onConsumeEffect = viewModel::consumeEffect,
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubscriptionsScreenContent(
    uiState: SubscriptionsUiState,
    effect: SubscriptionsUiEffect?,
    expandedTransactionId: Long?,
    editingTransaction: Transaction?,
    recurrentToDelete: Transaction?,
    tags: List<String>,
    onBack: () -> Unit,
    onConfirmPaid: (Transaction, LocalDate) -> Unit,
    onSkip: (Transaction, LocalDate) -> Unit,
    onToggleExpanded: (Long) -> Unit,
    onEditRequested: (Transaction) -> Unit,
    onEditCancelled: () -> Unit,
    onSaveEdited: (Transaction) -> Unit,
    onDeleteRequested: (Transaction) -> Unit,
    onDeleteCancelled: () -> Unit,
    onConfirmDelete: (Transaction) -> Unit,
    onConsumeEffect: () -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(effect) {
        when (effect) {
            is SubscriptionsUiEffect.ShowSnackbar -> {
                onConsumeEffect()
                coroutineScope.launch { snackbarHostState.showSnackbar(effect.message) }
            }

            null -> { /* no-op */ }
        }
    }

    Subscriptions(
        state = uiState,
        actions = SubscriptionsActions(
            onBack = onBack,
            onConfirmPaid = onConfirmPaid,
            onSkip = onSkip,
            onToggleExpanded = onToggleExpanded,
            onEditRequested = onEditRequested,
            onEditCancelled = onEditCancelled,
            onSaveEdited = onSaveEdited,
            onDeleteRequested = onDeleteRequested,
            onDeleteCancelled = onDeleteCancelled,
            onConfirmDelete = onConfirmDelete,
        ),
        expandedTransactionId = expandedTransactionId,
        editingTransaction = editingTransaction,
        recurrentToDelete = recurrentToDelete,
        tags = tags,
        snackbarHostState = snackbarHostState,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
    )
}

@Preview(showBackground = true)
@Composable
private fun SubscriptionsScreenPreview() {
    val today = LocalDate.now()
    val sampleItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("12.99"),
            comment = "Spotify",
            date = today.minusMonths(1).atStartOfDay(),
            isDeleted = false,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today,
        isInCurrentPeriod = true,
    )
    MinusTheme {
        SubscriptionsScreenContent(
            uiState = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(sampleItem),
                upcoming = listOf(
                    sampleItem.copy(
                        transaction = sampleItem.transaction.copy(id = 2L, comment = "iCloud"),
                        nextChargeDate = today.plusDays(12),
                    )
                ),
                monthlyTotal = BigDecimal("13.98"),
                activeCount = 2,
                currencyCode = "USD",
            ),
            effect = null,
            expandedTransactionId = null,
            editingTransaction = null,
            recurrentToDelete = null,
            tags = emptyList(),
            onBack = {},
            onConfirmPaid = { _, _ -> },
            onSkip = { _, _ -> },
            onToggleExpanded = {},
            onEditRequested = {},
            onEditCancelled = {},
            onSaveEdited = {},
            onDeleteRequested = {},
            onDeleteCancelled = {},
            onConfirmDelete = {},
            onConsumeEffect = {},
        )
    }
}
